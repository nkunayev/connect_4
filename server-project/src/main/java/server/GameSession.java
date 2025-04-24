package server;

import java.io.IOException;
import java.util.logging.*;
import common.Protocol;
import server.UserManager.Result;

/**
 * GameSession: manages a Connect Four match between two clients,
 * handles move logic, win/draw detection, replay, and logging.
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

        while (running) {
            boolean gameOver = false;
            int winner = 0;
            boolean draw = false;

            // Game loop
            while (!gameOver) {
                ClientHandler current = (currentPlayer == 1) ? p1 : p2;
                ClientHandler other   = (currentPlayer == 1) ? p2 : p1;

                current.sendMessage(Protocol.YOUR_TURN);
                other.sendMessage(Protocol.STATUS + ":Waiting for opponent...");

                String msg;
                try {
                    msg = current.readMessage();
                } catch (IOException e) {
                    log.warning("Error reading move from " + current.getUsername());
                    break;
                }
                if (msg == null) {
                    log.info("Disconnect: " + current.getUsername());
                    other.sendMessage(Protocol.GAMEOVER + ":Opponent disconnected.");
                    winner = (currentPlayer == 1 ? 2 : 1);
                    recordWin(winner);
                    gameOver = true;
                    break;
                }

                if (msg.startsWith(Protocol.MOVE + ":")) {
                    int col;
                    try {
                        col = Integer.parseInt(msg.substring((Protocol.MOVE + ":").length()).trim());
                    } catch (NumberFormatException ex) {
                        current.sendMessage(Protocol.ERROR + ":Invalid move format");
                        continue;
                    }
                    int row = board.dropToken(col, currentPlayer);
                    if (row == -1) {
                        current.sendMessage(Protocol.ERROR + ":Column full or invalid");
                        continue;
                    }
                    log.info(current.getUsername() + " placed at col=" + col + ", row=" + row);
                    broadcastBoard();

                    // Win?
                    if (board.checkWin(currentPlayer)) {
                        log.info("Player " + currentPlayer + " wins");
                        broadcastMessage(Protocol.GAMEOVER + ":Player " + currentPlayer + " wins!");
                        recordWin(currentPlayer);
                        gameOver = true;
                        break;
                    }
                    // Draw?
                    if (board.isFull()) {
                        log.info("Draw detected");
                        broadcastMessage(Protocol.GAMEOVER + ":Draw!");
                        recordDraw();
                        gameOver = true;
                        break;
                    }

                    // Next turn
                    currentPlayer = (currentPlayer == 1 ? 2 : 1);
                }
                else if (msg.startsWith(Protocol.CHAT + ":")) {
                    log.info("Chat from " + current.getUsername() + ": " + msg.substring((Protocol.CHAT + ":").length()));
                    broadcastMessage(msg);
                }
                else {
                    current.sendMessage(Protocol.ERROR + ":Unknown command");
                }
            }

            // Replay prompt
            if (!askReplay()) {
                running = false;
            } else {
                board = new GameBoard();
                currentPlayer = 1;
                sendGameStart();
            }
        }

        // Cleanup
        p1.close();
        p2.close();
        log.info("Session ended: " + p1.getUsername() + " vs " + p2.getUsername());
    }

    private void sendGameStart() {
        p1.sendMessage(Protocol.GAME_START + ":You are Player 1 (Red)");
        p2.sendMessage(Protocol.GAME_START + ":You are Player 2 (Yellow)");
        broadcastBoard();
    }

    private void broadcastBoard() {
        String data = board.serialize();
        String msg = Protocol.BOARD + ":" + data;
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }

    private void broadcastMessage(String msg) {
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }

    private boolean askReplay() {
        p1.sendMessage(Protocol.END + ":Play again? (yes/no)");
        p2.sendMessage(Protocol.END + ":Play again? (yes/no)");
        try {
            String r1 = p1.readMessage();
            String r2 = p2.readMessage();
            log.info("Replay response: " + p1.getUsername() + "=" + r1 + ", "
                     + p2.getUsername() + "=" + r2);
            if ("yes".equalsIgnoreCase(r1) && "yes".equalsIgnoreCase(r2)) {
                return true;
            }
        } catch (IOException e) {
            log.warning("Error during replay dialog");
        }
        broadcastMessage(Protocol.STATUS + ":Session ending.");
        return false;
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
