package client;

import common.Message;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class ClientMain {
    private JFrame frame;
    private JTextField tfName, tfServerIP, tfAnswer;
    private JComboBox<String> cbOp;
    private JButton btnConnect;
    private CarPanel carPanel;
    private JLabel lblQuestion;
    private JLabel lblStatus;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private int playerId;
    private String myOp;
    
    // Music thread
    private Thread musicThread;
    private Clip backgroundMusicClip;
    private volatile boolean musicPlaying = false;
    
    // Colorful theme colors
    private final Color PRIMARY_ORANGE = new Color(255, 138, 0);
    private final Color PRIMARY_BLUE = new Color(102, 178, 255);
    private final Color PRIMARY_GREEN = new Color(102, 255, 102);
    private final Color PRIMARY_YELLOW = new Color(255, 204, 0);
    private final Color PRIMARY_CYAN = new Color(51, 204, 255);
    private final Color BG_WHITE = Color.WHITE;
    private final Color TEXT_BLACK = Color.BLACK;
    private final Color GRAY_LIGHT = new Color(240, 240, 240);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientMain().build());
    }

    public void build() {
        frame = new JFrame("Quiz Matematika");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout(0, 0));
        frame.getContentPane().setBackground(BG_WHITE);

        // ===== TOP PANEL (CONNECTION) =====
        JPanel topPanel = createTopPanel();
        frame.add(topPanel, BorderLayout.NORTH);

        // ===== MAIN CONTENT AREA =====
        JPanel mainContent = new JPanel(new BorderLayout(20, 0));
        mainContent.setBackground(BG_WHITE);
        mainContent.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // LEFT SIDE - Racing Panel
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(BG_WHITE);

        // Status Label
        lblStatus = new JLabel("Ready to race");
        lblStatus.setFont(new Font("Comic Sans MS", Font.BOLD, 14));
        lblStatus.setForeground(PRIMARY_ORANGE);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        leftPanel.add(lblStatus);

        // Car Panel with rounded border
        carPanel = new CarPanel();
        carPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        carPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_ORANGE, 3),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        carPanel.setPreferredSize(new Dimension(600, 400));
        carPanel.setMaximumSize(new Dimension(600, 400));
        leftPanel.add(carPanel);

        mainContent.add(leftPanel, BorderLayout.CENTER);

        // RIGHT SIDE - Question and Answer Panel
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(BG_WHITE);
        rightPanel.add(Box.createVerticalGlue());
        
        JPanel qaPanel = createQuestionAnswerPanel();
        rightPanel.add(qaPanel);
        rightPanel.add(Box.createVerticalGlue());

        mainContent.add(rightPanel, BorderLayout.EAST);

        frame.add(mainContent, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        // Start background music
        startBackgroundMusic();
    }
    
    private void startBackgroundMusic() {
        musicThread = new Thread(() -> {
            try {
                AudioInputStream audioStream =
                    AudioSystem.getAudioInputStream(
                        getClass().getResource("/client/music/mixkit-im-hungry-808.wav")
                    );
    
                backgroundMusicClip = AudioSystem.getClip();
                backgroundMusicClip.open(audioStream);
    
                FloatControl volume =
                    (FloatControl) backgroundMusicClip.getControl(FloatControl.Type.MASTER_GAIN);
                volume.setValue(-10.0f);
    
                backgroundMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
                musicPlaying = true;
    
                System.out.println("Background music started");
    
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        musicThread.start();
    }
    
    private void stopBackgroundMusic() {
        if (backgroundMusicClip != null && backgroundMusicClip.isRunning()) {
            backgroundMusicClip.stop();
            backgroundMusicClip.close();
            musicPlaying = false;
            System.out.println("Background music stopped");
        }
    }
    
    private void toggleMusic() {
        if (backgroundMusicClip != null) {
            if (musicPlaying) {
                backgroundMusicClip.stop();
                musicPlaying = false;
                System.out.println("Music paused");
            } else {
                backgroundMusicClip.start();
                musicPlaying = true;
                System.out.println("Music resumed");
            }
        }
    }
    
    private void playSoundEffect(String soundType) {
        new Thread(() -> {
            try {
                String path = null;
    
                switch (soundType) {
                    case "correct":
                        path = "/client/music/mixkit-game-success-alert-2039.wav";
                        break;
                    case "wrong":
                        path = "/client/music/mixkit-game-error-alert-19.wav";
                        break;
                    case "win":
                        path = "/client/music/mixkit-cheering-crowd-loud-whistle-610.wav";
                        break;
                    case "countdown":
                        path = "/client/music/mixkit-start-countdown-927.wav";
                        break;
                }
    
                if (path == null) return;
    
                AudioInputStream audioStream =
                        AudioSystem.getAudioInputStream(
                            getClass().getResource(path)
                        );
    
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
    
            } catch (Exception e) {
                e.printStackTrace(); 
            }
        }).start();
    }
    

    private JPanel createTopPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 3, 0, TEXT_BLACK),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        // Title
        JLabel title = new JLabel("Quiz Matematika", SwingConstants.CENTER);
        title.setFont(new Font("Comic Sans MS", Font.BOLD, 32));
        title.setForeground(TEXT_BLACK);
        wrapper.add(title, BorderLayout.NORTH);

        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        connectionPanel.setBackground(BG_WHITE);
        connectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Server IP
        connectionPanel.add(createLabel("Server IP:"));
        tfServerIP = createTextField("127.0.0.1", 12);
        connectionPanel.add(tfServerIP);

        // Port
        connectionPanel.add(createLabel("Port:"));
        JTextField tfPort = createTextField("5000", 6);
        connectionPanel.add(tfPort);

        // Name
        connectionPanel.add(createLabel("Name:"));
        tfName = createTextField("", 10);
        connectionPanel.add(tfName);

        // Operation
        connectionPanel.add(createLabel("Operation:"));
        cbOp = new JComboBox<>(new String[]{"+", "-", "*", "/"});
        cbOp.setFont(new Font("Comic Sans MS", Font.BOLD, 14));
        cbOp.setBackground(BG_WHITE);
        cbOp.setForeground(TEXT_BLACK);
        cbOp.setFocusable(false);
        cbOp.setBorder(BorderFactory.createLineBorder(PRIMARY_BLUE, 2));
        connectionPanel.add(cbOp);

        // Connect Button
        btnConnect = createColorfulButton("Connect", PRIMARY_GREEN);
        btnConnect.addActionListener(e -> {
            String ip = tfServerIP.getText().trim();
            int port = Integer.parseInt(tfPort.getText().trim());
            connectToServer(ip, port);
        });
        connectionPanel.add(btnConnect);

        wrapper.add(connectionPanel, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel createQuestionAnswerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(GRAY_LIGHT);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_CYAN, 3),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        panel.setPreferredSize(new Dimension(350, 500));
        panel.setMaximumSize(new Dimension(350, 500));

        // Question Label
        lblQuestion = new JLabel("Menunggu soal...");
        lblQuestion.setForeground(TEXT_BLACK);
        lblQuestion.setFont(new Font("Comic Sans MS", Font.BOLD, 16));
        lblQuestion.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblQuestion.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        panel.add(lblQuestion);

        // Answer Input Section
        JPanel answerInputPanel = new JPanel();
        answerInputPanel.setLayout(new BoxLayout(answerInputPanel, BoxLayout.Y_AXIS));
        answerInputPanel.setBackground(GRAY_LIGHT);

        JLabel lblAnswerTitle = new JLabel("Your Answer:");
        lblAnswerTitle.setForeground(TEXT_BLACK);
        lblAnswerTitle.setFont(new Font("Comic Sans MS", Font.BOLD, 14));
        lblAnswerTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        answerInputPanel.add(lblAnswerTitle);

        answerInputPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        tfAnswer = new JTextField(8);
        tfAnswer.setFont(new Font("Comic Sans MS", Font.BOLD, 24));
        tfAnswer.setHorizontalAlignment(JTextField.CENTER);
        tfAnswer.setBackground(BG_WHITE);
        tfAnswer.setForeground(TEXT_BLACK);
        tfAnswer.setCaretColor(PRIMARY_ORANGE);
        tfAnswer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_ORANGE, 3),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        tfAnswer.setMaximumSize(new Dimension(180, 50));
        tfAnswer.setAlignmentX(Component.CENTER_ALIGNMENT);
        answerInputPanel.add(tfAnswer);

        answerInputPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Submit Button
        JButton btnSubmit = createColorfulButton("Submit Answer", PRIMARY_YELLOW);
        btnSubmit.setFont(new Font("Comic Sans MS", Font.BOLD, 14));
        btnSubmit.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSubmit.setMaximumSize(new Dimension(180, 40));
        btnSubmit.addActionListener(e -> submitAnswer());
        answerInputPanel.add(btnSubmit);

        tfAnswer.addActionListener(e -> submitAnswer());

        panel.add(answerInputPanel);

        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Keypad
        JPanel keypadPanel = createKeypad();
        keypadPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(keypadPanel);

        return panel;
    }

    private JPanel createKeypad() {
        JPanel panel = new JPanel(new GridLayout(4, 3, 8, 8));
        panel.setBackground(GRAY_LIGHT);
        panel.setMaximumSize(new Dimension(250, 200));

        Font keyFont = new Font("Comic Sans MS", Font.BOLD, 16);

        Color[] buttonColors = {
            PRIMARY_ORANGE, PRIMARY_BLUE, PRIMARY_GREEN,
            PRIMARY_YELLOW, PRIMARY_CYAN, PRIMARY_ORANGE,
            PRIMARY_BLUE, PRIMARY_GREEN, PRIMARY_YELLOW,
            PRIMARY_CYAN, PRIMARY_ORANGE, new Color(255, 51, 51) // Red for backspace
        };

        ActionListener keyListener = e -> {
            String key = ((JButton) e.getSource()).getText();

            if (key.equals("DEL")) {
                // Backspace - hapus karakter terakhir
                String current = tfAnswer.getText();
                if (!current.isEmpty()) {
                    tfAnswer.setText(current.substring(0, current.length() - 1));
                }
            } else if (key.equals("+/-")) {
                if (!tfAnswer.getText().isEmpty()) {
                    if (tfAnswer.getText().startsWith("-")) {
                        tfAnswer.setText(tfAnswer.getText().substring(1));
                    } else {
                        tfAnswer.setText("-" + tfAnswer.getText());
                    }
                }
            } else if (key.equals(".")) {
                if (!tfAnswer.getText().contains(".")) {
                    tfAnswer.setText(tfAnswer.getText() + ".");
                }
            } else {
                tfAnswer.setText(tfAnswer.getText() + key);
            }

            tfAnswer.requestFocus();
        };

        String[] keys = {
            "7", "8", "9",
            "4", "5", "6",
            "1", "2", "3",
            "0", ".", "DEL"  // Tombol backspace ditambahkan
        };

        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            JButton btn = new JButton(k);
            btn.setFont(keyFont);
            btn.setFocusPainted(false);
            btn.setBackground(buttonColors[i]);
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            // Hover effect
            Color originalColor = buttonColors[i];
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    btn.setBackground(originalColor.darker());
                }
                public void mouseExited(MouseEvent e) {
                    btn.setBackground(originalColor);
                }
            });
            
            btn.addActionListener(keyListener);
            panel.add(btn);
        }

        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_BLACK);
        label.setFont(new Font("Comic Sans MS", Font.BOLD, 13));
        return label;
    }

    private JTextField createTextField(String defaultText, int cols) {
        JTextField tf = new JTextField(defaultText, cols);
        tf.setFont(new Font("Comic Sans MS", Font.PLAIN, 14));
        tf.setBackground(BG_WHITE);
        tf.setForeground(TEXT_BLACK);
        tf.setCaretColor(PRIMARY_ORANGE);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_BLUE, 2),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return tf;
    }

    private JButton createColorfulButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Comic Sans MS", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 2),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bgColor.darker());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bgColor);
            }
        });
        
        return btn;
    }

    private void connectToServer(String ip, int port) {
        try {
            Socket s = new Socket(ip, port);
            out = new ObjectOutputStream(s.getOutputStream());
            in = new ObjectInputStream(s.getInputStream());

            // send CONNECT
            String name = tfName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter your name!", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            myOp = (String) cbOp.getSelectedItem();
            Message m = new Message(Message.Type.CONNECT).put("name", name).put("operation", myOp);
            out.writeObject(m);
            out.flush();

            // listen thread
            new Thread(() -> {
                try {
                    while (true) {
                        Message incoming = (Message) in.readObject();
                        handleIncoming(incoming);
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, "Disconnected from server", "Connection Lost", JOptionPane.ERROR_MESSAGE);
                        btnConnect.setEnabled(true);
                        lblStatus.setText("Disconnected from server");
                    });
                }
            }).start();

            btnConnect.setEnabled(false);
            btnConnect.setText("Connected");
            lblStatus.setText("Connected to server as " + name);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Failed to connect: " + ex.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleIncoming(Message m) {
        switch (m.type) {
            case CONNECT_ACK:
                playerId = (int) m.get("playerId");
                System.out.println("Connected as player " + playerId);
                break;
            case COUNTDOWN:
                int seconds = (int) m.get("seconds");
                int playerCount = (int) m.get("playerCount");
                SwingUtilities.invokeLater(() -> {
                    lblQuestion.setText("Game starts in " + seconds + " seconds...");
                    lblStatus.setText("Waiting for players... (" + playerCount + " joined)");
                    
                    // Stop background music when countdown starts (player 2 joined)
                    if (seconds == 10 && playerCount >= 2) {
                        stopBackgroundMusic();
                    }
                    
                    // Play countdown sound for last 3 seconds
                    if (seconds <= 10                     && seconds > 0) {
                        playSoundEffect("countdown");
                    }
                });
                break;
            case START:
                int totalPlayers = (int) m.get("playerCount");
                SwingUtilities.invokeLater(() -> {
                    lblQuestion.setText("Game Started! Get Ready...");
                    lblStatus.setText("Game in progress... (" + totalPlayers + " players)");
                });
                break;
            case QUESTION:
                int qId = (int) m.get("qId");
                String text = (String) m.get("text");
                SwingUtilities.invokeLater(() -> {
                    lblQuestion.setText(text);  // Show only the question, no "Question #X:"
                    tfAnswer.setName(String.valueOf(qId));
                    tfAnswer.setText("");
                    tfAnswer.requestFocus();
                });
                break;
            case RESULT:
                Map<Integer, Integer> posMap = (Map<Integer, Integer>) m.get("posMap");
                Map<Integer, String> names = (Map<Integer, String>) m.get("names");
                SwingUtilities.invokeLater(() -> {
                    // Always update with server data - this contains ALL players
                    carPanel.updateState(posMap, names);
                    System.out.println("Updated positions: " + posMap);
                    System.out.println("Updated names: " + names);
                });
                break;
            case GAME_OVER:
                int winner = (int) m.get("winnerId");
                String winnerName = (String) m.get("winnerName");
                SwingUtilities.invokeLater(() -> {
                    playSoundEffect("win"); // Play victory sound
                    JOptionPane.showMessageDialog(frame, 
                        "Game Over!\n\nWinner: " + winnerName, 
                        "Race Complete!", 
                        JOptionPane.INFORMATION_MESSAGE);
                    lblStatus.setText("Game finished! Winner: " + winnerName);
                });
                break;
            case ERROR:
                String msg = (String) m.get("msg");
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(frame, "Server error: " + msg, "Error", JOptionPane.ERROR_MESSAGE)
                );
                break;
            default:
                // ignore
        }
    }

    private void submitAnswer() {
        try {
            String answerText = tfAnswer.getText().trim();
            if (answerText.isEmpty()) return;
            
            int a = Integer.parseInt(answerText);
            int qId = Integer.parseInt(tfAnswer.getName());
            Message m = new Message(Message.Type.ANSWER).put("qId", qId).put("answer", a);
            out.writeObject(m);
            out.flush();
            
            tfAnswer.setText("");
            lblStatus.setText("Answer submitted!");
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid number!", "Invalid Input", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Failed to submit answer: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}