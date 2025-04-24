package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.*;
import javax.swing.SwingUtilities;

import common.Protocol;
import server.chat.ChatServer;

/**
 * GameServer: accepts game connections, matchmaking,
 * starts ChatServer on port 5555, and logs to console & GUI.
 */
public class GameServer {
    private static final int PORT      = 12345;
    private static final int CHAT_PORT = 5555;
    private static final Logger log    = Logger.getLogger(GameServer.class.getName());

    // Track online users and matchmaking queue
    private static final Map<String, ClientHandler> onlineUsers     = Collections.synchronizedMap(new HashMap<>());
    private static final List<ClientHandler>        waitingClients  = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        // 1) Start chat server
        new ChatServer(CHAT_PORT).start();
        log.info("ChatServer started on port " + CHAT_PORT);

        // 2) Setup GUI logging window
        ServerGUI gui = new ServerGUI();
        SwingUtilities.invokeLater(() -> gui.setVisible(true));

        // 3) Configure root logger for console + text area
        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
        for (Handler h : root.getHandlers()) root.removeHandler(h);

        ConsoleHandler console = new ConsoleHandler();
        console.setFormatter(new SimpleFormatter());
        console.setLevel(Level.INFO);
        root.addHandler(console);

        TextAreaHandler taHandler = new TextAreaHandler(gui);
        taHandler.setLevel(Level.INFO);
        root.addHandler(taHandler);

        log.info("=== GameServer starting on port " + PORT + " ===");

        // 4) Accept loop for game clients
        try (ServerSocket ss = new ServerSocket(PORT)) {
            log.info("GameServer listening on port " + PORT);
            while (true) {
                Socket sock = ss.accept();
                log.info("New game connection from " + sock.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(sock);
                new Thread(handler, "ClientHandler-" + sock.getPort()).start();
            }
        }
    }

    public static boolean userLogin(String user, String pass, ClientHandler ch) {
        if (!UserManager.authenticate(user, pass)) {
            log.info("Authentication failed for user: " + user);
            return false;
        }
        if (onlineUsers.containsKey(user)) {
            log.info("Duplicate login attempt for user: " + user);
            return false;
        }
        onlineUsers.put(user, ch);
        log.info("User logged in: " + user);
        return true;
    }

    public static void userLogout(String user) {
        if (user != null && onlineUsers.remove(user) != null) {
            log.info("User logged out: " + user);
        }
    }

    public static void addWaitingClient(ClientHandler ch) {
        synchronized (waitingClients) {
            waitingClients.add(ch);
            log.info("Added to queue: " + ch.getUsername() + " (size=" + waitingClients.size() + ")");
            ch.sendMessage(Protocol.QUEUE_JOINED);
            if (waitingClients.size() >= 2) {
                ClientHandler p1 = waitingClients.remove(0);
                ClientHandler p2 = waitingClients.remove(0);
                log.info("Matchmaking pair: " + p1.getUsername() + " vs " + p2.getUsername());
                new Thread(new GameSession(p1, p2),
                           "GameSession-" + p1.getUsername() + "-" + p2.getUsername())
                    .start();
            }
        }
    }

    public static void removeWaitingClient(ClientHandler ch) {
        synchronized (waitingClients) {
            if (waitingClients.remove(ch)) {
                log.info("Removed from queue: " + ch.getUsername());
            }
        }
    }

    public static void requestFriends(ClientHandler ch) {
        Set<String> friends = UserManager.getFriends(ch.getUsername());
        StringBuilder sb = new StringBuilder();
        for (String f : friends) {
            boolean on = onlineUsers.containsKey(f);
            sb.append(f).append(',').append(on ? "on" : "off").append(';');
        }
        log.info("Friends list for " + ch.getUsername() + ": " + sb);
        ch.sendMessage(Protocol.FRIEND_LIST_RESPONSE + ":" + sb.toString());
    }

    public static boolean addFriend(String user, String friend) {
        boolean ok = UserManager.addFriend(user, friend);
        log.info("AddFriend from " + user + " to " + friend + ": " + (ok ? "success" : "failure"));
        return ok;
    }

    public static void requestStats(ClientHandler ch) {
        UserManager.Stats s = UserManager.getStats(ch.getUsername());
        String payload = s.wins + "," + s.losses + "," + s.draws;
        log.info("Stats for " + ch.getUsername() + ": " + payload);
        ch.sendMessage(Protocol.STATS_RESPONSE + ":" + payload);
    }
}
