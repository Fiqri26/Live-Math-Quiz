package server;

import common.Message;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.function.BiConsumer;

public class PlayerHandler extends Thread {
    private final Socket socket;
    private final GameManager manager;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final int playerId;
    public String playerName;
    public String operation; // "+", "-", "*", "/"

    public PlayerHandler(Socket socket, GameManager manager, int playerId) {
        this.socket = socket;
        this.manager = manager;
        this.playerId = playerId;
    }

    public void send(Message m) {
        try {
            out.writeObject(m);
            out.flush();
        } catch (IOException e) {
            System.err.println("Send fail to player " + playerId + ": " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());
            System.out.println("PlayerHandler " + playerId + " streams ready.");

            // expect CONNECT
            Message m = (Message) in.readObject();
            if (m.type != Message.Type.CONNECT) {
                send(new Message(Message.Type.ERROR).put("msg", "First message must be CONNECT"));
                socket.close();
                return;
            }
            playerName = (String) m.get("name");
            operation = (String) m.get("operation");
            System.out.println("Player " + playerId + " connected: " + playerName + " op=" + operation);

            send(new Message(Message.Type.CONNECT_ACK).put("playerId", playerId));
            manager.playerReady(playerId, this);

            // main loop: read answers or pings
            while (!socket.isClosed()) {
                Message incoming = (Message) in.readObject();
                if (incoming.type == Message.Type.ANSWER) {
                    int qId = (int) incoming.get("qId");
                    int ans = (int) incoming.get("answer");
                    manager.onAnswer(playerId, qId, ans);
                } else if (incoming.type == Message.Type.PING) {
                    // ignore or reply
                }
            }

        } catch (EOFException eof) {
            System.out.println("Player " + playerId + " disconnected.");
        } catch (Exception e) {
            System.err.println("PlayerHandler error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            manager.playerDisconnected(playerId);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
