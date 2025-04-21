package server;

/**
 * GameBoard.java
 *
 * Implements the Connect Four game board.
 * Provides methods to drop a token into a specified column,
 * check for win conditions (horizontal, vertical, and diagonal),
 * and serialize the board state into a string.
 */
public class GameBoard {
    public static final int ROWS = 6;
    public static final int COLS = 7;
    private int[][] grid;

    public GameBoard() {
        grid = new int[ROWS][COLS];
    }
    
    /**
     * Attempts to drop a token for the given player into the chosen column.
     * @param col The column where the token should be placed.
     * @param player The identifier for the player (1 or 2).
     * @return The row in which the token landed, or -1 if the column is full.
     */
    public synchronized int dropToken(int col, int player) {
        if (col < 0 || col >= COLS)
            return -1;
        for (int row = ROWS - 1; row >= 0; row--) {
            if (grid[row][col] == 0) {
                grid[row][col] = player;
                return row;
            }
        }
        return -1; // No empty space; column is full.
    }
    
    /**
     * Checks if the specified player has achieved a win condition.
     * @param player The player to check for a win.
     * @return True if the player has four tokens in a row; otherwise false.
     */
    public boolean checkWin(int player) {
        // Horizontal check.
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col <= COLS - 4; col++) {
                if (grid[row][col] == player &&
                    grid[row][col+1] == player &&
                    grid[row][col+2] == player &&
                    grid[row][col+3] == player)
                    return true;
            }
        }
        // Vertical check.
        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row <= ROWS - 4; row++) {
                if (grid[row][col] == player &&
                    grid[row+1][col] == player &&
                    grid[row+2][col] == player &&
                    grid[row+3][col] == player)
                    return true;
            }
        }
        // Diagonal (positive slope) check.
        for (int row = 3; row < ROWS; row++) {
            for (int col = 0; col <= COLS - 4; col++) {
                if (grid[row][col] == player &&
                    grid[row-1][col+1] == player &&
                    grid[row-2][col+2] == player &&
                    grid[row-3][col+3] == player)
                    return true;
            }
        }
        // Diagonal (negative slope) check.
        for (int row = 0; row <= ROWS - 4; row++) {
            for (int col = 0; col <= COLS - 4; col++) {
                if (grid[row][col] == player &&
                    grid[row+1][col+1] == player &&
                    grid[row+2][col+2] == player &&
                    grid[row+3][col+3] == player)
                    return true;
            }
        }
        return false;
    }
    
    /**
     * Checks whether the board is completely filled.
     * @return True if the board is full; otherwise false.
     */
    public boolean isFull() {
        for (int col = 0; col < COLS; col++) {
            if (grid[0][col] == 0)
                return false;
        }
        return true;
    }
    
    /**
     * Serializes the board state into a string.
     * Format: row1,col1,...;row2,col2,...
     * @return The serialized board state.
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                sb.append(grid[row][col]);
                if (col < COLS - 1)
                    sb.append(",");
            }
            if (row < ROWS - 1)
                sb.append(";");
        }
        return sb.toString();
    }
    
    public int[][] getGrid() {
        return grid;
    }
}
