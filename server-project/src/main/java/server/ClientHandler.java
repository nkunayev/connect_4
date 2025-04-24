package server;

import java.io.*;
import java.net.Socket;
import java.util.logging.*;

import common.Protocol;

/**
 * ClientHandler: handles a single player's socket through login, lobby,
 * and hands off to GameSession on JOIN_QUEUE.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Logger log = Logger.getLogger(ClientHandler.class.getName());
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private volatile boolean running = true;

    public ClientHandler(Socket sock) {
        this.socket = sock;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            throw new RuntimeException("Error initializing ClientHandler streams", e);
        }
    }

    @Override
    public void run() {
        // ===== LOGIN PHASE =====
        try {
            String line;
            while ((line = in.readLine()) != null) {
                log.fine("[login] " + line);
                if (line.startsWith(Protocol.REGISTER + ":")) {
                    String[] parts = line.substring((Protocol.REGISTER + ":").length()).split(":");
                    boolean ok = parts.length == 2 && UserManager.register(parts[0], parts[1]);
                    sendMessage(ok ? Protocol.REGISTER_SUCCESS : Protocol.REGISTER_ERROR + ":Username exists");
                } else if (line.startsWith(Protocol.LOGIN + ":")) {
                    String[] parts = line.substring((Protocol.LOGIN + ":").length()).split(":");
                    if (parts.length == 2 && GameServer.userLogin(parts[0], parts[1], this)) {
                        username = parts[0];
                        sendMessage(Protocol.LOGIN_SUCCESS);
                        break;
                    } else {
                        sendMessage(Protocol.ERROR + ":Login failed");
                    }
                } else {
                    sendMessage(Protocol.ERROR + ":Please register or login first");
                }
            }
        } catch (IOException e) {
            log.log(Level.INFO, "Connection lost during login for " + username, e);
            close();
            GameServer.userLogout(username);
            return;
        }

        // ===== LOBBY PHASE =====
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                log.fine("[" + username + "] " + line);
                switch (line) {
                    case Protocol.FRIEND_LIST_REQUEST:
                        GameServer.requestFriends(this);
                        break;
                    case Protocol.STATS_REQUEST:
                        GameServer.requestStats(this);
                        break;
                    case Protocol.JOIN_QUEUE:
                        GameServer.addWaitingClient(this);
                        // hand control to GameSession; do not clean up here
                        return;
                    default:
                        if (line.startsWith(Protocol.FRIEND_ADD + ":")) {
                            String friend = line.substring((Protocol.FRIEND_ADD + ":").length());
                            boolean ok = GameServer.addFriend(username, friend);
                            sendMessage(ok ? Protocol.FRIEND_ADD_SUCCESS : Protocol.FRIEND_ADD_ERROR + ":Cannot add friend");
                        } else {
                            log.warning("Unknown command from " + username + ": " + line);
                        }
                }
            }
        } catch (IOException e) {
            log.log(Level.INFO, "Connection lost in lobby for " + username, e);
        }

        // Cleanup if exiting lobby without joining a game
        GameServer.removeWaitingClient(this);
        GameServer.userLogout(username);
        close();
    }

    /**
     * Read a single message line from the client socket.
     */
    public String readMessage() throws IOException {
        return in.readLine();
    }

    /**
     * Send a single-line command back to the client.
     */
    public void sendMessage(String msg) {
        out.println(msg);
        log.fine("To " + username + ": " + msg);
    }

    /**
     * Accessor for this client's username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Close socket and mark handler no longer running.
     */
    public void close() {
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {}
        log.info("Closed connection for " + username);
    }
}
