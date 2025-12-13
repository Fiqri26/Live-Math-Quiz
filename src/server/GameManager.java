package server;

import common.Message;
import server.QuestionGenerator.Q;

import java.util.HashMap;
import java.util.Map;

public class GameManager {
    private final Map<Integer, PlayerHandler> players = new HashMap<>();
    private final Map<Integer, Integer> position = new HashMap<>();
    private final Map<Integer, Q> lastQuestion = new HashMap<>();
    private final int FINISH = 10;
    private boolean started = false;

    public synchronized void playerReady(int playerId, PlayerHandler handler) {
        players.put(playerId, handler);
        position.put(playerId, 0);
        broadcastStatus();
        if (players.size() == 2 && !started) {
            startGame();
        }
    }

    public synchronized void playerDisconnected(int playerId) {
        players.remove(playerId);
        position.remove(playerId);
        broadcastStatus();
    }

    private void startGame() {
        started = true;
        System.out.println("Game starting!");
        broadcast(new Message(Message.Type.START));
        // kirim pertanyaan pertama ke setiap pemain
        players.forEach((id, p) -> sendQuestionToPlayer(id));
    }

    private void sendQuestionToPlayer(int playerId) {
        PlayerHandler p = players.get(playerId);
        if (p == null) return;
        Q q = QuestionGenerator.make(p.operation);
        lastQuestion.put(playerId, q);
        Message m = new Message(Message.Type.QUESTION)
                .put("qId", q.id)
                .put("text", q.text);
        p.send(m);
    }

    public synchronized void onAnswer(int playerId, int qId, int answer) {
        Q q = lastQuestion.get(playerId);
        if (q == null || q.id != qId) {
            // out-of-sync question, ignore
            System.out.println("Player " + playerId + " answered unknown qId");
            players.get(playerId).send(new Message(Message.Type.ERROR).put("msg", "Question mismatch"));
            return;
        }
        if (q.answer == answer) {
            // benar: maju
            position.put(playerId, position.getOrDefault(playerId,0) + 1);
            System.out.println("Player " + playerId + " correct! pos=" + position.get(playerId));
            broadcastStatus();
            if (position.get(playerId) >= FINISH) {
                broadcast(new Message(Message.Type.GAME_OVER).put("winnerId", playerId));
                started = false;
                // game berhenti; bisa restart jika diinginkan
                return;
            }
        } else {
            // salah -> tidak maju
            System.out.println("Player " + playerId + " incorrect. given=" + answer + " expect=" + q.answer);
        }
        // kirim soal baru untuk pemain itu
        sendQuestionToPlayer(playerId);
    }

    private void broadcastStatus() {
        Message m = new Message(Message.Type.RESULT).put("posMap", new HashMap<>(position));
        // include names
        Map<Integer, String> names = new HashMap<>();
        players.forEach((id, p) -> names.put(id, p.playerName));
        m.put("names", names);
        players.forEach((id, p) -> p.send(m));
    }

    private void broadcast(Message m) {
        players.forEach((id, p) -> p.send(m));
    }
}
