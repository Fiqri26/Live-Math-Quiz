package client;

import common.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

public class ClientMain {
    private JFrame frame;
    private JTextField tfName, tfServerIP, tfAnswer;
    private JComboBox<String> cbOp;
    private JButton btnConnect;
    private CarPanel carPanel;
    private JLabel lblQuestion;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private int playerId;
    private String myOp;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientMain().build());
    }

    public void build() {
        frame = new JFrame("🚗 Live Math Quiz Race");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 550);
        frame.setLayout(new BorderLayout());

        // ===== TOP PANEL (CONNECT) =====
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.setBackground(new Color(30, 30, 30));

        JLabel lblServer = new JLabel("Server IP:");
        lblServer.setForeground(Color.WHITE);
        top.add(lblServer);

        tfServerIP = new JTextField("127.0.0.1", 10);
        top.add(tfServerIP);

        top.add(new JLabel("Port:")).setForeground(Color.WHITE);
        JTextField tfPort = new JTextField("5000", 5);
        top.add(tfPort);

        JLabel lblName = new JLabel("Name:");
        lblName.setForeground(Color.WHITE);
        top.add(lblName);

        tfName = new JTextField(8);
        top.add(tfName);

        JLabel lblOp = new JLabel("Op:");
        lblOp.setForeground(Color.WHITE);
        top.add(lblOp);

        cbOp = new JComboBox<>(new String[]{"+", "-", "*", "/"});
        top.add(cbOp);

        btnConnect = new JButton("Connect");
        btnConnect.setBackground(new Color(0, 180, 100));
        btnConnect.setForeground(Color.WHITE);
        top.add(btnConnect);

        frame.add(top, BorderLayout.NORTH);

        // ===== CENTER CONTAINER =====
        JPanel centerContainer = new JPanel();
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));
        centerContainer.setBackground(Color.BLACK);

        // --- CAR PANEL ---
        carPanel = new CarPanel();
        carPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerContainer.add(carPanel);

        // ===== QUESTION PANEL (DI BAWAH PLAYER 2) =====
        JPanel questionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        questionPanel.setBackground(new Color(20, 20, 20));
        questionPanel.setMaximumSize(new Dimension(900, 70));

        lblQuestion = new JLabel("Menunggu soal...");
        lblQuestion.setForeground(Color.WHITE);
        lblQuestion.setFont(new Font("Segoe UI", Font.BOLD, 16));
        questionPanel.add(lblQuestion);

        tfAnswer = new JTextField(6);
        tfAnswer.setFont(new Font("Segoe UI", Font.BOLD, 16));
        tfAnswer.setHorizontalAlignment(JTextField.CENTER);
        questionPanel.add(tfAnswer);

        JButton btnSubmit = new JButton("Jawab");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSubmit.setBackground(new Color(70, 130, 255));
        btnSubmit.setForeground(Color.WHITE);
        questionPanel.add(btnSubmit);

        centerContainer.add(questionPanel);

        frame.add(centerContainer, BorderLayout.CENTER);

        // ===== ACTIONS =====
        btnConnect.addActionListener(e -> {
            String ip = tfServerIP.getText().trim();
            int port = Integer.parseInt(tfPort.getText().trim());
            connectToServer(ip, port);
        });

        btnSubmit.addActionListener(e -> submitAnswer());
        tfAnswer.addActionListener(e -> submitAnswer());

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }


    private void connectToServer(String ip, int port) {
        try {
            Socket s = new Socket(ip, port);
            out = new ObjectOutputStream(s.getOutputStream());
            in  = new ObjectInputStream(s.getInputStream());

            // send CONNECT
            String name = tfName.getText().trim();
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
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Disconnected from server"));
                    ex.printStackTrace();
                }
            }).start();

            btnConnect.setEnabled(false);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Gagal konek: " + ex.getMessage());
        }
    }

    private void handleIncoming(Message m) {
        switch (m.type) {
            case CONNECT_ACK:
                playerId = (int) m.get("playerId");
                System.out.println("Connected as player " + playerId);
                break;
            case START:
                SwingUtilities.invokeLater(() -> lblQuestion.setText("Game dimulai! Tunggu soal..."));
                break;
            case QUESTION:
                int qId = (int) m.get("qId");
                String text = (String) m.get("text");
                SwingUtilities.invokeLater(() -> {
                    lblQuestion.setText("Q#" + qId + ": " + text);
                    tfAnswer.setName(String.valueOf(qId)); // simpan qId di komponen
                    tfAnswer.requestFocus();
                });
                break;
            case RESULT:
                Map<Integer,Integer> posMap = (Map<Integer,Integer>) m.get("posMap");
                Map<Integer,String> names = (Map<Integer,String>) m.get("names");
                SwingUtilities.invokeLater(() -> carPanel.updateState(posMap, names));
                break;
            case GAME_OVER:
                int winner = (int) m.get("winnerId");
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Game over! Pemenang: Player " + winner));
                break;
            case ERROR:
                String msg = (String) m.get("msg");
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Server error: " + msg));
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
            int qId = Integer.parseInt(tfAnswer.getName()); // qId stored earlier
            Message m = new Message(Message.Type.ANSWER).put("qId", qId).put("answer", a);
            out.writeObject(m);
            out.flush();
            tfAnswer.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Gagal kirim jawaban: " + ex.getMessage());
        }
    }
}
