package client;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import common.Protocol;
import common.chat.Message;
import common.chat.MessageType;
import client.chat.ChatClient;

public class ConnectFourClient {
    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private NetworkHandler network;
    private String username;

    private LoginPanel loginPanel;
    private HomePanel homePanel;
    private FriendsPanel friendsPanel;
    private StatsPanel statsPanel;
    private GameBoardPanel boardPanel;

    private JTextArea chatArea;
    private JTextField chatField;
    private JLabel statusLabel;

    private ChatClient chatClient;

    public ConnectFourClient(String serverIP, int port) {
        try {
            network = new NetworkHandler(serverIP, port);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Unable to connect to server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
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

        // --- Login Screen ---
        loginPanel = new LoginPanel(new LoginPanel.LoginListener() {
            @Override
            public void onLogin(String u, String p) {
                username = u;
                network.sendMessage(Protocol.LOGIN + ":" + u + ":" + p);
                new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        return network.readMessage();
                    }
                    @Override
                    protected void done() {
                        try {
                            String resp = get();
                            if (Protocol.LOGIN_SUCCESS.equals(resp)) {
                                cardLayout.show(mainPanel, "home");
                            } else {
                                JOptionPane.showMessageDialog(frame, resp, "Login Error", JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception ex) {
                            showError("Login failed: " + ex.getMessage());
                        }
                    }
                }.execute();
            }
            @Override
            public void onRegister(String u, String p) {
                network.sendMessage(Protocol.REGISTER + ":" + u + ":" + p);
                new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        return network.readMessage();
                    }
                    @Override
                    protected void done() {
                        try {
                            String resp = get();
                            if (Protocol.REGISTER_SUCCESS.equals(resp)) {
                                JOptionPane.showMessageDialog(frame, "Registration successful! You can now log in.", "Registered", JOptionPane.INFORMATION_MESSAGE);
                            } else if (resp.startsWith(Protocol.REGISTER_ERROR + ":")) {
                                JOptionPane.showMessageDialog(frame, resp.substring((Protocol.REGISTER_ERROR + ":").length()), "Registration Error", JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception ex) {
                            showError("Registration failed: " + ex.getMessage());
                        }
                    }
                }.execute();
            }
        });
        mainPanel.add(loginPanel, "login");

        // --- Home Screen ---
        homePanel = new HomePanel(new HomePanel.HomeListener() {
            @Override
            public void onPlayOnline() {
                network.sendMessage(Protocol.JOIN_QUEUE);
                cardLayout.show(mainPanel, "game");
                startNetworkListener();
            }
            @Override
            public void onSinglePlayer() {
                JOptionPane.showMessageDialog(frame, "Single-player mode not available.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
            @Override
            public void onViewFriends() {
                network.sendMessage(Protocol.FRIEND_LIST_REQUEST);
                cardLayout.show(mainPanel, "friends");
            }
            @Override
            public void onViewStats() {
                network.sendMessage(Protocol.STATS_REQUEST);
                cardLayout.show(mainPanel, "stats");
            }
        });
        mainPanel.add(homePanel, "home");

        // --- Friends Screen ---
        friendsPanel = new FriendsPanel(new FriendsPanel.FriendsListener() {
            @Override
            public void onAddFriend(String user) {
                network.sendMessage(Protocol.FRIEND_ADD + ":" + user);
            }
            @Override
            public void onBack() {
                cardLayout.show(mainPanel, "home");
            }
        });
        mainPanel.add(friendsPanel, "friends");

        // --- Stats Screen ---
        statsPanel = new StatsPanel(() -> cardLayout.show(mainPanel, "home"));
        mainPanel.add(statsPanel, "stats");

        // --- Game Screen ---
        JPanel gamePanel = new JPanel(new BorderLayout(10, 10));
        boardPanel = new GameBoardPanel(col -> network.sendMessage(Protocol.MOVE + ":" + col));
        gamePanel.add(boardPanel, BorderLayout.CENTER);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatField = new JTextField();
        chatField.addActionListener(e -> {
            String txt = chatField.getText().trim();
            if (!txt.isEmpty()) {
                // send via game chat protocol
                network.sendMessage(Protocol.CHAT + ":" + txt);
                chatField.setText("");
            }
        });
        JPanel chatPanel = new JPanel(new BorderLayout(5,5));
        chatPanel.setBorder(BorderFactory.createTitledBorder("Game Chat"));
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatPanel.add(chatField, BorderLayout.SOUTH);
        chatPanel.setPreferredSize(new Dimension(250, 0));
        gamePanel.add(chatPanel, BorderLayout.EAST);

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        gamePanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(gamePanel, "game");

        frame.getContentPane().add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        cardLayout.show(mainPanel, "login");

        // --- Start chat client on port 5555 ---
        chatClient = new ChatClient("127.0.0.1", 5555, msg -> SwingUtilities.invokeLater(() -> {
            switch (msg.type) {
                case NEWUSER:
                    chatArea.append("[Chat] " + msg.message + "\n");
                    break;
                case DISCONNECT:
                    chatArea.append("[Chat] " + msg.message + "\n");
                    break;
                case TEXT:
                    chatArea.append(msg.message + "\n");
                    break;
            }
        }));
        chatClient.start();
    }

    private void startNetworkListener() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = network.readMessage()) != null) {
                    final String m = msg;
                    SwingUtilities.invokeLater(() -> handleMessage(m));
                }
            } catch (Exception e) {
                showError("Disconnected from server.");
            }
        }).start();
    }

    private void handleMessage(String msg) {
        if (msg.startsWith(Protocol.BOARD + ":")) {
            boardPanel.updateBoard(msg.substring((Protocol.BOARD + ":").length()));
        } else if (msg.startsWith(Protocol.CHAT + ":")) {
            chatArea.append("[Game] " + msg.substring((Protocol.CHAT + ":").length()) + "\n");
        } else if (msg.equals(Protocol.YOUR_TURN)) {
            statusLabel.setText("Your turn! Click a column.");
        } else if (msg.startsWith(Protocol.GAME_START + ":")) {
            statusLabel.setText(msg.substring((Protocol.GAME_START + ":").length()));
        } else if (msg.startsWith(Protocol.GAMEOVER + ":")) {
            String r = msg.substring((Protocol.GAMEOVER + ":").length());
            statusLabel.setText(r);
            JOptionPane.showMessageDialog(frame, r, "Game Over", JOptionPane.INFORMATION_MESSAGE);
        } else if (msg.equals(Protocol.QUEUE_JOINED)) {
            statusLabel.setText("Waiting for opponent...");
        } else if (msg.startsWith(Protocol.FRIEND_LIST_RESPONSE + ":")) {
            Map<String,Boolean> m = parseKeyBoolList(msg.substring((Protocol.FRIEND_LIST_RESPONSE + ":").length()));
            friendsPanel.updateFriendsList(m);
        } else if (msg.startsWith(Protocol.STATS_RESPONSE + ":")) {
            String[] p = msg.substring((Protocol.STATS_RESPONSE + ":").length()).split(",");
            statsPanel.updateStats(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
        } else if (msg.startsWith(Protocol.END + ":")) {
            String prompt = msg.substring((Protocol.END + ":").length());
            int choice = JOptionPane.showConfirmDialog(frame, prompt, "Play Again?", JOptionPane.YES_NO_OPTION);
            network.sendMessage(choice == JOptionPane.YES_OPTION ? "yes" : "no");
        } else if (msg.startsWith(Protocol.ERROR + ":")) {
            showError(msg.substring((Protocol.ERROR + ":").length()));
        }
    }

    private Map<String,Boolean> parseKeyBoolList(String data) {
        Map<String,Boolean> map = new HashMap<>();
        for (String e: data.split(";")) {
            String[] kv = e.split(",");
            if (kv.length == 2) map.put(kv[0], "on".equals(kv[1]));
        }
        return map;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) {
        new ConnectFourClient("127.0.0.1", 12345);
    }
}
