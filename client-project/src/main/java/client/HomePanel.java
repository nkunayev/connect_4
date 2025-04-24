package client;

import javax.swing.*;
import java.awt.*;

/**
 * HomePanel: main menu after login.
 */
public class HomePanel extends JPanel {
    public interface HomeListener {
        void onPlayOnline();
        void onSinglePlayer();
        void onViewFriends();
        void onViewStats();
    }

    public HomePanel(HomeListener l) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("Welcome to Connect Four", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(title, gbc);

        gbc.gridy++;
        JButton play = new JButton("Play Online");
        play.addActionListener(e -> l.onPlayOnline());
        add(play, gbc);

        gbc.gridy++;
        JButton single = new JButton("Single Player");
        single.addActionListener(e -> l.onSinglePlayer());
        add(single, gbc);

        gbc.gridy++;
        JButton friends = new JButton("Friends");
        friends.addActionListener(e -> l.onViewFriends());
        add(friends, gbc);

        gbc.gridy++;
        JButton stats = new JButton("My Stats");
        stats.addActionListener(e -> l.onViewStats());
        add(stats, gbc);
    }
}
