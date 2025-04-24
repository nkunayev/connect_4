package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * GameBoardPanel: a JLayeredPane containing:
 *  - GameBoardCanvas on layer 0
 *  - Seven transparent JButtons on layer 1 for column clicks
 */
public class GameBoardPanel extends JLayeredPane {
    private final GameBoardCanvas canvas;
    private final JPanel overlay;
    private final JButton[] columnButtons = new JButton[7];

    public interface MoveListener {
        void onColumnSelected(int col);
    }

    public GameBoardPanel(MoveListener listener) {
        setLayout(null);

        // Layer 0: drawing canvas
        canvas = new GameBoardCanvas();
        add(canvas, JLayeredPane.DEFAULT_LAYER);

        // Layer 1: invisible buttons for input
        overlay = new JPanel(null);
        overlay.setOpaque(false);
        for (int i = 0; i < 7; i++) {
            JButton btn = new JButton();
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            final int col = i;
            btn.addActionListener(e -> listener.onColumnSelected(col));
            columnButtons[i] = btn;
            overlay.add(btn);
        }
        add(overlay, JLayeredPane.PALETTE_LAYER);

        // Responsive layout
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = getWidth(), h = getHeight();
                canvas.setBounds(0, 0, w, h);
                overlay.setBounds(0, 0, w, h);
                int colW = w / 7;
                for (int c = 0; c < 7; c++) {
                    columnButtons[c].setBounds(c * colW, 0, colW, h);
                }
            }
        });
    }

    /**
     * Update the board state (called by ConnectFourClient.handleMessage).
     */
    public void updateBoard(String serialized) {
        canvas.setBoardState(serialized);
    }
}