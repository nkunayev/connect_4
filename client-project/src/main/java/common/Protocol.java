package common;

/**
 * Protocol: shared command constants for client-server communication.
 */
public final class Protocol {
    private Protocol() {}

    // Authentication
    public static final String LOGIN             = "LOGIN";
    public static final String LOGIN_SUCCESS     = "LOGIN_SUCCESS";
    public static final String REGISTER          = "REGISTER";
    public static final String REGISTER_SUCCESS  = "REGISTER_SUCCESS";
    public static final String REGISTER_ERROR    = "REGISTER_ERROR";
    public static final String ERROR             = "ERROR";

    // Matchmaking
    public static final String JOIN_QUEUE        = "JOIN_QUEUE";
    public static final String QUEUE_JOINED      = "QUEUE_JOINED";

    // Game flow
    public static final String GAME_START        = "GAME_START";
    public static final String YOUR_TURN         = "YOUR_TURN";
    public static final String STATUS            = "STATUS";
    public static final String MOVE              = "MOVE";
    public static final String BOARD             = "BOARD";
    public static final String CHAT              = "CHAT";
    public static final String GAMEOVER          = "GAMEOVER";
    public static final String END               = "END";
    public static final String LEAVE               = "LEAVE"; 
    public static final String PROMPT            = "PROMPT";

    // Friends
    public static final String FRIEND_LIST_REQUEST  = "FRIEND_LIST_REQUEST";
    public static final String FRIEND_LIST_RESPONSE = "FRIEND_LIST_RESPONSE";
    public static final String FRIEND_ADD           = "FRIEND_ADD";
    public static final String FRIEND_ADD_SUCCESS   = "FRIEND_ADD_SUCCESS";
    public static final String FRIEND_ADD_ERROR     = "FRIEND_ADD_ERROR";

    // Stats
    public static final String STATS_REQUEST         = "STATS_REQUEST";
    public static final String STATS_RESPONSE        = "STATS_RESPONSE";
}
