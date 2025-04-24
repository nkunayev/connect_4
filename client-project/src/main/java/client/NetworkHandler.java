package client;

import java.io.*;
import java.net.Socket;

/**
 * NetworkHandler: handles socket communication.
 */
public class NetworkHandler {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public NetworkHandler(String serverIP, int port) throws IOException {
        socket = new Socket(serverIP, port);
        // removed socket.setSoTimeout(...)
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public String readMessage() throws IOException {
        return in.readLine();
    }

    public void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}
