package server;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import common.Protocol;
import server.UserManager.Result;

public class GameSession implements Runnable {
    private static final Logger log = Logger.getLogger(GameSession.class.getName());
    private final ClientHandler p1, p2;
    private GameBoard board;
    private int currentPlayer;
    private boolean running;

    public GameSession(ClientHandler p1, ClientHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
        this.board = new GameBoard();
        this.currentPlayer = 1;
        this.running = true;
    }

    @Override
    public void run() {
        log.info("Starting session: " + p1.getUsername() + " vs " + p2.getUsername());
        sendGameStart();

        outer:
        while (running) {
            boolean gameOver = false;

            // Main game loop
            while (!gameOver && running) {
                ClientHandler current = (currentPlayer == 1 ? p1 : p2);
                ClientHandler other   = (currentPlayer == 1 ? p2 : p1);

                // Tell both sides who's up
                current.sendMessage(Protocol.YOUR_TURN);
                other.sendMessage(Protocol.STATUS + ":Waiting for opponent...");
                log.info("Waiting for move from " + current.getUsername());

                String msg = null;

                // Temporarily timeout reads so we can poll both sockets
                try {
                    current.setReadTimeout(500);  // 500ms granularity
                    while (running) {
                        try {
                            msg = current.readMessage();
                            break;  // got input from current
                        } catch (SocketTimeoutException ste) {
                            // timeout: check for LEAVE on either side
                            if (checkQuit(other, current)) break outer;
                            if (checkQuit(current, other)) break outer;
                            // no quit, loop again
                        }
                    }
                } catch (IOException ioe) {
                    log.log(Level.WARNING, "Error during timed read setup", ioe);
                    running = false;
                    break;
                } finally {
                    try { current.setReadTimeout(0); }
                    catch (IOException ignored) {}
                }

                // if session was aborted by a quit, break out
                if (!running) break;

                if (msg == null) {
                    // clean disconnect
                    handleDisconnect(current, other);
                    gameOver = true;
                    break;
                }

                msg = msg.trim();
                if (Protocol.LEAVE.equalsIgnoreCase(msg)) {
                    // current player quit
                    log.info(current.getUsername() + " left mid-game");
                    other.sendMessage(Protocol.STATUS + ":Session ending.");
                    recordWin(currentPlayer == 1 ? 2 : 1);
                    running  = false;
                    break;
                }

                // --- normal MOVE handling ---
                if (msg.startsWith(Protocol.MOVE + ":")) {
                    int col;
                    try {
                        col = Integer.parseInt(msg.substring((Protocol.MOVE + ":").length()).trim());
                    } catch (NumberFormatException ex) {
                        current.sendMessage(Protocol.ERROR + ":Invalid move format");
                        continue;
                    }
                    int row = board.dropToken(col, currentPlayer);
                    if (row < 0) {
                        current.sendMessage(Protocol.ERROR + ":Column full or invalid");
                        continue;
                    }
                    log.info(current.getUsername() + " placed at col=" + col + ", row=" + row);
                    broadcastBoard();

                    if (board.checkWin(currentPlayer)) {
                        broadcastMessage(Protocol.GAMEOVER + ":Player " + currentPlayer + " wins!");
                        recordWin(currentPlayer);
                        gameOver = true;
                        break;
                    }
                    if (board.isFull()) {
                        broadcastMessage(Protocol.GAMEOVER + ":Draw!");
                        recordDraw();
                        gameOver = true;
                        break;
                    }
                    currentPlayer = (currentPlayer == 1 ? 2 : 1);
                }
                // chat
                else if (msg.startsWith(Protocol.CHAT + ":")) {
                    broadcastMessage(msg);
                }
                else {
                    current.sendMessage(Protocol.ERROR + ":Unknown command");
                }
            }

            if (!running) break;

            // if we arrived here via normal game-over, ask replay
            try {
                if (!askReplay()) break;
            } catch (IOException e) {
                log.log(Level.WARNING, "Error during replay prompt", e);
                break;
            }
            board = new GameBoard();
            currentPlayer = 1;
            sendGameStart();
        }

        // signal both back to lobby
        p1.sendMessage(Protocol.STATUS + ":Session ending.");
        p2.sendMessage(Protocol.STATUS + ":Session ending.");
        p1.signalGameOver();
        p2.signalGameOver();
        log.info("Session ended: " + p1.getUsername() + " vs " + p2.getUsername());
    }

    /** Returns true if `who` just sent LEAVE; handles win-count and messaging. */
    private boolean checkQuit(ClientHandler who, ClientHandler other) throws IOException {
        if (who.hasData()) {
            String line = who.readMessage();
            if (line != null && Protocol.LEAVE.equalsIgnoreCase(line.trim())) {
                log.info(who.getUsername() + " left mid-game");
                other.sendMessage(Protocol.STATUS + ":Session ending.");
                if (who == p1) recordWin(2); else recordWin(1);
                running = false;
                return true;
            }
        }
        return false;
    }

    private void handleDisconnect(ClientHandler from, ClientHandler to) {
        to.sendMessage(Protocol.GAMEOVER + ":Opponent disconnected.");
        recordWin(from == p1 ? 2 : 1);
        running  = false;
    }

    private void sendGameStart() {
        p1.sendMessage(Protocol.GAME_START + ":You are Player 1 (Red)");
        p2.sendMessage(Protocol.GAME_START + ":You are Player 2 (Yellow)");
        broadcastBoard();
    }

    private void broadcastBoard() {
        String msg = Protocol.BOARD + ":" + board.serialize();
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }

    private void broadcastMessage(String msg) {
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }

    private boolean askReplay() throws IOException {
        p1.sendMessage(Protocol.END + ":Play again? (yes/no)");
        String r1 = p1.readMessage();
        if (!"yes".equalsIgnoreCase(r1)) {
            broadcastMessage(Protocol.STATUS + ":Session ending.");
            return false;
        }
        p2.sendMessage(Protocol.END + ":Play again? (yes/no)");
        String r2 = p2.readMessage();
        if (!"yes".equalsIgnoreCase(r2)) {
            broadcastMessage(Protocol.STATUS + ":Session ending.");
            return false;
        }
        return true;
    }

    private void recordWin(int player) {
        if (player == 1) {
            UserManager.recordResult(p1.getUsername(), Result.WIN);
            UserManager.recordResult(p2.getUsername(), Result.LOSS);
        } else {
            UserManager.recordResult(p2.getUsername(), Result.WIN);
            UserManager.recordResult(p1.getUsername(), Result.LOSS);
        }
    }

    private void recordDraw() {
        UserManager.recordResult(p1.getUsername(), Result.DRAW);
        UserManager.recordResult(p2.getUsername(), Result.DRAW);
    }
}
