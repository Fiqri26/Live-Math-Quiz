package client;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.HashMap;

public class CarPanel extends JPanel {
    private Map<Integer,Integer> pos = new HashMap<>();
    private Map<Integer,String> names = new HashMap<>();
    private final int finish = 10;

    public CarPanel() {
        setPreferredSize(new Dimension(800,200));
    }

    public void updateState(Map<Integer,Integer> posMap, Map<Integer,String> nameMap) {
        this.pos = new HashMap<>(posMap);
        this.names = new HashMap<>(nameMap);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();

        // draw track
        g.setColor(Color.DARK_GRAY);
        g.fillRect(50, h/3, w-100, h/3);
        // finish line
        int stepWidth = (w-100) / (finish);
        g.setColor(Color.WHITE);
        for (int i=0;i<finish;i++) {
            g.fillRect(50 + i*stepWidth + stepWidth - 4, h/3, 4, h/3);
        }

        // draw two cars by playerId 1 & 2
        drawCar(g, 1, Color.ORANGE, 50, h/3 - 40, stepWidth);
        drawCar(g, 2, Color.CYAN, 50, h/3 + 60, stepWidth);
    }

    private void drawCar(Graphics g, int playerId, Color c, int baseX, int y, int stepW) {
        int p = pos.getOrDefault(playerId, 0);
        int x = baseX + p * stepW;
        g.setColor(c);
        g.fillRoundRect(x, y, 80, 30, 10, 10);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, 80, 30, 10, 10);
        // name
        String nm = names.getOrDefault(playerId, "P"+playerId);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
        g.drawString(nm, x+10, y-8);
    }
}
