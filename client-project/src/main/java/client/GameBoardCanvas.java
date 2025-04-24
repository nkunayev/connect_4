package client;

import javax.swing.*;
import java.awt.*;

/**
 * GameBoardCanvas: paints the Connect Four board & tokens.
 */
public class GameBoardCanvas extends JComponent {
    private static final int ROWS = 6, COLS = 7;
    private final int[][] board = new int[ROWS][COLS];

    /**
     * Parse the serialized "r0c0,r0c1...;r1c0..." string and repaint.
     */
    public void setBoardState(String data) {
        String[] rows = data.split(";");
        for (int r = 0; r < ROWS; r++) {
            String[] cols = r < rows.length ? rows[r].split(",") : new String[0];
            for (int c = 0; c < COLS; c++) {
                board[r][c] = (c < cols.length) ? parseIntSafe(cols[c]) : 0;
            }
        }
        repaint();
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return 0; }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth(), h = getHeight();
        int cellW = w / COLS, cellH = h / ROWS;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);

        // Board background
        g2.setColor(new Color(30, 144, 255));
        g2.fillRect(0, 0, w, h);

        // Draw tokens
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int x = c * cellW + 4;
                int y = r * cellH + 4;
                int tw = cellW - 8, th = cellH - 8;
                int v = board[r][c];
                Color fill;
                if (v == 1)      fill = new Color(220, 20, 60);
                else if (v == 2) fill = new Color(255, 215, 0);
                else             fill = Color.WHITE;

                g2.setColor(fill);
                g2.fillOval(x, y, tw, th);
            }
        }
    }
}