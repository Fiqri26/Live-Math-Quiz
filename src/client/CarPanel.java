package client;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class CarPanel extends JPanel {
    private Map<Integer, Integer> positions = new HashMap<>();
    private Map<Integer, String> names = new HashMap<>();
    
    // Warna untuk setiap player
    private final Color[] PLAYER_COLORS = {
        new Color(255, 138, 0),   // Orange
        new Color(102, 178, 255), // Blue
        new Color(102, 255, 102), // Green
        new Color(255, 204, 0),   // Yellow
        new Color(255, 102, 178), // Pink
        new Color(178, 102, 255), // Purple
        new Color(255, 153, 51),  // Light Orange
        new Color(51, 204, 204)   // Cyan
    };
    
    private final Color TEXT_BLACK = Color.BLACK;

    public CarPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 400));
    }

    public void updateState(Map<Integer, Integer> newPos, Map<Integer, String> newNames) {
        this.positions = newPos;
        this.names = newNames;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int numPlayers = positions.size();
        
        if (numPlayers == 0) {
            // Show waiting message
            g2.setColor(new Color(150, 150, 150));
            g2.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
            String msg = "Waiting for players to join...";
            FontMetrics fm = g2.getFontMetrics();
            int msgWidth = fm.stringWidth(msg);
            g2.drawString(msg, (width - msgWidth) / 2, height / 2);
            return;
        }

        // Calculate track dimensions based on number of players
        int availableHeight = height - 40; // Leave some margin
        int trackHeight = Math.min(60, availableHeight / numPlayers - 10);
        int trackSpacing = Math.max(5, (availableHeight - (numPlayers * trackHeight)) / (numPlayers + 1));
        
        int startY = trackSpacing;

        // Sort players by ID for consistent ordering
        java.util.List<Integer> sortedPlayerIds = new ArrayList<>(positions.keySet());
        Collections.sort(sortedPlayerIds);

        int trackIndex = 0;
        for (int playerId : sortedPlayerIds) {
            int position = positions.get(playerId);
            String playerName = names.getOrDefault(playerId, "Player " + playerId);

            int y = startY + (trackIndex * (trackHeight + trackSpacing));
            
            // Draw track background
            g2.setColor(new Color(60, 60, 60));
            g2.fillRect(50, y, width - 100, trackHeight);
            
            // Draw lane dividers (white dashes)
            g2.setColor(new Color(200, 200, 200));
            for (int x = 60; x < width - 100; x += 20) {
                g2.fillRect(x, y + trackHeight / 2 - 1, 10, 2);
            }
            
            // Draw finish line
            g2.setColor(Color.WHITE);
            int finishX = width - 70;
            for (int i = 0; i < trackHeight; i += 8) {
                if ((i / 8) % 2 == 0) {
                    g2.fillRect(finishX, y + i, 20, 8);
                } else {
                    g2.setColor(Color.BLACK);
                    g2.fillRect(finishX, y + i, 20, 8);
                    g2.setColor(Color.WHITE);
                }
            }

            // Draw car
            int carX = 50 + (int) ((width - 150) * (position / 100.0));
            int carY = y + (trackHeight - 30) / 2;
            Color carColor = PLAYER_COLORS[(playerId - 1) % PLAYER_COLORS.length];
            drawCar(g2, carX, carY, carColor);
            
            // Draw player name above car (follows the car) - WHITE COLOR
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Comic Sans MS", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int nameWidth = fm.stringWidth(playerName);
            
            // Draw shadow for better visibility
            g2.setColor(new Color(0, 0, 0, 100));
            g2.drawString(playerName, carX + 26 - (nameWidth / 2), carY - 4);
            
            // Draw name in white
            g2.setColor(Color.WHITE);
            g2.drawString(playerName, carX + 25 - (nameWidth / 2), carY - 5);
            
            // Draw progress percentage below car (follows the car) - WHITE COLOR
            g2.setFont(new Font("Comic Sans MS", Font.BOLD, 10));
            String progressText = position + "%";
            int progressWidth = fm.stringWidth(progressText);
            
            // Draw shadow for progress
            g2.setColor(new Color(0, 0, 0, 100));
            g2.drawString(progressText, carX + 26 - (progressWidth / 2), carY + 46);
            
            // Draw progress in white
            g2.setColor(Color.WHITE);
            g2.drawString(progressText, carX + 25 - (progressWidth / 2), carY + 45);

            trackIndex++;
        }
    }

    private void drawCar(Graphics2D g2, int x, int y, Color color) {
        // Shadow
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillOval(x + 2, y + 28, 50, 8);
        
        // Car body
        g2.setColor(color);
        int[] bodyX = {x + 5, x + 45, x + 50, x + 48, x + 2, x};
        int[] bodyY = {y + 5, y + 5, y + 10, y + 25, y + 25, y + 10};
        g2.fillPolygon(bodyX, bodyY, 6);
        
        // Windshield
        g2.setColor(new Color(150, 200, 255, 150));
        int[] windX = {x + 12, x + 35, x + 38, x + 15};
        int[] windY = {y + 8, y + 8, y + 18, y + 18};
        g2.fillPolygon(windX, windY, 4);
        
        // Car details (racing stripes)
        g2.setColor(Color.WHITE);
        g2.fillRect(x + 22, y + 7, 2, 16);
        g2.fillRect(x + 28, y + 7, 2, 16);
        
        // Wheels
        g2.setColor(Color.BLACK);
        g2.fillOval(x + 5, y + 20, 12, 12);
        g2.fillOval(x + 35, y + 20, 12, 12);
        
        // Wheel rims
        g2.setColor(new Color(200, 200, 200));
        g2.fillOval(x + 7, y + 22, 8, 8);
        g2.fillOval(x + 37, y + 22, 8, 8);
        
        // Headlights
        g2.setColor(new Color(255, 255, 200));
        g2.fillOval(x + 45, y + 12, 4, 4);
        g2.fillOval(x + 45, y + 18, 4, 4);
    }
}