package server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import common.Protocol;

public class ClientHandler implements Runnable {
    private static final Logger log = Logger.getLogger(ClientHandler.class.getName());

    private final Socket        socket;
    private final BufferedReader in;
    private final PrintWriter    out;
    private String               username;

    // Used to block while in a game and resume back in the lobby
    private volatile boolean inGame = false;
    private final Object     gameLock = new Object();

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out    = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            // ===== LOGIN PHASE =====
            String line;
            while ((line = in.readLine()) != null) {
                log.fine("[login] " + line);
                if (line.startsWith(Protocol.REGISTER + ":")) {
                    String[] parts = line.substring((Protocol.REGISTER + ":").length()).split(":");
                    boolean ok = parts.length == 2 && UserManager.register(parts[0], parts[1]);
                    sendMessage(ok
                        ? Protocol.REGISTER_SUCCESS
                        : Protocol.REGISTER_ERROR + ":Username exists"
                    );
                }
                else if (line.startsWith(Protocol.LOGIN + ":")) {
                    String[] parts = line.substring((Protocol.LOGIN + ":").length()).split(":");
                    if (parts.length == 2 && GameServer.userLogin(parts[0], parts[1], this)) {
                        username = parts[0];
                        sendMessage(Protocol.LOGIN_SUCCESS);
                        break;
                    } else {
                        sendMessage(Protocol.ERROR + ":Login failed");
                    }
                }
                else {
                    sendMessage(Protocol.ERROR + ":Please register or login first");
                }
            }
            if (line == null) {
                // client disconnected before logging in
                cleanup();
                return;
            }

            // ===== LOBBY PHASE =====
            while ((line = in.readLine()) != null) {
                log.fine("[" + username + "] " + line);

                if (Protocol.FRIEND_LIST_REQUEST.equals(line)) {
                    GameServer.requestFriends(this);
                }
                else if (Protocol.STATS_REQUEST.equals(line)) {
                    GameServer.requestStats(this);
                }
                else if (Protocol.JOIN_QUEUE.equals(line)) {
                    inGame = true;
                    GameServer.addWaitingClient(this);
                    // wait until GameSession signals us back
                    synchronized (gameLock) {
                        gameLock.wait();
                    }
                    inGame = false;
                }
                else if (line.startsWith(Protocol.FRIEND_ADD + ":")) {
                    String friend = line.substring((Protocol.FRIEND_ADD + ":").length());
                    boolean ok = GameServer.addFriend(username, friend);
                    sendMessage(ok
                        ? Protocol.FRIEND_ADD_SUCCESS
                        : Protocol.FRIEND_ADD_ERROR + ":Cannot add friend"
                    );
                }
                else {
                    log.warning("Unknown command from " + username + ": " + line);
                    sendMessage(Protocol.ERROR + ":Unknown command");
                }
            }
        } catch (IOException e) {
            log.log(Level.INFO, "Connection lost for " + username, e);
        } catch (InterruptedException e) {
            log.log(Level.WARNING, "Interrupted waiting for game to finish", e);
        } finally {
            cleanup();
        }
    }

    /**
     * Called by GameSession when it’s time to return to the lobby.
     */
    public void signalGameOver() {
        synchronized (gameLock) {
            gameLock.notify();
        }
    }

    /**
     * Allows the server (e.g. GameSession) to set a SO_TIMEOUT
     * on this client's socket.
     */
    public void setReadTimeout(int millis) throws SocketException {
        socket.setSoTimeout(millis);
    }

    /**
     * Non-blocking check: returns true if there's data ready to be read
     * from the client socket.
     */
    public boolean hasData() {
        try {
            return in.ready();
        } catch (IOException e) {
            return false;
        }
    }

    /** Send one line back to the client. */
    public void sendMessage(String msg) {
        out.println(msg);
        log.fine("To " + username + ": " + msg);
    }

    /** Read one line from the client (blocking). */
    public String readMessage() throws IOException {
        return in.readLine();
    }

    /** Return this client's username. */
    public String getUsername() {
        return username;
    }

    /** Clean up on disconnect or exit. */
    private void cleanup() {
        GameServer.removeWaitingClient(this);
        if (username != null) {
            GameServer.userLogout(username);
        }
        try {
            socket.close();
        } catch (IOException ignored) {}
        log.info("Closed connection for " + username);
    }

    /** Close immediately, without waiting for cleanup. */
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}
