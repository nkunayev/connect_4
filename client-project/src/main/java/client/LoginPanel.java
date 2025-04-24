package client;

import javax.swing.*;
import java.awt.*;

/**
 * LoginPanel: username/password UI with Login & Register buttons.
 */
public class LoginPanel extends JPanel {
    public interface LoginListener {
        void onLogin(String username, String password);
        void onRegister(String username, String password);
    }

    public LoginPanel(LoginListener listener) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        // Title
        JLabel title = new JLabel("Connect Four Login", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(title, gbc);

        // Username
        gbc.gridwidth = 1;
        gbc.gridy++;
        add(new JLabel("Username:"), gbc);
        JTextField userField = new JTextField(15);
        gbc.gridx = 1;
        add(userField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Password:"), gbc);
        JPasswordField passField = new JPasswordField(15);
        gbc.gridx = 1;
        add(passField, gbc);

        // Buttons row
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton loginBtn    = new JButton("Login");
        JButton registerBtn = new JButton("Register");

        loginBtn.addActionListener(e -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword()).trim();
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Both fields are required.",
                    "Input Error",
                    JOptionPane.WARNING_MESSAGE);
            } else {
                listener.onLogin(u, p);
            }
        });

        registerBtn.addActionListener(e -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword()).trim();
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Both fields are required.",
                    "Input Error",
                    JOptionPane.WARNING_MESSAGE);
            } else {
                listener.onRegister(u, p);
            }
        });

        buttonRow.add(loginBtn);
        buttonRow.add(registerBtn);
        add(buttonRow, gbc);
    }
}
