package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;

public class FriendsPanel extends JPanel {
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String>            friendsList;
    private final JTextField               addField;
    private final JButton                  addButton;
    private final JButton                  backButton;

    public interface FriendsListener {
        void onAddFriend(String username);
        void onBack();
    }

    public FriendsPanel(FriendsListener listener) {
        setLayout(new BorderLayout(10,10));

        JLabel title = new JLabel("Friends", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        friendsList = new JList<>(listModel);
        add(new JScrollPane(friendsList), BorderLayout.CENTER);

        addField   = new JTextField(15);
        addButton  = new JButton("Add Friend");
        backButton = new JButton("Back");

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        south.add(addField);
        south.add(addButton);
        south.add(backButton);
        add(south, BorderLayout.SOUTH);

        addButton.addActionListener((ActionEvent e) -> {
            String user = addField.getText().trim();
            if (!user.isEmpty()) {
                listener.onAddFriend(user);
                addField.setText("");
            }
        });
        backButton.addActionListener((ActionEvent e) -> listener.onBack());
    }

    public void updateFriendsList(Map<String,Boolean> map) {
        listModel.clear();
        map.forEach((user, online) -> {
            String status = online
                ? "<font color='green'>(online)</font>"
                : "<font color='red'>(offline)</font>";
            listModel.addElement("<html>" + user + " " + status + "</html>");
        });
    }

    /** Clears previous entries before a fresh request. */
    public void clearList() {
        listModel.clear();
    }
}
