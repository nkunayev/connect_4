package client;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class StatsPanel extends JPanel {
    private final JLabel winsLabel   = new JLabel("–");
    private final JLabel lossesLabel = new JLabel("–");
    private final JLabel drawsLabel  = new JLabel("–");
    private final JButton backButton = new JButton("Back");

    public interface StatsListener { void onBack(); }

    public StatsPanel(StatsListener listener) {
        setLayout(new BorderLayout(10,10));

        JLabel title = new JLabel("Statistics", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(3,2,5,5));
        grid.add(new JLabel("Wins:"));
        grid.add(winsLabel);
        grid.add(new JLabel("Losses:"));
        grid.add(lossesLabel);
        grid.add(new JLabel("Draws:"));
        grid.add(drawsLabel);
        add(grid, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.add(backButton);
        add(south, BorderLayout.SOUTH);

        backButton.addActionListener(e -> listener.onBack());
    }

    public void updateStats(int wins, int losses, int draws) {
        winsLabel.setText(String.valueOf(wins));
        lossesLabel.setText(String.valueOf(losses));
        drawsLabel.setText(String.valueOf(draws));
    }

    /** Reset labels before fresh request. */
    public void clearStats() {
        winsLabel.setText("–");
        lossesLabel.setText("–");
        drawsLabel.setText("–");
    }
}
