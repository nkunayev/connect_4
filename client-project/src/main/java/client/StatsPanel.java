package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * StatsPanel: nicely displays wins, losses, and draws
 */
public class StatsPanel extends JPanel {
    private final JLabel winsLabel   = new JLabel("–");
    private final JLabel lossesLabel = new JLabel("–");
    private final JLabel drawsLabel  = new JLabel("–");
    private final JButton backButton = new JButton("Back");

    public interface StatsListener { void onBack(); }

    public StatsPanel(StatsListener listener) {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title
        JLabel title = new JLabel("Your Statistics", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);

        // Center: vertical box of stat rows
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(20, 50, 20, 50));

        center.add(createStatRow("Wins:", winsLabel));
        center.add(Box.createVerticalStrut(10));
        center.add(createStatRow("Losses:", lossesLabel));
        center.add(Box.createVerticalStrut(10));
        center.add(createStatRow("Draws:", drawsLabel));

        add(center, BorderLayout.CENTER);

        // Back button at bottom
        JPanel south = new JPanel();
        south.setBorder(new EmptyBorder(10, 0, 0, 0));
        south.add(backButton);
        add(south, BorderLayout.SOUTH);

        backButton.addActionListener(e -> listener.onBack());
    }

    /** Helper to build a single “label + value” row. */
    private JPanel createStatRow(String labelText, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.PLAIN, 16));
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(label, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }

    /** Update all three stats at once. */
    public void updateStats(int wins, int losses, int draws) {
        winsLabel.setText(String.valueOf(wins));
        lossesLabel.setText(String.valueOf(losses));
        drawsLabel.setText(String.valueOf(draws));
    }

    /** Clear previous before a fresh request. */
    public void clearStats() {
        winsLabel.setText("–");
        lossesLabel.setText("–");
        drawsLabel.setText("–");
    }
}
