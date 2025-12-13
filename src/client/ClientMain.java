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
        frame = new JFrame("Quiz Race Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900,500);

        JPanel top = new JPanel();
        top.add(new JLabel("Server IP:"));
        tfServerIP = new JTextField("127.0.0.1",10);
        top.add(tfServerIP);
        top.add(new JLabel("Port:"));
        JTextField tfPort = new JTextField("5000",5);
        top.add(tfPort);

        top.add(new JLabel("Name:"));
        tfName = new JTextField(10);
        top.add(tfName);

        cbOp = new JComboBox<>(new String[]{"+","-","*","/"});
        top.add(new JLabel("Operation:"));
        top.add(cbOp);

        btnConnect = new JButton("Connect");
        top.add(btnConnect);

        frame.getContentPane().add(top, BorderLayout.NORTH);

        carPanel = new CarPanel();
        frame.getContentPane().add(carPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        lblQuestion = new JLabel("Soal akan tampil di sini");
        bottom.add(lblQuestion);
        tfAnswer = new JTextField(5);
        bottom.add(tfAnswer);
        JButton btnSubmit = new JButton("Jawab");
        bottom.add(btnSubmit);
        frame.getContentPane().add(bottom, BorderLayout.SOUTH);

        btnConnect.addActionListener(e -> {
            String ip = tfServerIP.getText().trim();
            int port = Integer.parseInt(tfPort.getText().trim());
            connectToServer(ip, port);
        });

        btnSubmit.addActionListener(e -> submitAnswer());

        tfAnswer.addActionListener(e -> submitAnswer());

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
