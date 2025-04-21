// ConnectFourClient.java
// Fully optimized client for smooth, responsive, and freeze-free Connect Four experience

package client;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ConnectFourClient {
    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private NetworkHandler network;
    private GameBoardPanel boardPanel;
    private JTextArea chatArea;
    private JTextField chatField;
    private JLabel statusLabel;
    private String username;

    public ConnectFourClient(String serverIP, int port) {
        try {
            network = new NetworkHandler(serverIP, port);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Unable to connect to the server.");
            System.exit(1);
        }
        SwingUtilities.invokeLater(this::initGUI);
    }

    private void initGUI() {
        frame = new JFrame("Connect Four Networked");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        JPanel loginPanel = new LoginPanel(username -> {
            this.username = username;
            network.sendMessage("LOGIN:" + username);
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    return network.readMessage();
                }

                @Override
                protected void done() {
                    try {
                        String response = get();
                        if ("LOGIN_SUCCESS".equals(response)) {
                            cardLayout.show(mainPanel, "game");
                            startNetworkListener();
                        } else {
                            JOptionPane.showMessageDialog(frame, response, "Login Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        showError("Login failed: " + ex.getMessage());
                    }
                }
            }.execute();
        });

        JPanel gamePanel = new JPanel(new BorderLayout(10, 10));
        boardPanel = new GameBoardPanel(col -> network.sendMessage("MOVE:" + col));
        gamePanel.add(boardPanel, BorderLayout.CENTER);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatField = new JTextField();
        chatField.addActionListener(e -> {
            String msg = chatField.getText().trim();
            if (!msg.isEmpty()) {
                network.sendMessage("CHAT:" + msg);
                chatField.setText("");
            }
        });
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatPanel.add(chatField, BorderLayout.SOUTH);
        chatPanel.setPreferredSize(new Dimension(250, 0));
        gamePanel.add(chatPanel, BorderLayout.EAST);

        statusLabel = new JLabel("Connecting...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        gamePanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(loginPanel, "login");
        mainPanel.add(gamePanel, "game");
        frame.getContentPane().add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void startNetworkListener() {
        Thread listenerThread = new Thread(() -> {
            try {
                String msg;
                while ((msg = network.readMessage()) != null) {
                    final String received = msg;
                    SwingUtilities.invokeLater(() -> handleMessage(received));
                }
            } catch (IOException e) {
                showError("Disconnected from server.");
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleMessage(String msg) {
        if (msg.startsWith("BOARD:")) {
            boardPanel.updateBoard(msg.substring(6));
        } else if (msg.startsWith("CHAT:")) {
            chatArea.append(msg.substring(5) + "\n");
        } else if (msg.startsWith("YOUR_TURN")) {
            statusLabel.setText("Your turn! Click a column.");
        } else if (msg.startsWith("GAMEOVER:")) {
            String result = msg.substring(9);
            statusLabel.setText(result);
            JOptionPane.showMessageDialog(frame, result, "Game Over", JOptionPane.INFORMATION_MESSAGE);
        } else if (msg.startsWith("END:")) {
            String prompt = msg.substring(4);
            int option = JOptionPane.showConfirmDialog(frame, prompt, "Play Again?", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                boardPanel.updateBoard("0,0,0,0,0,0,0;0,0,0,0,0,0,0;0,0,0,0,0,0,0;0,0,0,0,0,0,0;0,0,0,0,0,0,0;0,0,0,0,0,0,0");
                statusLabel.setText("Waiting for new game to begin...");
            } else {
                System.exit(0);
            }
        } else if (msg.startsWith("GAME_START:")) {
            statusLabel.setText(msg.substring(11));
        } else if (msg.startsWith("ERROR:")) {
            showError(msg.substring(6));
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) {
        new ConnectFourClient("127.0.0.1", 12345);
    }
}
