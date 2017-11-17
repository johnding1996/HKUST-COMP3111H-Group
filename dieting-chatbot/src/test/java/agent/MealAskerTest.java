package agent;

import controller.State;
import controller.TestConfiguration;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import utility.JazzySpellChecker;
import utility.ParserMessageJSON;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {MealAsker.class, JazzySpellChecker.class, MenuManager.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class MealAskerTest extends AgentTest {

    @Autowired
    private MealAsker asker;

    @PostConstruct
    public void init() {
        agent = asker;
        agentState = State.ASK_MEAL;
        userId = "agong";

        JSONObject menuJSON = new JSONObject();
        menuJSON.put("userId", userId);
        JSONArray menu = new JSONArray();
        for (int i=0; i<3; ++i) {
            JSONObject dish = new JSONObject();
            dish.put("name", "dish" + (i+1));
            menu.put(dish);
        }
        menuJSON.put("menu", menu);
    }

    @Test
    public void testAccept() {
        controller.setUserState(userId, agentState);
        asker.registerUser(userId);
        checkHandler("", "Well, I got", 0, Agent.END_STATE);
    }

    @Test
    public void testAnotherState() {
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.setState("Feedback")
           .set("textContent", "hello");
        checkNotExecuted(psr);
    }
}