package utility;

import org.json.JSONException;
import org.json.JSONObject;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.MessageContentResponse;

import controller.State;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

/**
 * ParserMessageJSON: used by Controller to send message to agent modules.
 * @author szhouan
 * @version v2.0.0
 */
@Slf4j
public class ParserMessageJSON extends MessageJSON {
    private static final HashSet<String> keySet;
    private static final HashSet<String> typeSet;
    static {
        keySet = new HashSet<>(
            Arrays.asList(
                "userId", "type", "state",
                "messageId", "textContent", "imageContent"
            )
        );
        typeSet = new HashSet<>(
            Arrays.asList(
                "text", "image", "transition"
            )
        );
    }

    /**
     * Constructor for ParserMessageJSON.
     * @param userId String of user Id
     * @param type Message type
     */
    public ParserMessageJSON(String userId, String type) {
        if (!typeSet.contains(type)) {
            log.info("Invalid type: {}", type);
            type = "transition";
        }
        json.put("userId", userId)
            .put("type", type);
    }

    /**
     * Set key-value pair for ParserMessageJSON.
     * @param key Name of the key to set.
     * @param value Value to set.
     * @return this object.
     */
    public ParserMessageJSON set(String key, Object value) {
        if (!keySet.contains(key)) {
            log.info("Set invalid field: {}", key);
            return this;
        }
        boolean valid = true;
        switch (key) {
            case "type":
                if (typeSet.contains(value)) json.put(key, value);
                else valid = false;
                break;
            default:
                json.put(key, value);
        }
        if (!valid) {
            log.info("Invalid value for field {}: {}", key, value.toString());
        }
        return this;
    }

    /**
     * Get value corresponding to key (except imageContent).
     * @param key Name of the key
     * @return String of value contained in the key
     */
    public String get(String key) {
        if (!keySet.contains(key) || key.equals("imageContent")) {
            return null;
        }
        return json.getString(key);
    }

    /**
     * Get image content, that is the response body.
     * @return the responseBody
     */
    public MessageContentResponse getImageContent() {
        ObjectMapper m = new ObjectMapper();
        MessageContentResponse responseBody;
		try {
            responseBody = m.readValue(json.getJSONObject("imageContent").toString(), MessageContentResponse.class);
            return responseBody;
		} catch (JSONException | IOException e) {
			log.info("get exception when parsing imageContent to MessageContentResponse Object");
		} 
    }

    /**
     * Set image content using the reponse
     * @param the response body
     * @return this object
     */
    public ParserMessageJSON setImageContent(MessageContentResponse mcr) {
        json.put("imageContent", mcr);
        return this;
    }

    /**
     * Get state object corresponding to the JSON.
     * @return State object
     */
    public State getState() {
        return State.getStateByName(json.getString("state"));
    }

    /**
     * Set state field of the JSON.
     * @param state Name of the state
     * @return this object
     */
    public ParserMessageJSON setState(String state) {
        if (State.validateStateName(state)) {
            json.put("state", state);
        }
        return this;
    }
}