// client-project/src/main/java/client/GameBoard.java

package client;

/**
 * Local copy of GameBoard logic for single-player mode.
 */
public class GameBoard {
    public static final int ROWS = 6;
    public static final int COLS = 7;
    private final int[][] grid;

    /** Empty board constructor */
    public GameBoard() {
        grid = new int[ROWS][COLS];
    }

    /** Copy constructor */
    private GameBoard(int[][] g) {
        grid = new int[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            System.arraycopy(g[r], 0, grid[r], 0, COLS);
        }
    }

    /** Drop a token for player (1 or 2). Returns the row, or –1 if invalid. */
    public int dropToken(int col, int player) {
        if (col < 0 || col >= COLS || grid[0][col] != 0) return -1;
        for (int r = ROWS - 1; r >= 0; r--) {
            if (grid[r][col] == 0) {
                grid[r][col] = player;
                return r;
            }
        }
        return -1;
    }

    public boolean isValidMove(int col) {
        return col >= 0 && col < COLS && grid[0][col] == 0;
    }

    public boolean isFull() {
        for (int c = 0; c < COLS; c++) {
            if (grid[0][c] == 0) return false;
        }
        return true;
    }

    /**
     * Returns true if the given player has four in a row anywhere.
     */
    public boolean checkWin(int player) {
        // Horizontal
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (grid[r][c] == player
                 && grid[r][c+1] == player
                 && grid[r][c+2] == player
                 && grid[r][c+3] == player) {
                    return true;
                }
            }
        }
        // Vertical
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r <= ROWS - 4; r++) {
                if (grid[r][c] == player
                 && grid[r+1][c] == player
                 && grid[r+2][c] == player
                 && grid[r+3][c] == player) {
                    return true;
                }
            }
        }
        // Diagonal down-right
        for (int r = 0; r <= ROWS - 4; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (grid[r][c] == player
                 && grid[r+1][c+1] == player
                 && grid[r+2][c+2] == player
                 && grid[r+3][c+3] == player) {
                    return true;
                }
            }
        }
        // Diagonal up-right
        for (int r = 3; r < ROWS; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (grid[r][c] == player
                 && grid[r-1][c+1] == player
                 && grid[r-2][c+2] == player
                 && grid[r-3][c+3] == player) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Return a deep copy for search. */
    public GameBoard copy() {
        return new GameBoard(grid);
    }

    /**
     * Count how many contiguous sequences of exactly `length` the given player has,
     * unblocked by opponent. Used by the AI heuristic.
     */
    public int countSequences(int player, int length) {
        int count = 0;
        int opponent = 3 - player;

        // Horizontal
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c <= COLS - length; c++) {
                int pCount = 0, emptyCount = 0;
                for (int k = 0; k < length; k++) {
                    int v = grid[r][c+k];
                    if (v == player) pCount++;
                    else if (v == 0) emptyCount++;
                    else { pCount = -1; break; }
                }
                if (pCount == length) count++;
            }
        }
        // Vertical
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r <= ROWS - length; r++) {
                int pCount = 0;
                for (int k = 0; k < length; k++) {
                    int v = grid[r+k][c];
                    if (v == player) pCount++;
                    else if (v != 0) { pCount = -1; break; }
                }
                if (pCount == length) count++;
            }
        }
        // Diag down-right
        for (int r = 0; r <= ROWS - length; r++) {
            for (int c = 0; c <= COLS - length; c++) {
                int pCount = 0;
                for (int k = 0; k < length; k++) {
                    int v = grid[r+k][c+k];
                    if (v == player) pCount++;
                    else if (v != 0) { pCount = -1; break; }
                }
                if (pCount == length) count++;
            }
        }
        // Diag up-right
        for (int r = length - 1; r < ROWS; r++) {
            for (int c = 0; c <= COLS - length; c++) {
                int pCount = 0;
                for (int k = 0; k < length; k++) {
                    int v = grid[r-k][c+k];
                    if (v == player) pCount++;
                    else if (v != 0) { pCount = -1; break; }
                }
                if (pCount == length) count++;
            }
        }
        return count;
    }

    /** Serialize to "r0c0,r0c1,…;r1c0,..." format for your GUI. */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                sb.append(grid[r][c]);
                if (c < COLS - 1) sb.append(',');
            }
            if (r < ROWS - 1) sb.append(';');
        }
        return sb.toString();
    }
}
