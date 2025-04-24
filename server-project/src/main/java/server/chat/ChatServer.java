package server.chat;

import common.chat.Message;
import common.chat.MessageType;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * ChatServer: accepts sockets on a port, reads Message objects,
 * and broadcasts them to all connected clients.
 */
public class ChatServer {
    private final int port;
    private final List<ClientThread> clients = Collections.synchronizedList(new ArrayList<>());

    public ChatServer(int port) {
        this.port = port;
    }

    /** Starts the server in a daemon thread */
    public void start() {
        Thread t = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                while (true) {
                    Socket sock = ss.accept();
                    ClientThread ct = new ClientThread(sock);
                    clients.add(ct);
                    ct.start();
                    // notify others of new user
                    broadcast(new Message(ct.id, true));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "ChatServer-Port-" + port);
        t.setDaemon(true);
        t.start();
    }

    /** Broadcasts msg to every client; removes broken connections */
    private void broadcast(Message msg) {
        synchronized (clients) {
            Iterator<ClientThread> it = clients.iterator();
            while (it.hasNext()) {
                ClientThread ct = it.next();
                try {
                    ct.out.writeObject(msg);
                    ct.out.flush();
                } catch (IOException e) {
                    it.remove();
                }
            }
        }
    }

    private class ClientThread extends Thread {
        final int id;
        final Socket sock;
        ObjectOutputStream out;
        ObjectInputStream  in;

        ClientThread(Socket sock) throws IOException {
            this.sock = sock;
            this.id   = sock.getPort(); // unique per connection
            this.out  = new ObjectOutputStream(sock.getOutputStream());
            this.in   = new ObjectInputStream(sock.getInputStream());
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Message msg = (Message) in.readObject();
                    if (msg == null || msg.type == MessageType.DISCONNECT) break;
                    broadcast(msg);
                }
            } catch (Exception ignored) {
            } finally {
                clients.remove(this);
                broadcast(new Message(id, false));
                try { sock.close(); } catch (IOException ignored) {}
            }
        }
    }
}
