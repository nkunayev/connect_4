// GameBoardPanel.java
// Improved visuals and interactivity for Connect Four board rendering

package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * GameBoardPanel
 * Visually enhanced board renderer for Connect Four.
 * Adds clear tokens, hover column hints, and smooth user interaction.
 */
public class GameBoardPanel extends JPanel {
    private int rows = 6, cols = 7;
    private int[][] board;

    // Customizable visual styles
    private final Color boardColor = new Color(30, 144, 255);
    private final Color emptyColor = Color.WHITE;
    private final Color player1Color = new Color(220, 20, 60); // Red
    private final Color player2Color = new Color(255, 215, 0); // Yellow
    private final Color hoverOverlay = new Color(255, 255, 255, 80);

    private final int cellSize = 90;
    private int hoverColumn = -1;

    private MoveListener moveListener;

    /**
     * Callback interface for column selection.
     */
    public interface MoveListener {
        void onColumnSelected(int col);
    }

    public GameBoardPanel(MoveListener listener) {
        this.moveListener = listener;
        this.board = new int[rows][cols];
        setPreferredSize(new Dimension(cols * cellSize, rows * cellSize));
        setBackground(Color.WHITE);

        // Column hover indicator
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int newCol = e.getX() / cellSize;
                if (newCol != hoverColumn) {
                    hoverColumn = newCol;
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) {
                hoverColumn = -1;
                repaint();
            }

            public void mouseClicked(MouseEvent e) {
                int col = e.getX() / cellSize;
                if (moveListener != null && col >= 0 && col < cols) {
                    moveListener.onColumnSelected(col);
                }
            }
        });
    }

    /**
     * Updates the board from serialized string.
     */
    public void updateBoard(String serialized) {
        String[] rowsStr = serialized.split(";");
        for (int i = 0; i < rowsStr.length && i < rows; i++) {
            String[] colsStr = rowsStr[i].split(",");
            for (int j = 0; j < colsStr.length && j < cols; j++) {
                try {
                    board[i][j] = Integer.parseInt(colsStr[j]);
                } catch (NumberFormatException e) {
                    board[i][j] = 0;
                }
            }
        }
        repaint();
    }

    /**
     * Render the board and tokens.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the board background
        g2.setColor(boardColor);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Draw hover effect for column
        if (hoverColumn >= 0 && hoverColumn < cols) {
            g2.setColor(hoverOverlay);
            g2.fillRect(hoverColumn * cellSize, 0, cellSize, rows * cellSize);
        }

        // Draw cells with tokens
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = col * cellSize;
                int y = row * cellSize;

                Color color;
                switch (board[row][col]) {
                    case 1:
                        color = player1Color;
                        break;
                    case 2:
                        color = player2Color;
                        break;
                    default:
                        color = emptyColor;
                }

                g2.setColor(color);
                g2.fillOval(x + 8, y + 8, cellSize - 16, cellSize - 16);
            }
        }
    }
}
