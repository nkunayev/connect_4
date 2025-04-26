// ConnectFourClient.java

package client;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import common.Protocol;
import common.chat.Message;
import client.NetworkHandler;
import client.chat.ChatClient;

// AI imports
import javax.swing.Timer;
import client.GameBoard;
import client.AIPlayer;

/**
 * ConnectFourClient: GUI client for networked Connect Four,
 * with optional single-player AI mode and local replay.
 */
public class ConnectFourClient {
    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    private LoginPanel loginPanel;
    private HomePanel homePanel;
    private FriendsPanel friendsPanel;
    private StatsPanel statsPanel;
    private GameBoardPanel boardPanel;

    private JTextArea chatArea;
    private JTextField chatField;
    private JLabel statusLabel;

    private NetworkHandler network;
    private ChatClient chatClient;
    private String username;

    // Single-player AI fields
    private boolean   singlePlayerMode = false;
    private GameBoard localBoard;
    private AIPlayer  ai;
    private boolean   gameOver = false;

    public ConnectFourClient(String serverIP, int port) {
        try {
            network = new NetworkHandler(serverIP, port);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Unable to connect to server.",
                "Connection Error",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        SwingUtilities.invokeLater(this::initGUI);
    }

    private void initGUI() {
        frame = new JFrame("Connect Four Networked");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);

        cardLayout = new CardLayout();
        mainPanel  = new JPanel(cardLayout);

        // --- Login Screen ---
        loginPanel = new LoginPanel(new LoginPanel.LoginListener() {
            @Override public void onLogin(String u, String p) {
                username = u;
                network.sendMessage(Protocol.LOGIN + ":" + u + ":" + p);
                new SwingWorker<String, Void>() {
                    @Override protected String doInBackground() throws Exception {
                        return network.readMessage();
                    }
                    @Override protected void done() {
                        try {
                            String resp = get();
                            if (Protocol.LOGIN_SUCCESS.equals(resp)
                                || (resp != null && resp.startsWith(Protocol.LOGIN_SUCCESS + ":"))) {
                                cardLayout.show(mainPanel, "home");
                                startNetworkListener();
                                startGlobalChat();
                            } else {
                                JOptionPane.showMessageDialog(frame,
                                    resp == null ? "No response" : resp,
                                    "Login Error",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(frame,
                                "Login failed: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }.execute();
            }
            @Override public void onRegister(String u, String p) {
                network.sendMessage(Protocol.REGISTER + ":" + u + ":" + p);
                new SwingWorker<String, Void>() {
                    @Override protected String doInBackground() throws Exception {
                        return network.readMessage();
                    }
                    @Override protected void done() {
                        try {
                            String resp = get();
                            if (Protocol.REGISTER_SUCCESS.equals(resp)
                                || (resp != null && resp.startsWith(Protocol.REGISTER_SUCCESS + ":"))) {
                                JOptionPane.showMessageDialog(frame,
                                    "Registration successful! Please log in.",
                                    "Registered",
                                    JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(frame,
                                    resp == null ? "No response" : resp,
                                    "Registration Error",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(frame,
                                "Registration failed: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }.execute();
            }
        });
        mainPanel.add(loginPanel, "login");

        // --- Home Screen ---
        homePanel = new HomePanel(new HomePanel.HomeListener() {
            @Override public void onPlayOnline() {
                singlePlayerMode = false;
                gameOver = false;
                network.sendMessage(Protocol.JOIN_QUEUE);
                cardLayout.show(mainPanel, "game");
                statusLabel.setText("Waiting for opponent...");
                boardPanel.setInteractive(false);
                chatField.setEnabled(false);
            }
            @Override public void onSinglePlayer() {
                singlePlayerMode = true;
                gameOver = false;
                localBoard = new GameBoard();
                ai         = new AIPlayer(2, 5);
                boardPanel.updateBoard(localBoard.serialize());
                boardPanel.setInteractive(true);
                statusLabel.setText("Your turn");
                cardLayout.show(mainPanel, "game");
            }
            @Override public void onViewFriends() {
                network.sendMessage(Protocol.FRIEND_LIST_REQUEST);
                cardLayout.show(mainPanel, "friends");
            }
            @Override public void onViewStats() {
                network.sendMessage(Protocol.STATS_REQUEST);
                cardLayout.show(mainPanel, "stats");
            }
        });
        mainPanel.add(homePanel, "home");

        // --- Friends Screen ---
        friendsPanel = new FriendsPanel(new FriendsPanel.FriendsListener() {
            @Override public void onAddFriend(String user) {
                network.sendMessage(Protocol.FRIEND_ADD + ":" + user);
            }
            @Override public void onBack() {
                cardLayout.show(mainPanel, "home");
            }
        });
        mainPanel.add(friendsPanel, "friends");

        // --- Stats Screen ---
        statsPanel = new StatsPanel(() -> cardLayout.show(mainPanel, "home"));
        mainPanel.add(statsPanel, "stats");

        // --- Game Screen ---
        JPanel gamePanel = new JPanel(new BorderLayout(10,10));
        boardPanel = new GameBoardPanel(col -> {
            if (gameOver) return;
            if (singlePlayerMode) {
                // Human move
                int row = localBoard.dropToken(col, 1);
                if (row < 0) return;
                boardPanel.updateBoard(localBoard.serialize());
                if (localBoard.checkWin(1)) {
                    statusLabel.setText("You win!");
                    boardPanel.setInteractive(false);
                    gameOver = true;
                    promptReplay();
                    return;
                }
                if (localBoard.isFull()) {
                    statusLabel.setText("Draw!");
                    boardPanel.setInteractive(false);
                    gameOver = true;
                    promptReplay();
                    return;
                }
                // AI move
                statusLabel.setText("AI is thinking...");
                boardPanel.setInteractive(false);
                Timer t = new Timer(300, e -> {
                    int aiCol = ai.chooseColumn(localBoard);
                    localBoard.dropToken(aiCol, 2);
                    boardPanel.updateBoard(localBoard.serialize());
                    if (localBoard.checkWin(2)) {
                        statusLabel.setText("AI wins!");
                        boardPanel.setInteractive(false);
                        gameOver = true;
                        promptReplay();
                    } else if (localBoard.isFull()) {
                        statusLabel.setText("Draw!");
                        boardPanel.setInteractive(false);
                        gameOver = true;
                        promptReplay();
                    } else {
                        statusLabel.setText("Your turn");
                        boardPanel.setInteractive(true);
                    }
                });
                t.setRepeats(false);
                t.start();
            } else {
                network.sendMessage(Protocol.MOVE + ":" + col);
            }
        });
        boardPanel.setInteractive(false);
        gamePanel.add(boardPanel, BorderLayout.CENTER);

        // Chat panel
        chatArea = new JTextArea(); chatArea.setEditable(false); chatArea.setLineWrap(true);
        chatField = new JTextField();
        chatField.addActionListener(e -> {
            String txt = chatField.getText().trim();
            if (!txt.isEmpty()) {
                chatClient.send(new Message(username + ": " + txt));
                chatField.setText("");
            }
        });
        JPanel chatPanel = new JPanel(new BorderLayout(5,5));
        chatPanel.setBorder(BorderFactory.createTitledBorder("Game Chat"));
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatPanel.add(chatField, BorderLayout.SOUTH);
        chatPanel.setPreferredSize(new Dimension(250,0));
        gamePanel.add(chatPanel, BorderLayout.EAST);

        // Status label
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        gamePanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(gamePanel, "game");

        frame.getContentPane().add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        cardLayout.show(mainPanel, "login");
    }

    private void promptReplay() {
        int choice = JOptionPane.showConfirmDialog(
            frame, "Play again?", "Replay", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            localBoard = new GameBoard();
            boardPanel.updateBoard(localBoard.serialize());
            gameOver = false;
            statusLabel.setText("Your turn");
            boardPanel.setInteractive(true);
        } else {
            cardLayout.show(mainPanel, "home");
        }
    }

    private void startNetworkListener() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = network.readMessage()) != null) {
                    if (!singlePlayerMode) {
                        final String m = msg;
                        SwingUtilities.invokeLater(() -> handleMessage(m));
                    }
                }
            } catch (Exception ignored) {}
        }, "NetListener").start();
    }

    private void startGlobalChat() {
        chatClient = new ChatClient(
            "127.0.0.1", 5555,
            m -> SwingUtilities.invokeLater(() -> chatArea.append(m.message + "\n"))
        );
        chatClient.start();
    }

    private void handleMessage(String msg) {
        System.out.println("CLIENT ← " + msg);

        if (msg.startsWith(Protocol.BOARD + ":")) {
            boardPanel.updateBoard(msg.substring((Protocol.BOARD + ":").length()));
        }
        else if (msg.equals(Protocol.YOUR_TURN)) {
            statusLabel.setText("Your turn! Click a column.");
            boardPanel.setInteractive(true);
            gameOver = false;
        }
        else if (msg.startsWith(Protocol.STATUS + ":")) {
            statusLabel.setText(msg.substring((Protocol.STATUS + ":").length()));
            boardPanel.setInteractive(false);
        }
        else if (msg.startsWith(Protocol.GAME_START + ":")) {
            statusLabel.setText(msg.substring((Protocol.GAME_START + ":").length()));
            boardPanel.setInteractive(true);
            gameOver = false;
        }
        else if (msg.startsWith(Protocol.GAMEOVER + ":")) {
            String r = msg.substring((Protocol.GAMEOVER + ":").length());
            JOptionPane.showMessageDialog(frame, r, "Game Over", JOptionPane.INFORMATION_MESSAGE);
            boardPanel.setInteractive(false);
            gameOver = true;
        }
        else if (msg.equals(Protocol.QUEUE_JOINED)) {
            statusLabel.setText("Waiting for opponent...");
            boardPanel.setInteractive(false);
        }
        else if (msg.startsWith(Protocol.FRIEND_LIST_RESPONSE + ":")) {
            friendsPanel.updateFriendsList(parseKeyBoolList(
                msg.substring((Protocol.FRIEND_LIST_RESPONSE + ":").length())));
        }
        else if (msg.equals(Protocol.FRIEND_ADD_SUCCESS)) {
            JOptionPane.showMessageDialog(frame,
                "Friend added successfully.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
        else if (msg.startsWith(Protocol.FRIEND_ADD_ERROR + ":")) {
            JOptionPane.showMessageDialog(frame,
                msg.substring((Protocol.FRIEND_ADD_ERROR + ":").length()),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
        else if (msg.startsWith(Protocol.STATS_RESPONSE + ":")) {
            String[] parts = msg.substring((Protocol.STATS_RESPONSE + ":").length()).split(",");
            statsPanel.updateStats(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
            );
        }
        else if (msg.startsWith(Protocol.ERROR + ":")) {
            JOptionPane.showMessageDialog(frame,
                msg.substring((Protocol.ERROR + ":").length()),
                "Error", JOptionPane.ERROR_MESSAGE);
        }

        chatField.setEnabled(true);
    }

    private Map<String, Boolean> parseKeyBoolList(String d) {
        Map<String, Boolean> m = new HashMap<>();
        for (String e : d.split(";")) {
            String[] kv = e.split(",");
            if (kv.length == 2) m.put(kv[0], "on".equals(kv[1]));
        }
        return m;
    }

    public static void main(String[] args) {
        new ConnectFourClient("127.0.0.1", 12345);
    }
}
