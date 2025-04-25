package server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import common.Protocol;
import server.UserManager.Result;

/**
 * GameSession: manages a Connect Four match between two clients,
 * handles move logic, win/draw detection, replay, and notifying
 * the handlers to return to the lobby.
 */
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
    }

    @Override
    public void run() {
        log.info("Starting session: " + p1.getUsername() + " vs " + p2.getUsername());
        sendGameStart();

        // Outer loop handles rematches
        while (running) {
            boolean gameOver = false;

            // Play one game
            while (!gameOver && running) {
                ClientHandler current = (currentPlayer == 1) ? p1 : p2;
                ClientHandler other   = (currentPlayer == 1) ? p2 : p1;

                current.sendMessage(Protocol.YOUR_TURN);
                other.sendMessage(Protocol.STATUS + ":Waiting for opponent...");
                log.info("Waiting for move from " + current.getUsername());

                String msg;
                try {
                    msg = current.readMessage();
                    log.info("Received from " + current.getUsername() + ": " + msg);
                } catch (IOException e) {
                    log.log(Level.WARNING, "Error reading move from " + current.getUsername(), e);
                    other.sendMessage(Protocol.GAMEOVER + ":Opponent disconnected.");
                    recordWin(currentPlayer == 1 ? 2 : 1);
                    gameOver = true;
                    running  = false;
                    break;
                }
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
                    log.info(current.getUsername() + " dropped at col=" + col + ", row=" + row);
                    broadcastBoard();

                    if (board.checkWin(currentPlayer)) {
                        log.info("Player " + currentPlayer + " wins");
                        broadcastMessage(Protocol.GAMEOVER + ":" + current.getUsername() + " wins!");
                        recordWin(currentPlayer);
                        gameOver = true;
                        break;
                    }
                    if (board.isFull()) {
                        log.info("Board full: draw");
                        broadcastMessage(Protocol.GAMEOVER + ":Draw!");
                        recordDraw();
                        gameOver = true;
                        break;
                    }

                    currentPlayer = (currentPlayer == 1 ? 2 : 1);
                }
                else if (msg.startsWith(Protocol.CHAT + ":")) {
                    broadcastMessage(msg);
                }
                else {
                    current.sendMessage(Protocol.ERROR + ":Unknown command");
                }
            }

            // Ask for a rematch; if either says no, end the session
            if (!askReplay()) {
                // **NEW**: inform clients that the session is ending
                broadcastMessage(Protocol.STATUS + ":Session ending.");
                running = false;
            } else {
                // reset and start a new game
                board = new GameBoard();
                currentPlayer = 1;
                sendGameStart();
            }
        }

        log.info("Session ended: " + p1.getUsername() + " vs " + p2.getUsername());
        // Wake both handlers so they return to the lobby loop
        p1.signalGameOver();
        p2.signalGameOver();
    }

    /** Notify both clients of a new game start. */
    private void sendGameStart() {
        log.info("Sending GAME_START + initial board");
        p1.sendMessage(Protocol.GAME_START + ":You are Player 1 (Red)");
        p2.sendMessage(Protocol.GAME_START + ":You are Player 2 (Yellow)");
        broadcastBoard();
    }

    /** Send the current board to both clients. */
    private void broadcastBoard() {
        String data = board.serialize();
        String msg  = Protocol.BOARD + ":" + data;
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }

    /** Send an arbitrary message to both clients. */
    private void broadcastMessage(String msg) {
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }

    /** Prompt both players for a rematch; returns true only if both say “yes.” */
    private boolean askReplay() {
        p1.sendMessage(Protocol.END + ":Play again? (yes/no)");
        p2.sendMessage(Protocol.END + ":Play again? (yes/no)");
        try {
            String r1 = p1.readMessage();
            String r2 = p2.readMessage();
            log.info("Replay: " + p1.getUsername() + "=" + r1 + ", " + p2.getUsername() + "=" + r2);
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
