package common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        CONNECT,
        CONNECT_ACK,
        COUNTDOWN,      // For countdown messages
        START,
        QUESTION,
        ANSWER,
        RESULT,
        GAME_OVER,
        ERROR,
        PING           // For keep-alive messages
    }

    public Type type;
    private Map<String, Object> data = new HashMap<>();

    public Message(Type type) {
        this.type = type;
    }

    public Message put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public Object get(String key) {
        return data.get(key);
    }
}