package client.chat;

import common.chat.Message;
import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * ChatClient: connects to ChatServer, reads Message objects,
 * and delivers them to the provided callback.
 */
public class ChatClient extends Thread {
    private final String host;
    private final int    port;
    private Socket       socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private final Consumer<Message> callback;

    public ChatClient(String host, int port, Consumer<Message> callback) {
        this.host     = host;
        this.port     = port;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(host, port);
            socket.setTcpNoDelay(true);
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());
            while (true) {
                Message msg = (Message) in.readObject();
                if (msg == null) break;
                callback.accept(msg);
            }
        } catch (Exception ignored) {
        }
    }

    /** Sends a Message to the chat server */
    public void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
