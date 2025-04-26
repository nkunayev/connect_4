package server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import common.Protocol;

public class ClientHandler implements Runnable {
    private static final Logger log = Logger.getLogger(ClientHandler.class.getName());

    private final Socket      socket;
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
            // ==== LOGIN PHASE ====
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith(Protocol.REGISTER + ":")) {
                    // … registration logic …
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
            if (line == null) return;  // disconnected pre-login

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
                    synchronized (gameLock) {
                        gameLock.wait();
                    }
                    inGame = false;
                    break;
                  default:
                    sendMessage(Protocol.ERROR + ":Unknown command");
                }
            }
        } catch (IOException e) {
            log.log(Level.INFO, "Connection lost for " + username, e);
        } catch (InterruptedException e) {
            log.log(Level.WARNING, "Interrupted while waiting for game to finish", e);
        } finally {
            cleanup();
        }
    }

    /** Wake this handler up after a GameSession ends. */
    public void signalGameOver() {
        synchronized (gameLock) {
            gameLock.notify();
        }
    }

    /** Non-blocking check for any pending data on the socket. */
    public boolean hasData() {
        try {
            return in.ready();
        } catch (IOException e) {
            return false;
        }
    }

    /** Set a SO_TIMEOUT on the socket to allow timed reads. */
    public void setReadTimeout(int millis) throws SocketException {
        socket.setSoTimeout(millis);
    }

    /** Send one line to the client. */
    public void sendMessage(String msg) {
        out.println(msg);
    }

    /** Read one line (blocking or timeout) from the client. */
    public String readMessage() throws IOException {
        return in.readLine();
    }

    public String getUsername() {
        return username;
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