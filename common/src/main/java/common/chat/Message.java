package common.chat;

import java.io.Serializable;

/**
 * A serializable chat message, sent between ChatClient and ChatServer.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 42L;

    public MessageType type;
    public String      message;
    public int         recipient; // -1 for broadcast

    /** Notification of join/leave **/
    public Message(int id, boolean connect) {
        if (connect) {
            type      = MessageType.NEWUSER;
            message   = "User " + id + " has joined chat.";
        } else {
            type      = MessageType.DISCONNECT;
            message   = "User " + id + " has left chat.";
        }
        recipient = id;
    }

    /** Broadcast text **/
    public Message(String text) {
        type      = MessageType.TEXT;
        message   = text;
        recipient = -1;
    }

    /** Direct text **/
    public Message(int rec, String text) {
        type      = MessageType.TEXT;
        message   = text;
        recipient = rec;
    }
}
