package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;

/**
 * FriendsPanel: view/add friends.
 */
public class FriendsPanel extends JPanel {
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String>            friendsList;
    private final JTextField               addField;
    private final JButton                  addButton;
    private final JButton                  backButton;
    private final FriendsListener          listener;

    public interface FriendsListener {
        void onAddFriend(String username);
        void onBack();
    }

    public FriendsPanel(FriendsListener listener) {
        this.listener = listener;
        setLayout(new BorderLayout(10,10));

        // --- Title ---
        JLabel title = new JLabel("Friends", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        // --- The scrolling list ---
        friendsList = new JList<>(listModel);
        friendsList.setCellRenderer(new DefaultListCellRenderer());
        add(new JScrollPane(friendsList), BorderLayout.CENTER);

        // --- Input field + buttons ---
        addField   = new JTextField(15);
        addButton  = new JButton("Add Friend");
        backButton = new JButton("Back");

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        south.add(addField);
        south.add(addButton);
        south.add(backButton);
        add(south, BorderLayout.SOUTH);

        // --- Wire up the buttons ---
        addButton.addActionListener((ActionEvent e) -> {
            String user = addField.getText().trim();
            if (!user.isEmpty()) {
                listener.onAddFriend(user);
                addField.setText("");
            }
        });
        backButton.addActionListener((ActionEvent e) -> listener.onBack());
    }

    /**
     * Populate the list with the given map of username→online-status.
     * Expects values true=online, false=offline.
     */
    public void updateFriendsList(Map<String,Boolean> m) {
        listModel.clear();
        m.forEach((user, online) -> {
            String status = online
                ? "<font color='green'>(online)</font>"
                : "<font color='red'>(offline)</font>";
            listModel.addElement("<html>" + user + " " + status + "</html>");
        });
    }
}
