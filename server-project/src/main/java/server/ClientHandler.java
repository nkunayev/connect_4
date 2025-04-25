// server-project/src/main/java/server/ClientHandler.java
package server;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import common.Protocol;

public class ClientHandler implements Runnable {
    private static final Logger log = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter    out;
    private String username;

    // Tracks whether currently in a GameSession
    private volatile boolean inGame = false;
    private final Object gameLock = new Object();

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            throw new RuntimeException("Error initializing handler", e);
        }
    }

    @Override
    public void run() {
        try {
            // ==== LOGIN PHASE ====
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith(Protocol.REGISTER + ":")) {
                    // handle registration (omitted)
                } else if (line.startsWith(Protocol.LOGIN + ":")) {
                    String[] parts = line.substring((Protocol.LOGIN + ":").length()).split(":");
                    if (parts.length == 2 && GameServer.userLogin(parts[0], parts[1], this)) {
                        username = parts[0];
                        sendMessage(Protocol.LOGIN_SUCCESS);
                        break;  // out of login
                    } else {
                        sendMessage(Protocol.ERROR + ":Login failed");
                    }
                } else {
                    sendMessage(Protocol.ERROR + ":Please register or login first");
                }
            }
            if (line == null) return;  // disconnected during login

            // ==== LOBBY PHASE ====
            lobby:
            while ((line = in.readLine()) != null) {
                switch (line) {
                  case Protocol.FRIEND_LIST_REQUEST:
                    GameServer.requestFriends(this);
                    break;
                  case Protocol.STATS_REQUEST:
                    GameServer.requestStats(this);
                    break;
                  case Protocol.JOIN_QUEUE:
                    inGame = true;
                    GameServer.addWaitingClient(this);
                    // block here until GameSession calls signalGameOver()
                    synchronized (gameLock) {
                        gameLock.wait();
                    }
                    inGame = false;
                    // back to lobby
                    break;
                  default:
                    sendMessage(Protocol.ERROR + ":Unknown command");
                }
            }
        } catch (IOException e) {
            log.log(Level.INFO, "Connection lost for " + username, e);
        } catch (InterruptedException e) {
            log.log(Level.WARNING, "Interrupted while in gameLock.wait()", e);
        } finally {
            cleanup();
        }
    }

    /** Wakes the handler up after a game (and any replay) finishes. */
    public void signalGameOver() {
        synchronized (gameLock) {
            gameLock.notify();
        }
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public String readMessage() throws IOException {
        return in.readLine();
    }

    private void cleanup() {
        GameServer.removeWaitingClient(this);
        if (username != null) GameServer.userLogout(username);
        try {
            socket.close();
        } catch (IOException e) {
            log.log(Level.WARNING, "Error closing socket for " + username, e);
        }
        log.info("ClientHandler terminated for " + username);
    }
}
