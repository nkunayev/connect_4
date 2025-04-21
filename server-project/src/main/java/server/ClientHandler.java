package server;

import java.io.*;
import java.net.Socket;

/**
 * ClientHandler.java
 *
 * Handles communication with an individual client.
 * Processes login commands and relays messages between the client and server.
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a text message to the connected client.
     */
    public void sendMessage(String msg) {
        out.println(msg);
    }

    /**
     * Reads a text message from the client (blocking call).
     */
    public String readMessage() throws IOException {
        return in.readLine();
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Closes the client connection.
     */
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            // Error during socket close; ignore.
        }
    }
    
    @Override
    public void run() {
        try {
            // Expect a login command of the form "LOGIN:username" from the client.
            String loginMsg = in.readLine();
            if (loginMsg != null && loginMsg.startsWith("LOGIN:")) {
                String name = loginMsg.substring(6).trim();
                // Attempt to register the username with the server.
                if (GameServer.registerUsername(name, this)) {
                    setUsername(name);
                    sendMessage("LOGIN_SUCCESS");
                    System.out.println("User logged in: " + name);
                    // Add the client to the waiting room for a game.
                    GameServer.addWaitingClient(this);
                } else {
                    sendMessage("ERROR:Username already taken.");
                    close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
