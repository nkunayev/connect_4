package server;

import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import common.Protocol;
import server.UserManager.Result;

public class GameSession implements Runnable {
    private static final Logger log = Logger.getLogger(GameSession.class.getName());

    private final ClientHandler p1;
    private final ClientHandler p2;
    private GameBoard board;
    private int currentPlayer;
    private boolean running;

    public GameSession(ClientHandler p1, ClientHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
        this.board = new GameBoard();
        this.currentPlayer = 1;
        this.running = true;

        // set a small read timeout so we can interleave CHAT from both sides
        try {
            p1.setReadTimeout(200);
            p2.setReadTimeout(200);
        } catch (SocketException e) {
            log.log(Level.WARNING, "Could not set SO_TIMEOUT", e);
        }
    }

    @Override
    public void run() {
        log.info("Starting session: " + p1.getUsername() + " vs " + p2.getUsername());
        sendGameStart();

        while (running) {
            boolean gameOver = false;

            while (!gameOver && running) {
                // first, pull off any chat from either side
                flushPendingChat();

                ClientHandler current = (currentPlayer == 1) ? p1 : p2;
                ClientHandler other   = (currentPlayer == 1) ? p2 : p1;

                current.sendMessage(Protocol.YOUR_TURN);
                other.sendMessage(Protocol.STATUS + ":Waiting for opponent...");
                log.info("Waiting for move from " + current.getUsername());

                String msg = null;

                // loop until we get a MOVE or CHAT from the "current" player,
                // but keep flushing chat from both sides on timeout
                while (running) {
                    try {
                        msg = current.readMessage();  // may timeout
                        break;
                    } catch (IOException e) {
                        // if it's a timeout, it will be wrapped in an IOException whose
                        // message contains "Read timed out". We'll detect that:
                        if (e.getMessage() != null && e.getMessage().contains("timed out")) {
                            flushPendingChat();
                            continue;
                        } else {
                            log.log(Level.WARNING,
                                "Error reading from " + current.getUsername(), e);
                            other.sendMessage(Protocol.GAMEOVER + ":Opponent disconnected.");
                            recordWin(currentPlayer == 1 ? 2 : 1);
                            gameOver = true;
                            running  = false;
                            break;
                        }
                    }
                }

                if (!running) break;
                if (msg == null) {
                    log.info("Disconnect: " + current.getUsername());
                    other.sendMessage(Protocol.GAMEOVER + ":Opponent disconnected.");
                    recordWin(currentPlayer == 1 ? 2 : 1);
                    gameOver = true;
                    running  = false;
                    break;
                }

                if (msg.startsWith(Protocol.MOVE + ":")) {
                    int col = Integer.parseInt(msg.substring((Protocol.MOVE + ":").length()).trim());
                    int row = board.dropToken(col, currentPlayer);
                    if (row < 0) {
                        current.sendMessage(Protocol.ERROR + ":Invalid move");
                        continue;
                    }
                    log.info(current.getUsername() + " placed at col=" + col + ", row=" + row);
                    broadcastBoard();

                    if (board.checkWin(currentPlayer)) {
                        log.info("Player " + currentPlayer + " wins");
                        broadcastMessage(Protocol.GAMEOVER + ":" + current.getUsername() + " wins!");
                        recordWin(currentPlayer);
                        gameOver = true;
                        break;
                    }
                    if (board.isFull()) {
                        log.info("Draw detected");
                        broadcastMessage(Protocol.GAMEOVER + ":Draw!");
                        recordDraw();
                        gameOver = true;
                        break;
                    }

                    currentPlayer = (currentPlayer == 1 ? 2 : 1);
                }
                else if (msg.startsWith(Protocol.CHAT + ":")) {
                    // immediate broadcast of CHAT from current
                    broadcastMessage(msg);
                }
                else {
                    current.sendMessage(Protocol.ERROR + ":Unknown command");
                }
            }

            // ask replay
            // — disable chat‐interleave timeout so replay blocks until answer —
        try {
            p1.setReadTimeout(0);
            p2.setReadTimeout(0);
        } catch (SocketException e) {
            log.log(Level.WARNING, "Couldn't disable timeout for replay", e);
        }

        // ask replay (this will now block until each client clicks yes/no)
        boolean bothYes = askReplay();

        if (!bothYes) {
            broadcastMessage(Protocol.STATUS + ":Session ending.");
            running = false;
        } else {
            board = new GameBoard();
            currentPlayer = 1;
            sendGameStart();
        }

        // — restore the 200 ms timeout for the next game’s chat‐interleaving —
        try {
            p1.setReadTimeout(200);
            p2.setReadTimeout(200);
        } catch (SocketException e) {
            log.log(Level.WARNING, "Couldn't restore timeout after replay", e);
        }
        }

        log.info("Session ended: " + p1.getUsername() + " vs " + p2.getUsername());
        p1.signalGameOver();
        p2.signalGameOver();
    }

    private void sendGameStart() {
        p1.sendMessage(Protocol.GAME_START + ":You are Player 1 (Red)");
        p2.sendMessage(Protocol.GAME_START + ":You are Player 2 (Yellow)");
        broadcastBoard();
    }

    private void broadcastBoard() {
        String data = board.serialize();
        String msg  = Protocol.BOARD + ":" + data;
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }

    private void broadcastMessage(String msg) {
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }

    /** Read & broadcast any pending CHAT: messages on either socket. */
    private void flushPendingChat() {
        try {
            while (p1.hasData()) {
                String m = p1.readMessage();
                if (m != null && m.startsWith(Protocol.CHAT + ":")) {
                    broadcastMessage(m);
                }
            }
            while (p2.hasData()) {
                String m = p2.readMessage();
                if (m != null && m.startsWith(Protocol.CHAT + ":")) {
                    broadcastMessage(m);
                }
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Error flushing chat", e);
        }
    }

    private boolean askReplay() {
        p1.sendMessage(Protocol.END + ":Play again? (yes/no)");
        p2.sendMessage(Protocol.END + ":Play again? (yes/no)");
        try {
            String r1 = p1.readMessage();
            String r2 = p2.readMessage();
            log.info("Replay responses: " + r1 + ", " + r2);
            return "yes".equalsIgnoreCase(r1) && "yes".equalsIgnoreCase(r2);
        } catch (IOException e) {
            log.log(Level.WARNING, "Error during replay dialog", e);
            return false;
        }
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