// GameServer.java
// Improved server main logic for pairing, logging, and resilience

package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * GameServer
 * Main entry point for the Connect Four server.
 * Accepts client connections, handles usernames, and creates game sessions.
 */
public class GameServer {
    // Track unique usernames and waiting players
    private static final Set<String> usernames = Collections.synchronizedSet(new HashSet<>());
    private static final List<ClientHandler> waitingClients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        int port = 12345;
        System.out.println("=== Connect Four Server Starting on port " + port + " ===");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client attempting to connect...");
                ClientHandler client = new ClientHandler(socket);
                new Thread(client).start();
            }
        } catch (IOException e) {
            System.err.println("Server encountered a critical error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registers the client's username. Ensures uniqueness.
     */
    public static boolean registerUsername(String username, ClientHandler client) {
        synchronized (usernames) {
            if (usernames.contains(username)) {
                return false;
            } else {
                usernames.add(username);
                return true;
            }
        }
    }

    /**
     * Adds the client to the matchmaking queue.
     */
    public static void addWaitingClient(ClientHandler client) {
        synchronized (waitingClients) {
            waitingClients.add(client);
            System.out.println("[Server] Waiting queue size: " + waitingClients.size());

            if (waitingClients.size() >= 2) {
                ClientHandler p1 = waitingClients.remove(0);
                ClientHandler p2 = waitingClients.remove(0);

                GameSession session = new GameSession(p1, p2);
                new Thread(session).start();

                System.out.println("[Server] Started new game session between "
                        + p1.getUsername() + " and " + p2.getUsername());
            }
        }
    }

    /**
     * Optionally used to remove username if a player quits early.
     */
    public static void unregisterUsername(String username) {
        if (username != null) {
            usernames.remove(username);
        }
    }
}