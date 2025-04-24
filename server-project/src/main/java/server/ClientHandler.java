package server;

import java.io.*;
import java.net.Socket;
import java.util.logging.*;
import common.Protocol;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Logger log = Logger.getLogger("ClientHandler");
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private volatile boolean running = true;

    public ClientHandler(Socket sock) {
        this.socket = sock;
        try {
            in  = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            out = new PrintWriter(sock.getOutputStream(), true);
        } catch (IOException e) {
            log.log(Level.SEVERE, "I/O error in handler constructor", e);
        }
    }

    public String getUsername() { return username; }
    public void sendMessage(String msg) {
        out.println(msg);
        log.fine("To " + username + ": " + msg);
    }

    @Override
    public void run() {
        try {
            String line;
            // Registration & Login loop
            while ((line = in.readLine()) != null) {
                log.fine("From socket: " + line);
                if (line.startsWith(Protocol.REGISTER + ":")) {
                    String[] parts = line.substring((Protocol.REGISTER + ":").length()).split(":");
                    boolean ok = parts.length == 2 && UserManager.register(parts[0], parts[1]);
                    sendMessage(ok
                        ? Protocol.REGISTER_SUCCESS
                        : Protocol.REGISTER_ERROR + ":Username already exists");
                }
                else if (line.startsWith(Protocol.LOGIN + ":")) {
                    String[] parts = line.substring((Protocol.LOGIN + ":").length()).split(":");
                    if (parts.length == 2 && GameServer.userLogin(parts[0], parts[1], this)) {
                        username = parts[0];
                        sendMessage(Protocol.LOGIN_SUCCESS);
                        break;
                    } else {
                        sendMessage(Protocol.ERROR + ":Login failed");
                        close();
                        return;
                    }
                } else {
                    sendMessage(Protocol.ERROR + ":Please register or login first");
                }
            }

            // Main command loop
            while (running && (line = in.readLine()) != null) {
                log.fine("[" + username + "] " + line);
                if (line.equals(Protocol.FRIEND_LIST_REQUEST)) {
                    GameServer.requestFriends(this);
                }
                else if (line.startsWith(Protocol.FRIEND_ADD + ":")) {
                    String f = line.substring((Protocol.FRIEND_ADD + ":").length());
                    boolean ok = GameServer.addFriend(username, f);
                    sendMessage(ok
                        ? Protocol.FRIEND_ADD_SUCCESS
                        : Protocol.FRIEND_ADD_ERROR + ":Cannot add friend");
                }
                else if (line.equals(Protocol.STATS_REQUEST)) {
                    GameServer.requestStats(this);
                }
                else if (line.equals(Protocol.JOIN_QUEUE)) {
                    GameServer.addWaitingClient(this);
                }
                else {
                    log.warning("Unknown command from " + username + ": " + line);
                }
            }
        } catch (IOException e) {
            log.log(Level.INFO, "Connection lost with " + username, e);
        } finally {
            GameServer.removeWaitingClient(this);
            GameServer.userLogout(username);
            close();
        }
    }

    public String readMessage() throws IOException {
        return in.readLine();
    }

    public void close() {
        running = false;
        try { socket.close(); } catch (IOException ignored) {}
        log.info("Closed connection for " + username);
    }
}
