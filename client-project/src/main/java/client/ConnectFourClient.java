package client;

import javax.swing.*;
import javax.swing.text.*;
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

public class ConnectFourClient {
    private static final int ROWS = 6, COLS = 7;

    private JFrame       frame;
    private CardLayout   cardLayout;
    private JPanel       mainPanel;
    private LoginPanel   loginPanel;
    private HomePanel    homePanel;
    private FriendsPanel friendsPanel;
    private StatsPanel   statsPanel;
    private GameBoardPanel boardPanel;
    private JTextPane    chatArea;
    private JTextField   chatField;
    private JLabel       statusLabel;
    private NetworkHandler network;
    private String       username;

    // === AI fields ===
    private boolean singlePlayerMode = false;
    private GameBoard localBoard;
    private AIPlayer  ai;
    private boolean   gameOver = false;

    public ConnectFourClient(String serverIP, int port) {
        try {
            network = new NetworkHandler(serverIP, port);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Unable to connect to server:\n" + e.getMessage(),
                "Connection Error", JOptionPane.ERROR_MESSAGE);
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
                new SwingWorker<String,Void>() {
                    @Override protected String doInBackground() throws Exception {
                        return network.readMessage();
                    }
                    @Override protected void done() {
                        try {
                            String resp = get();
                            if (Protocol.LOGIN_SUCCESS.equals(resp)) {
                                cardLayout.show(mainPanel, "home");
                                startNetworkListener();
                            } else {
                                JOptionPane.showMessageDialog(frame, resp,
                                    "Login Error", JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception ex) {
                            showError("Login failed: " + ex.getMessage());
                        }
                    }
                }.execute();
            }
            @Override public void onRegister(String u, String p) {
                network.sendMessage(Protocol.REGISTER + ":" + u + ":" + p);
                new SwingWorker<String,Void>() {
                    @Override protected String doInBackground() throws Exception {
                        return network.readMessage();
                    }
                    @Override protected void done() {
                        try {
                            String resp = get();
                            if (Protocol.REGISTER_SUCCESS.equals(resp)) {
                                JOptionPane.showMessageDialog(frame,
                                    "Registration successful! Please log in.",
                                    "Registered", JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(frame,
                                    resp.substring((Protocol.REGISTER_ERROR + ":").length()),
                                    "Registration Error", JOptionPane.ERROR_MESSAGE);
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
            @Override public void onPlayOnline() {
                singlePlayerMode = false;
                gameOver = false;
                boardPanel.updateBoard(emptyBoardString());
                chatArea.setText("");
                statusLabel.setText("Waiting for opponent...");
                boardPanel.setInteractive(false);
                chatField.setEnabled(true);

                network.sendMessage(Protocol.JOIN_QUEUE);
                cardLayout.show(mainPanel, "game");
            }
            @Override public void onSinglePlayer() {
                singlePlayerMode = true;
                gameOver = false;
                localBoard = new GameBoard();
                ai         = new AIPlayer(2, 5);
                boardPanel.updateBoard(localBoard.serialize());
                boardPanel.setInteractive(true);
                chatArea.setText("");
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
            @Override public void onAddFriend(String u) {
                network.sendMessage(Protocol.FRIEND_ADD + ":" + u);
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
        JPanel gamePane = new JPanel(new BorderLayout(10,10));
        boardPanel = new GameBoardPanel(col -> {
            if (singlePlayerMode) {
                if (gameOver) return;
                // human move
                int row = localBoard.dropToken(col, 1);
                if (row < 0) return;
                boardPanel.updateBoard(localBoard.serialize());
                if (localBoard.checkWin(1)) {
                    statusLabel.setText("You win!");
                    boardPanel.setInteractive(false);
                    gameOver = true;
                    promptAiReplay();
                    return;
                }
                if (localBoard.isFull()) {
                    statusLabel.setText("Draw!");
                    boardPanel.setInteractive(false);
                    gameOver = true;
                    promptAiReplay();
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
                        promptAiReplay();
                    } else if (localBoard.isFull()) {
                        statusLabel.setText("Draw!");
                        boardPanel.setInteractive(false);
                        gameOver = true;
                        promptAiReplay();
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
        gamePane.add(boardPanel, BorderLayout.CENTER);

        chatArea = new JTextPane();
        chatArea.setEditable(false);

        chatField = new JTextField();
        chatField.addActionListener(e -> {
            String txt = chatField.getText().trim();
            if (!txt.isEmpty()) {
                network.sendMessage(Protocol.CHAT + ":" + username + ": " + txt);
                chatField.setText("");
            }
        });

        JPanel chatPane = new JPanel(new BorderLayout(5,5));
        chatPane.setBorder(BorderFactory.createTitledBorder("Game Chat"));
        chatPane.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatPane.add(chatField, BorderLayout.SOUTH);
        chatPane.setPreferredSize(new Dimension(250,0));
        gamePane.add(chatPane, BorderLayout.EAST);

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        gamePane.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(gamePane, "game");

        frame.getContentPane().add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        cardLayout.show(mainPanel, "login");
    }

    private void promptAiReplay() {
        String prompt = "Play again? (yes/no)";
        int choice = JOptionPane.showConfirmDialog(frame, prompt, "Play Again?", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            localBoard = new GameBoard();
            boardPanel.updateBoard(localBoard.serialize());
            gameOver = false;
            statusLabel.setText("Your turn");
            boardPanel.setInteractive(true);
            chatArea.setText("");
        } else {
            cardLayout.show(mainPanel, "home");
        }
    }

    private String emptyBoardString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                sb.append('0');
                if (c < COLS-1) sb.append(',');
            }
            if (r < ROWS-1) sb.append(';');
        }
        return sb.toString();
    }

    private void startNetworkListener() {
        new Thread(() -> {
            try {
                String line;
                while ((line = network.readMessage()) != null) {
                    final String msg = line;
                    SwingUtilities.invokeLater(() -> handleMessage(msg));
                }
            } catch (Exception e) {
                showError("Disconnected from server.");
            }
        }).start();
    }

    private void handleMessage(String msg) {
        System.out.println("CLIENT ← " + msg);

        if (msg.startsWith(Protocol.BOARD + ":")) {
            boardPanel.updateBoard(msg.substring((Protocol.BOARD + ":").length()));
        }
        else if (msg.equals(Protocol.YOUR_TURN)) {
            statusLabel.setText("Your turn! Click a column.");
            boardPanel.setInteractive(true);
        }
        else if (msg.startsWith(Protocol.STATUS + ":")) {
            String t = msg.substring((Protocol.STATUS + ":").length());
            statusLabel.setText(t);
            boardPanel.setInteractive(false);
            if ("Session ending.".equals(t)) {
                boardPanel.updateBoard(emptyBoardString());
                chatArea.setText("");
                chatField.setEnabled(false);
                cardLayout.show(mainPanel, "home");
            }
        }
        else if (msg.startsWith(Protocol.GAME_START + ":")) {
            statusLabel.setText(msg.substring((Protocol.GAME_START + ":").length()));
            boardPanel.setInteractive(true);
            gameOver = false;
        }
        else if (msg.startsWith(Protocol.GAMEOVER + ":")) {
            String r = msg.substring((Protocol.GAMEOVER + ":").length());
            statusLabel.setText(r);
            boardPanel.setInteractive(false);
            StyledDocument d = chatArea.getStyledDocument();
            Style win  = d.getStyle("WIN");
            if (win == null) { win = d.addStyle("WIN", null); StyleConstants.setForeground(win, Color.GREEN.darker()); }
            Style lose = d.getStyle("LOSE");
            if (lose == null){ lose = d.addStyle("LOSE", null); StyleConstants.setForeground(lose, Color.RED.darker()); }
            try {
                String winner = r.split(" ")[0];
                if (winner.equals(username)) {
                    d.insertString(d.getLength(), "You won, congratulations!\n", win);
                } else {
                    d.insertString(d.getLength(), "You lost, do better next time!\n", lose);
                }
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
        else if (msg.startsWith(Protocol.CHAT + ":")) {
            String text = msg.substring((Protocol.CHAT + ":").length());
            try {
                StyledDocument d = chatArea.getStyledDocument();
                Style s = d.getStyle(StyleContext.DEFAULT_STYLE);
                d.insertString(d.getLength(), text + "\n", s);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
        else if (msg.startsWith(Protocol.END + ":")) {
            String prompt = msg.substring((Protocol.END + ":").length());
            int choice = JOptionPane.showConfirmDialog(frame, prompt,
                "Play again?", JOptionPane.YES_NO_OPTION);
            network.sendMessage(choice==JOptionPane.YES_OPTION?"yes":"no");
        }
        else if (msg.equals(Protocol.QUEUE_JOINED)) {
            statusLabel.setText("Waiting for opponent...");
            boardPanel.setInteractive(false);
            chatField.setEnabled(true);
        }
        else if (msg.startsWith(Protocol.FRIEND_LIST_RESPONSE + ":")) {
            friendsPanel.updateFriendsList(
                parseKeyBoolList(msg.substring((Protocol.FRIEND_LIST_RESPONSE + ":").length()))
            );
            cardLayout.show(mainPanel, "friends");
        }
        else if (msg.equals(Protocol.FRIEND_ADD_SUCCESS)) {
            showInfo("Friend added successfully.");
        }
        else if (msg.startsWith(Protocol.FRIEND_ADD_ERROR + ":")) {
            showError(msg.substring((Protocol.FRIEND_ADD_ERROR + ":").length()));
        }
        else if (msg.startsWith(Protocol.STATS_RESPONSE + ":")) {
            String[] parts = msg.substring((Protocol.STATS_RESPONSE + ":").length()).split(",");
            statsPanel.updateStats(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
            );
            cardLayout.show(mainPanel, "stats");
        }
        else if (msg.startsWith(Protocol.ERROR + ":")) {
            showError(msg.substring((Protocol.ERROR + ":").length()));
        }
    }

    private Map<String,Boolean> parseKeyBoolList(String data) {
        Map<String,Boolean> m = new HashMap<>();
        for (String e : data.split(";")) {
            String[] kv = e.split(",");
            if (kv.length == 2) m.put(kv[0], "on".equals(kv[1]));
        }
        return m;
    }

    private void showError(String m) {
        JOptionPane.showMessageDialog(frame, m, "Error", JOptionPane.WARNING_MESSAGE);
    }
    private void showInfo(String m) {
        JOptionPane.showMessageDialog(frame, m, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        new ConnectFourClient("127.0.0.1", 12345);
    }
}
