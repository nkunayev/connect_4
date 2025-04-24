package client;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * FriendsPanel: view/add friends.
 */
public class FriendsPanel extends JPanel {
    private DefaultListModel<String> listModel = new DefaultListModel<>();

    public interface FriendsListener {
        void onAddFriend(String username);
        void onBack();
    }

    public FriendsPanel(FriendsListener l) {
        setLayout(new BorderLayout(10,10));
        JList<String> list = new JList<>(listModel);
        list.setBorder(BorderFactory.createTitledBorder("Friends"));
        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout());
        JTextField field = new JTextField(12);
        JButton add = new JButton("Add Friend");
        add.addActionListener(e -> l.onAddFriend(field.getText().trim()));
        JButton back = new JButton("Back");
        back.addActionListener(e -> l.onBack());

        bottom.add(field);
        bottom.add(add);
        bottom.add(back);
        add(bottom, BorderLayout.SOUTH);
    }

    public void updateFriendsList(Map<String,Boolean> m) {
        listModel.clear();
        m.forEach((user, online) -> {
            listModel.addElement(user + (online ? " (online)" : " (offline)"));
        });
    }
}
