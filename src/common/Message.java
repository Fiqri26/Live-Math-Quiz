package common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {
    public enum Type {
        CONNECT,        // client -> server {name, operation}
        CONNECT_ACK,    // server -> client {playerId}
        START,          // server -> clients
        QUESTION,       // server -> client {qId, text}
        ANSWER,         // client -> server {qId, answer}
        RESULT,         // server -> clients {playerPosMap}
        GAME_OVER,      // server -> clients {winnerId}
        ERROR,          // server -> client {msg}
        PING
    }

    public Type type;
    public Map<String, Object> data = new HashMap<>();

    public Message(Type type) {
        this.type = type;
    }

    public Message put(String k, Object v) {
        data.put(k, v);
        return this;
    }

    public Object get(String k) {
        return data.get(k);
    }
}
