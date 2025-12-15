package client;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class CarPanel extends JPanel {

    private Map<Integer, Integer> pos = new HashMap<>();
    private Map<Integer, String> names = new HashMap<>();

    private final int FINISH = 10;

    private Image bgQuiz;
    private Image road;
    private Image car1;
    private Image car2;

    public CarPanel() {
        setPreferredSize(new Dimension(900, 300));
        setDoubleBuffered(true);

        bgQuiz = new ImageIcon(getClass().getResource("/client/assets/bg_quiz.png")).getImage();
        road   = new ImageIcon(getClass().getResource("/client/assets/jalan.png")).getImage();
        car1   = new ImageIcon(getClass().getResource("/client/assets/P1.png")).getImage();
        car2   = new ImageIcon(getClass().getResource("/client/assets/P2.png")).getImage();
    }

    public void updateState(Map<Integer, Integer> posMap, Map<Integer, String> nameMap) {
        this.pos = new HashMap<>(posMap);
        this.names = new HashMap<>(nameMap);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();

        // background quiz
        g.drawImage(bgQuiz, 0, 0, w, h, this);

        // road
        int roadY1 = 130;
        int roadY2 = 190;
        g.drawImage(road, 0, roadY1, w, 50, this);
        g.drawImage(road, 0, roadY2, w, 50, this);

        int stepW = (w - 150) / FINISH;

        drawCar(g, 1, car1, 40, roadY1 - 20, stepW);
        drawCar(g, 2, car2, 40, roadY2 - 20, stepW);
    }

    private void drawCar(Graphics g, int playerId, Image img, int baseX, int y, int stepW) {
        int p = pos.getOrDefault(playerId, 0);
        int x = baseX + p * stepW;

        g.drawImage(img, x, y, 150, 80, this);

        String name = names.getOrDefault(playerId, "Player " + playerId);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g.drawString(name, x, y - 5);
    }
}
