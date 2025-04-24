package server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * UserManager: Handles user credentials, stats, and friendships.
 * Persists data to disk in JSON files under userdata/.
 */
public class UserManager {
    private static final File DATA_DIR     = new File("userdata");
    private static final File PWD_FILE     = new File(DATA_DIR, "passwords.json");
    private static final File STATS_FILE   = new File(DATA_DIR, "stats.json");
    private static final File FRIENDS_FILE = new File(DATA_DIR, "friends.json");

    private static final ObjectMapper mapper = new ObjectMapper();
    private static Map<String, String>         passwords;
    private static Map<String, Stats>          statsMap;
    private static Map<String, Set<String>>    friendsMap;

    static {
        // ensure data directory exists
        if (!DATA_DIR.exists()) {
            DATA_DIR.mkdirs();
        }

        // load or initialize each map
        passwords   = loadJson(PWD_FILE,    new TypeReference<Map<String,String>>() {}, new HashMap<>());
        statsMap    = loadJson(STATS_FILE,  new TypeReference<Map<String,Stats>>()   {}, new HashMap<>());
        friendsMap  = loadJson(FRIENDS_FILE,new TypeReference<Map<String,Set<String>>>() {}, new HashMap<>());

        // ensure every existing user has entries
        for (String user : passwords.keySet()) {
            statsMap  .putIfAbsent(user, new Stats());
            friendsMap.putIfAbsent(user, new HashSet<>());
        }

        // Write out blank maps immediately so userdata/*.json always exists
        saveAll();
    }

    private static <T> T loadJson(File file, TypeReference<T> type, T fallback) {
        if (file.exists()) {
            try {
                return mapper.readValue(file, type);
            } catch (IOException e) {
                System.err.println("Failed to load " + file.getName() + ": " + e.getMessage());
            }
        }
        return fallback;
    }

    private static void saveAll() {
        saveJson(PWD_FILE,     passwords);
        saveJson(STATS_FILE,   statsMap);
        saveJson(FRIENDS_FILE, friendsMap);
    }

    private static void saveJson(File file, Object data) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
        } catch (IOException e) {
            System.err.println("Failed to save " + file.getName() + ": " + e.getMessage());
        }
    }

    /** Register a new user with password. Returns false if username already taken. */
    public synchronized static boolean register(String user, String password) {
        if (passwords.containsKey(user)) return false;
        passwords.put(user, hash(password));
        statsMap.put(user, new Stats());
        friendsMap.put(user, new HashSet<>());
        saveAll();
        return true;
    }

    /** Authenticate an existing user. */
    public synchronized static boolean authenticate(String user, String password) {
        return passwords.containsKey(user) && passwords.get(user).equals(hash(password));
    }

    /** Retrieve stats for a user (wins/losses/draws). */
    public synchronized static Stats getStats(String user) {
        return statsMap.getOrDefault(user, new Stats());
    }

    /** Retrieve the friend list for a user. */
    public synchronized static Set<String> getFriends(String user) {
        return Collections.unmodifiableSet(
            friendsMap.getOrDefault(user, Collections.emptySet())
        );
    }

    /** Add a friend. Returns false if friend does not exist or is self. */
    public synchronized static boolean addFriend(String user, String friend) {
        if (!passwords.containsKey(friend) || user.equals(friend)) return false;
        boolean added = friendsMap.computeIfAbsent(user, k -> new HashSet<>()).add(friend);
        if (added) saveAll();
        return added;
    }

    /** Record a game result (WIN, LOSS, DRAW) for a user. */
    public synchronized static void recordResult(String user, Result r) {
        Stats s = statsMap.get(user);
        if (s == null) return;
        switch (r) {
            case WIN:   s.wins++;  break;
            case LOSS:  s.losses++;break;
            case DRAW:  s.draws++; break;
        }
        saveAll();
    }

    private static String hash(String pwd) {
        return Integer.toHexString(pwd.hashCode());
    }

    /** Container for win/loss/draw counts. */
    public static class Stats {
        public int wins   = 0;
        public int losses = 0;
        public int draws  = 0;
    }

    /** Enumeration of possible game outcomes. */
    public enum Result {
        WIN, LOSS, DRAW
    }
}
