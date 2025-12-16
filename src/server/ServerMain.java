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
    private Map<Integer, String> playerOperations = new ConcurrentHashMap<>();
    private Map<Integer, String> currentQuestions = new ConcurrentHashMap<>();
    
    private int currentQuestionId = 0;
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

            while (!gameOver) {
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

    public synchronized int registerPlayer(String name, String operation) {
        int id = nextPlayerId++;
        playerPositions.put(id, 0);
        playerNames.put(id, name);
        playerOperations.put(id, operation);
        
        System.out.println("Player " + id + " registered: " + name + " (Op: " + operation + ")");
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
        System.out.println("Starting game with " + clients.size() + " players!");
        
        Message startMsg = new Message(Message.Type.START)
            .put("playerCount", clients.size());
        broadcast(startMsg);
        
        // Broadcast initial positions
        broadcastPositions();
        
        // Send first question immediately
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Wait 2 seconds before first question
                sendNextQuestion();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendNextQuestion() {
        currentQuestionId++;
        
        // Send different question to each player based on their operation
        for (ClientHandler client : clients) {
            String op = playerOperations.get(client.playerId);
            QuestionAnswer qa = generateQuestion(op);
            
            // Store correct answer
            currentQuestions.put(client.playerId, qa.answer);
            
            Message msg = new Message(Message.Type.QUESTION)
                .put("qId", currentQuestionId)
                .put("text", qa.question);
            
            client.send(msg);
        }
    }

    private QuestionAnswer generateQuestion(String op) {
        Random rand = new Random();
        int a = rand.nextInt(20) + 1;
        int b = rand.nextInt(20) + 1;
        String question;
        int answer;
        
        switch (op) {
            case "+":
                question = a + " + " + b;
                answer = a + b;
                break;
            case "-":
                int sum = a + b;
                question = sum + " - " + b;
                answer = a;
                break;
            case "*":
                question = a + " × " + b;
                answer = a * b;
                break;
            case "/":
                int product = a * b;
                question = product + " ÷ " + b;
                answer = a;
                break;
            default:
                question = a + " + " + b;
                answer = a + b;
        }
        
        return new QuestionAnswer(question, String.valueOf(answer));
    }

    public synchronized void processAnswer(int playerId, int questionId, int answer) {
        if (gameOver || questionId != currentQuestionId) {
            return; // Ignore late or wrong question answers
        }

        String correctAnswer = currentQuestions.get(playerId);
        if (correctAnswer == null) return;
        
        boolean correct = (String.valueOf(answer).equals(correctAnswer));

        if (correct) {
            int currentPos = playerPositions.get(playerId);
            int newPos = Math.min(currentPos + 10, FINISH_LINE);
            playerPositions.put(playerId, newPos);

            System.out.println("Player " + playerId + " (" + playerNames.get(playerId) + ") correct! Position: " + newPos);

            // Broadcast updated positions
            broadcastPositions();

            // Check for winner
            if (newPos >= FINISH_LINE && !gameOver) {
                gameOver = true;
                Message winMsg = new Message(Message.Type.GAME_OVER)
                    .put("winnerId", playerId)
                    .put("winnerName", playerNames.get(playerId));
                broadcast(winMsg);
                System.out.println("Player " + playerId + " (" + playerNames.get(playerId) + ") wins!");
                
                // Close server after game over
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("Game ended. Shutting down server...");
                        System.exit(0);
                    }
                }, 5000);
            } else {
                // Send next question after correct answer
                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // Wait 1 second before next question
                        sendNextQuestion();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } else {
            System.out.println("Player " + playerId + " wrong answer: " + answer +
                    " (correct: " + correctAnswer + ")");
        
            // TAMBAHAN WAJIB
            broadcastPositions();
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

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        
        // If player disconnects during countdown, check if we still have enough players
        if (countdownStarted && !gameStarted) {
            if (clients.size() < MIN_PLAYERS) {
                System.out.println("Not enough players. Cancelling countdown...");
                if (countdownTimer != null) {
                    countdownTimer.cancel();
                }
                countdownStarted = false;
                
                // Notify remaining clients
                Message msg = new Message(Message.Type.ERROR)
                    .put("msg", "Countdown cancelled - not enough players");
                broadcast(msg);
            }
        }
        
        playerPositions.remove(client.playerId);
        playerNames.remove(client.playerId);
        playerOperations.remove(client.playerId);
        System.out.println("Player " + client.playerId + " disconnected");
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
                String operation = (String) firstMsg.get("operation");
                
                playerId = server.registerPlayer(name, operation);

                Message ack = new Message(Message.Type.CONNECT_ACK).put("playerId", playerId);
                send(ack);
                
                // Note: broadcastPositions() is called in registerPlayer(), 
                // so all clients will receive updated positions automatically

                // Listen for answers
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