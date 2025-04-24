package client;

import javax.swing.*;
import java.awt.*;

/**
 * StatsPanel: display wins/losses/draws.
 */
public class StatsPanel extends JPanel {
    private JLabel wins   = new JLabel("Wins: 0");
    private JLabel losses = new JLabel("Losses: 0");
    private JLabel draws  = new JLabel("Draws: 0");

    public interface StatsListener {
        void onBack();
    }

    public StatsPanel(StatsListener l) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.gridx = 0; gbc.gridy = 0;

        add(wins,   gbc);
        gbc.gridy++; add(losses, gbc);
        gbc.gridy++; add(draws,  gbc);
        gbc.gridy++;

        JButton back = new JButton("Back");
        back.addActionListener(e -> l.onBack());
        add(back, gbc);
    }

    public void updateStats(int w, int l, int d) {
        wins.setText("Wins: " + w);
        losses.setText("Losses: " + l);
        draws.setText("Draws: " + d);
    }
}
