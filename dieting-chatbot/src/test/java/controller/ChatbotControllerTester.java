package controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.bus.Event;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.LineMessagingClientImpl;
import com.linecorp.bot.client.LineMessagingService;
import com.linecorp.bot.client.LineMessagingServiceBuilder;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.ImageMessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.UserSource;
import lombok.extern.slf4j.Slf4j;

import static reactor.bus.selector.Selectors.$;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ChatbotController.class, Formatter.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class ChatbotControllerTester {
    @Autowired
    private ChatbotController controller;

    @Autowired
    private Publisher publisher;

    @Autowired
    private TaskScheduler taskScheduler;

    @Test
    public void testConstruct() {
        assert controller != null;
    }

    @Test
    public void testStateMachineGetter() {
        controller.clearStateMachines();
        StateMachine sm;

        sm = controller.getStateMachine("Robin");
        assert sm.getState().equals("Idle");
        sm.setState("RecordMeal");
        sm = controller.getStateMachine("Robin");
        assert sm.getState().equals("RecordMeal");

        sm = controller.getStateMachine("Hartshorne");
        assert sm.getState().equals("Idle");
        sm.setState("Feedback");
        sm = controller.getStateMachine("Hartshorne");
        assert sm.getState().equals("Feedback");

        controller.clearStateMachines();
        sm = controller.getStateMachine("Robin");
        assert sm.getState().equals("Idle");
        sm = controller.getStateMachine("Hartshorne");
        assert sm.getState().equals("Idle");
    }

    @Test
    public void testRecommendationRequestJudge1() {
        String sentence;
        sentence = "I want some recommendations.";
        assert ChatbotController.isRecommendationRequest(sentence);
        sentence = "Can you help me look at this menu?";
        assert ChatbotController.isRecommendationRequest(sentence);
        sentence = "What is your suggestion on this?";
        assert ChatbotController.isRecommendationRequest(sentence);
    }

    @Test
    public void testRecommendationRequestJudge2() {
        String sentence;
        sentence = "Hello";
        assert !ChatbotController.isRecommendationRequest(sentence);
        sentence = "Input personal information";
        assert !ChatbotController.isRecommendationRequest(sentence);
        sentence = "*(#Ujflkd#())";
        assert !ChatbotController.isRecommendationRequest(sentence);
    }

    @Test
    public void testInitialInputRequestJudge1() {
        String sentence;
        sentence = "I want to revise personal setting.";
        assert ChatbotController.isInitialInputRequest(sentence);
        sentence = "1093, can you check my settings?";
        assert ChatbotController.isInitialInputRequest(sentence);
        sentence = "Personal! info* please;";
        assert ChatbotController.isInitialInputRequest(sentence);
    }

    @Test
    public void testInitialInputRequestJudge2() {
        String sentence;
        sentence = "I want some recommendations.";
        assert !ChatbotController.isInitialInputRequest(sentence);
        sentence = "Can you help me look at this menu?";
        assert !ChatbotController.isInitialInputRequest(sentence);
        sentence = "What is your suggestion on this?";
        assert !ChatbotController.isInitialInputRequest(sentence);
    }

    @Test
    public void testFeedbackRequestJudge1() {
        String sentence;
        sentence = "Feedback please";
        assert ChatbotController.isFeedbackRequest(sentence);
        sentence = "I want my weekly/monthly digest";
        assert ChatbotController.isFeedbackRequest(sentence);
        sentence = "Can you generate!a report for me";
        assert ChatbotController.isFeedbackRequest(sentence);
    }

    @Test
    public void testFeedbackRequestJudge2() {
        String sentence;
        sentence = "I want some recommendations.";
        assert !ChatbotController.isFeedbackRequest(sentence);
        sentence = "Can you help me look at this menu?";
        assert !ChatbotController.isFeedbackRequest(sentence);
        sentence = "What is your suggestion on this?";
        assert !ChatbotController.isFeedbackRequest(sentence);
    }

    @Test
    public void testStateTransition1() {
        FormatterMessageJSON fmt = new FormatterMessageJSON();
        fmt.set("userId", "szhouan")
           .set("type", "transition")
           .set("stateTransition", "recommendationRequest");
        Event<FormatterMessageJSON> ev =
            new Event<FormatterMessageJSON>(null, fmt);
        controller.accept(ev);
        StateMachine sm = controller.getStateMachine("szhouan");
        assert sm.getState().equals("ParseMenu");
        
        fmt.set("stateTransition", "menuMessage");
        controller.accept(ev);
        assert sm.getState().equals("AskMeal");

        fmt.set("stateTransition", "confirmMeal");
        controller.accept(ev);
        assert sm.getState().equals("Recommend");

        sm.setState("RecordMeal");
        controller.accept(ev);
        assert sm.getState().equals("Idle");

        fmt.set("stateTransition", "initialInputRequest");
        controller.accept(ev);
        assert sm.getState().equals("InitialInput");

        fmt.set("stateTransition", "userInitialInput");
        controller.accept(ev);
        assert sm.getState().equals("Idle");

        fmt.set("stateTransition", "feedbackRequest");
        controller.accept(ev);
        assert sm.getState().equals("Feedback");

        fmt.set("stateTransition", "sendFeedback");
        controller.accept(ev);
        assert sm.getState().equals("Idle");
    }

    @Test
    public void testStateTransition2() {
        controller.clearStateMachines();
        FormatterMessageJSON fmt = new FormatterMessageJSON();
        fmt.set("userId", "agong")
           .set("type", "transition")
           .set("stateTransition", "invalidTransition");
        Event<FormatterMessageJSON> ev =
            new Event<FormatterMessageJSON>(null, fmt);
        controller.accept(ev);
        StateMachine sm = controller.getStateMachine("agong");
        assert sm.getState().equals("Idle");

        fmt.set("stateTransition", "confirmMeal");
        controller.accept(ev);
        assert sm.getState().equals("Idle");
    }

    @Test
    public void testTimeoutState() throws Exception {
        assert controller.taskScheduler != null;
        String userId = "timeoutTest";
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Runnable runnable = invocation.getArgumentAt(0,
                    Runnable.class);
                runnable.run();
                return null;
            }
        }).when(taskScheduler).schedule(
            any(Runnable.class), any(Date.class));
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                throws Throwable {
                ParserMessageJSON psr = invocation.getArgumentAt(0,
                    ParserMessageJSON.class);
                assert psr.get("userId").equals(userId);
                return null;
            }
        }).when(publisher)
          .publish(Matchers.any(ParserMessageJSON.class));
        controller.toNextState(userId, "recommendationRequest");
        Mockito.reset(taskScheduler);
        Mockito.reset(publisher);
    }

    @Test
    public void testAskWeight() {
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                throws Throwable {
                ParserMessageJSON psr = invocation.getArgumentAt(0,
                    ParserMessageJSON.class);
                assert psr.get("state").equals("AskWeight");
                return null;
            }
        }).when(publisher).publish(Matchers.any(ParserMessageJSON.class));
        controller.askWeight();
        Mockito.reset(publisher);
    }

    @Test
    public void testChangeStateByCommand1() {
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                    FormatterMessageJSON.class);
                assert fmt.get("userId").equals("commandTester");
                assert fmt.get("type").equals("push");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        controller.changeStateByCommand("commandTester", "$$$RecordMeal");
        assert controller.getStateMachine("commandTester").getState()
            .equals("RecordMeal");
        Mockito.reset(publisher);
    }

    @Test
    public void testChangeStateByCommand2() {
        MessageEvent<TextMessageContent> event =
            getTextMessageEvent("$$$AskMeal", "szhouan");
        try {
            controller.handleTextMessageEvent(event);
            assert controller.getStateMachine("szhouan").getState()
                .equals("AskMeal");
        } catch (Exception e) {
            assert false;
        }
    }

    @Test
    public void testInputHandling1() {
        MessageEvent<TextMessageContent> event;
        try {
            controller.clearStateMachines();
            event = getTextMessageEvent("Recommendation", "user");
            controller.handleTextMessageEvent(event);
            assert controller.getStateMachine("user").getState()
                .equals("ParseMenu");

            controller.clearStateMachines();
            event = getTextMessageEvent("setting", "user");
            controller.handleTextMessageEvent(event);
            assert controller.getStateMachine("user").getState()
                .equals("InitialInput");

            controller.clearStateMachines();
            event = getTextMessageEvent("feedback", "user");
            controller.handleTextMessageEvent(event);
            assert controller.getStateMachine("user").getState()
                .equals("Feedback");
        } catch (Exception e) {
            log.info(e.toString());
        }
    }

    @Test
    public void testInputHandling2() {
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                throws Throwable {
                ParserMessageJSON psr = invocation.getArgumentAt(0,
                    ParserMessageJSON.class);
                assert psr.get("userId").equals("user");
                assert psr.getTextContent().equals("Something");
                return null;
            }
        }).when(publisher).publish(Matchers.any(ParserMessageJSON.class));
        MessageEvent<TextMessageContent> event;
        event = getTextMessageEvent("user", "Something");
    }

    @Test
    public void testCreateURI() {
        String uri = "foo-bar";
        String[] tokens = ChatbotController.createUri(uri).split("/");
        assert tokens[tokens.length-1].equals(uri);
    }

    @Test
    public void testSaveContent() {
        ImageMessageContent content = new ImageMessageContent("id");
        MessageEvent<ImageMessageContent> event =
            new MessageEvent<>("token", null, content, null);
        try {
            controller.handleImageMessageEvent(event);
        } catch (Exception e) {
            log.info(e.toString());
        }
    }

    /**
     * Get text message event
     * @param textContent String of text content
     * @param userId String of user Id
     * @return An MessageEvent<TextMessageContent> object
     */
    private MessageEvent<TextMessageContent> getTextMessageEvent (
        String textContent, String userId) {

        TextMessageContent text = new TextMessageContent("1234",
            textContent);
        UserSource userSource = new UserSource(userId);
        return new MessageEvent<TextMessageContent>("token", userSource,
                    text, null);
    }
}