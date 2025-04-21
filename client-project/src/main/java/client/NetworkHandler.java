package client;

import java.io.*;
import java.net.Socket;

/**
 * NetworkHandler.java
 *
 * Handles socket communication with the server.
 * Provides methods to send and receive messages.
 */
public class NetworkHandler {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    public NetworkHandler(String serverIP, int port) throws IOException {
        socket = new Socket(serverIP, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
    }
    
    /**
     * Sends a message to the server.
     */
    public void sendMessage(String msg) {
        out.println(msg);
    }
    
    /**
     * Blocking call to read a message from the server.
     */
    public String readMessage() throws IOException {
        return in.readLine();
    }
    
    /**
     * Closes the network connection.
     */
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            // Ignore errors during close.
        }
    }
}
