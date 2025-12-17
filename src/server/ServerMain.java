package server;

import common.Message;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerMain {
    private static final int MIN_PLAYERS = 2;
    private static final int COUNTDOWN_SECONDS = 10;
    private static final int FINISH_LINE = 100;
    
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    public Map<Integer, Integer> playerPositions = new ConcurrentHashMap<>();
    public Map<Integer, String> playerNames = new ConcurrentHashMap<>();
    private Map<Integer, Integer> playerQuestionId = new ConcurrentHashMap<>();
    private Map<Integer, String> playerCorrectAnswer = new ConcurrentHashMap<>();

    private int nextPlayerId = 1;
    private boolean countdownStarted = false;
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private Timer countdownTimer;

    public static void main(String[] args) {
        new ServerMain().start();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(5000);
            System.out.println("Server started on port 5000");
            System.out.println("Waiting for at least " + MIN_PLAYERS + " players...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                
                if (!gameStarted) {
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    clients.add(handler);
                    new Thread(handler).start();
                } else {
                    // Reject connection if game already started
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                    Message error = new Message(Message.Type.ERROR)
                        .put("msg", "Game already started");
                    out.writeObject(error);
                    out.close();
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized int registerPlayer(String name) {
        int id = nextPlayerId++;
        playerPositions.put(id, 0);
        playerNames.put(id, name);
    
        System.out.println("Player " + id + " registered: " + name + " (Addition Only)");
    
        System.out.println("Total players: " + clients.size());
        System.out.println("Current positions: " + playerPositions);
        System.out.println("Current names: " + playerNames);
        
        // Broadcast current positions to ALL clients immediately
        broadcastPositions();
        
        // Start countdown when second player joins
        if (clients.size() == MIN_PLAYERS && !countdownStarted) {
            countdownStarted = true;
            startCountdown();
        }
        
        return id;
    }

    private void startCountdown() {
        System.out.println("Starting " + COUNTDOWN_SECONDS + " second countdown...");
    
        countdownTimer = new Timer();
        final int[] countdown = { COUNTDOWN_SECONDS };
    
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
    
                    Message countdownMsg = new Message(Message.Type.COUNTDOWN)
                            .put("seconds", countdown[0])
                            .put("playerCount", clients.size());
    
                    broadcast(countdownMsg);
    
                    System.out.println("Countdown: " + countdown[0]);
                    countdown[0]--;
    
                } else {
                    countdownTimer.cancel();
                    startGame();
                }
            }
        }, 1000, 1000); 
    }
    
    private void startGame() {
        gameStarted = true;
    
        broadcast(new Message(Message.Type.START)
            .put("playerCount", clients.size()));
    
        broadcastPositions();
    
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                for (ClientHandler c : clients) {
                    sendQuestionToPlayer(c.playerId);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private QuestionAnswer generateQuestion() {
        Random rand = new Random();
        int a = rand.nextInt(20) + 1;
        int b = rand.nextInt(20) + 1;
    
        String question = a + " + " + b;
        String answer = String.valueOf(a + b);
    
        return new QuestionAnswer(question, answer);
    }
    

    private void sendQuestionToPlayer(int playerId) {
        int qId = playerQuestionId.getOrDefault(playerId, 0) + 1;
        playerQuestionId.put(playerId, qId);
    
        QuestionAnswer qa = generateQuestion();
        playerCorrectAnswer.put(playerId, qa.answer);
    
        Message msg = new Message(Message.Type.QUESTION)
            .put("qId", qId)
            .put("text", qa.question);
    
        for (ClientHandler c : clients) {
            if (c.playerId == playerId) {
                c.send(msg);
                break;
            }
        }
    }
    
    
    public synchronized void processAnswer(int playerId, int qId, int answer) {
        if (gameOver) return;
    
        int expectedQId = playerQuestionId.getOrDefault(playerId, -1);
        if (qId != expectedQId) return;
    
        String correct = playerCorrectAnswer.get(playerId);
        boolean isCorrect = String.valueOf(answer).equals(correct);
    
        if (isCorrect) {
            int newPos = Math.min(playerPositions.get(playerId) + 10, FINISH_LINE);
            playerPositions.put(playerId, newPos);
    
            broadcastPositions();
            if (newPos >= FINISH_LINE) {
                gameOver = true;
            
                // Kirim GAME_OVER dengan data lengkap untuk podium
                Message winMsg = new Message(Message.Type.GAME_OVER)
                    .put("winnerId", playerId)
                    .put("winnerName", playerNames.get(playerId))
                    .put("posMap", new HashMap<>(playerPositions))
                    .put("names", new HashMap<>(playerNames));
            
                broadcast(winMsg);
            
                System.out.println("WINNER: " + playerNames.get(playerId));
                System.out.println("Final Positions: " + playerPositions);
            
                // Reset server setelah game over
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        resetGame();
                    }
                }, 5000); // delay 5 detik (biar client lihat podium)
            
                return;
            }
            
        } else {
            broadcastPositions();
        }
    
        if (!gameOver) {
            sendQuestionToPlayer(playerId);
        }
    }
    
    private void broadcastPositions() {
        Message msg = new Message(Message.Type.RESULT)
            .put("posMap", new HashMap<>(playerPositions))
            .put("names", new HashMap<>(playerNames));
        
        System.out.println("Broadcasting positions to " + clients.size() + " clients");
        System.out.println("Positions: " + playerPositions);
        System.out.println("Names: " + playerNames);
        
        broadcast(msg);
    }

    private void broadcast(Message msg) {
        for (ClientHandler client : clients) {
            client.send(msg);
        }
    }

    private synchronized void resetGame() {
        System.out.println("Resetting game...");
    
        gameOver = false;
        gameStarted = false;
        countdownStarted = false;
    
        nextPlayerId = 1;
    
        playerPositions.clear();
        playerNames.clear();
        playerQuestionId.clear();
        playerCorrectAnswer.clear();
    
        System.out.println("Server reset complete. Ready for new game.");
    }
    
    

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        playerPositions.remove(client.playerId);
        playerNames.remove(client.playerId);
        playerQuestionId.remove(client.playerId);
        playerCorrectAnswer.remove(client.playerId);
        System.out.println("Player " + client.playerId + " disconnected");
        broadcastPositions();
    }
    
    
    
    private static class QuestionAnswer {
        String question;
        String answer;
        
        QuestionAnswer(String q, String a) {
            this.question = q;
            this.answer = a;
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private ServerMain server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    public int playerId;

    public ClientHandler(Socket socket, ServerMain server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Message firstMsg = (Message) in.readObject();
            if (firstMsg.type == Message.Type.CONNECT) {
                String name = (String) firstMsg.get("name");
                playerId = server.registerPlayer(name);
                Message ack = new Message(Message.Type.CONNECT_ACK).put("playerId", playerId);
                send(ack);
                while (true) {
                    Message msg = (Message) in.readObject();
                    if (msg.type == Message.Type.ANSWER) {
                        int qId = (int) msg.get("qId");
                        int answer = (int) msg.get("answer");
                        server.processAnswer(playerId, qId, answer);
                    } else if (msg.type == Message.Type.PING) {
                        // Respond to ping to keep connection alive
                        send(new Message(Message.Type.PING));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Client handler error: " + e.getMessage());
        } finally {
            server.removeClient(this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.out.println("Error sending message to player " + playerId);
        }
    }
}