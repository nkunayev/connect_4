// GameBoardPanel.java

package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Consumer;

/**
 * GameBoardPanel: a JLayeredPane containing:
 *  - GameBoardCanvas on layer 0
 *  - Seven transparent JButtons on layer 1 for column clicks
 */
public class GameBoardPanel extends JLayeredPane {
    private final GameBoardCanvas canvas;
    private final JPanel overlay;
    private final JButton[] columnButtons = new JButton[7];

    public GameBoardPanel(Consumer<Integer> onColumnClick) {
        setLayout(null);
        canvas = new GameBoardCanvas();
        overlay = new JPanel(null);
        overlay.setOpaque(false);

        for (int i = 0; i < 7; i++) {
            JButton btn = new JButton();
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            final int col = i;
            btn.addActionListener(e -> onColumnClick.accept(col));
            columnButtons[i] = btn;
            overlay.add(btn, Integer.valueOf(1));
        }

        add(canvas, Integer.valueOf(0));
        add(overlay, Integer.valueOf(1));

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

    /** Enable or disable user interaction (dropping tokens). */
    public void setInteractive(boolean on) {
        for (JButton btn : columnButtons) {
            btn.setEnabled(on);
        }
    }

    /** Update the board state (called by client). */
    public void updateBoard(String serialized) {
        canvas.setBoardState(serialized);
    }
}
