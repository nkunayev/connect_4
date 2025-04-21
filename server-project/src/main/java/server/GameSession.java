// GameSession.java
// Final fix: fixed persistent YOUR_TURN bug and enabled chat for both players

package server;

import java.io.IOException;

public class GameSession implements Runnable {
    private final ClientHandler player1;
    private final ClientHandler player2;
    private GameBoard board;
    private int currentPlayer;
    private boolean running;

    public GameSession(ClientHandler p1, ClientHandler p2) {
        this.player1 = p1;
        this.player2 = p2;
        this.board = new GameBoard();
        this.currentPlayer = 1;
        this.running = true;
    }

    @Override
    public void run() {
        try {
            sendInitialMessages();
            while (running) {
                gameLoop();
                if (!askReplay()) {
                    running = false;
                } else {
                    board = new GameBoard();
                    currentPlayer = 1;
                    broadcastBoard();
                }
            }
        } catch (Exception e) {
            System.err.println("[GameSession] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void sendInitialMessages() throws IOException {
        player1.sendMessage("GAME_START:You are Player 1 (Red)");
        player2.sendMessage("GAME_START:You are Player 2 (Yellow)");
        broadcastBoard();
    }

    private void gameLoop() throws IOException {
        while (true) {
            ClientHandler current = (currentPlayer == 1) ? player1 : player2;
            ClientHandler waiting = (currentPlayer == 1) ? player2 : player1;

            // Send both the correct turn status
            current.sendMessage("YOUR_TURN");
            waiting.sendMessage("STATUS:Waiting for opponent's move...");

            String message = current.readMessage();
            if (message == null) {
                waiting.sendMessage("GAMEOVER:Opponent disconnected.");
                running = false;
                return;
            }

            if (message.startsWith("MOVE:")) {
                boolean gameEnded = handleMove(message);
                if (gameEnded) return;
            } else if (message.startsWith("CHAT:")) {
                processChat(current, message);
            } else {
                current.sendMessage("ERROR:Unrecognized command.");
            }
        }
    }

    private boolean handleMove(String message) {
        try {
            int col = Integer.parseInt(message.substring(5).trim());
            int row = board.dropToken(col, currentPlayer);

            if (row == -1) {
                return false; // invalid move
            }

            broadcastBoard();
            broadcastMessage("STATUS:Player " + currentPlayer + " moved.");

            if (board.checkWin(currentPlayer)) {
                broadcastMessage("GAMEOVER:Player " + currentPlayer + " wins!");
                return true;
            } else if (board.isFull()) {
                broadcastMessage("GAMEOVER:Draw!");
                return true;
            }

            currentPlayer = (currentPlayer == 1) ? 2 : 1;
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void processChat(ClientHandler sender, String message) {
        String msg = message.substring(5).trim();
        String displayName = (sender == player1) ? "Player 1" : "Player 2";
        broadcastMessage("CHAT:" + displayName + ": " + msg);
    }

    private void broadcastBoard() {
        String boardState = "BOARD:" + board.serialize();
        player1.sendMessage(boardState);
        player2.sendMessage(boardState);
    }

    private void broadcastMessage(String msg) {
        player1.sendMessage(msg);
        player2.sendMessage(msg);
    }

    private boolean askReplay() {
        broadcastMessage("END:Play again? (yes/no)");
        try {
            player1.sendMessage("PROMPT:Replay? Type 'yes' or 'no'");
            player2.sendMessage("PROMPT:Replay? Type 'yes' or 'no'");

            String r1 = player1.readMessage();
            String r2 = player2.readMessage();

            if (r1 != null && r2 != null && r1.equalsIgnoreCase("yes") && r2.equalsIgnoreCase("yes")) {
                return true;
            } else {
                broadcastMessage("CHAT:One player declined replay. Session ending.");
                return false;
            }
        } catch (IOException e) {
            broadcastMessage("CHAT:Replay error. Ending session.");
            return false;
        }
    }

    private void cleanup() {
        player1.close();
        player2.close();
        System.out.println("[GameSession] Session ended.");
    }
}
