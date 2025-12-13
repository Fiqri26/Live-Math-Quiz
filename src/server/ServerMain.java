package server;

import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    public static void main(String[] args) throws Exception {
        int port = 5000;
        if (args.length >= 1) port = Integer.parseInt(args[0]);
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server listening on port " + port);
        GameManager manager = new GameManager();

        int nextPlayerId = 1;
        while (true) {
            Socket client = serverSocket.accept();
            System.out.println("Incoming connection from " + client.getRemoteSocketAddress());
            if (nextPlayerId > 2) {
                // optionally reject more than 2 players
                client.close();
                System.out.println("Rejected connection: game supports 2 players only.");
                continue;
            }
            PlayerHandler ph = new PlayerHandler(client, manager, nextPlayerId);
            ph.start();
            nextPlayerId++;
        }
    }
}
