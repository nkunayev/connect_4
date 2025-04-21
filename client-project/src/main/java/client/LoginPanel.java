// LoginPanel.java
// Modernized and stylized login screen for Connect Four client

package client;

import javax.swing.*;
import java.awt.*;

/**
 * LoginPanel
 * Provides a clean login interface to enter a unique username.
 */
public class LoginPanel extends JPanel {
    private JTextField usernameField;
    private JButton loginButton;
    private LoginListener listener;

    /**
     * Functional interface for login callback.
     */
    public interface LoginListener {
        void onLogin(String username);
    }

    public LoginPanel(LoginListener listener) {
        this.listener = listener;
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel title = new JLabel("Connect Four");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setForeground(new Color(30, 144, 255));

        JLabel prompt = new JLabel("Enter a unique username:");
        prompt.setFont(new Font("SansSerif", Font.PLAIN, 16));

        usernameField = new JTextField(20);
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 16));

        loginButton = new JButton("Login");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        loginButton.setBackground(new Color(30, 144, 255));
        loginButton.setForeground(Color.WHITE);

        // Add components to the panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(title, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        add(prompt, gbc);

        gbc.gridy++;
        add(usernameField, gbc);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.CENTER;
        add(loginButton, gbc);

        // Event listener for login
        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            if (!username.isEmpty()) {
                listener.onLogin(username);
            } else {
                JOptionPane.showMessageDialog(this, "Username cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            }
        });
    }
}