package server;

/**
 * GameBoard: core logic for Connect Four.
 */
public class GameBoard {
    public static final int ROWS = 6;
    public static final int COLS = 7;
    private int[][] grid;

    public GameBoard() {
        grid = new int[ROWS][COLS];
    }

    public synchronized int dropToken(int col, int player) {
        if (col < 0 || col >= COLS) return -1;
        for (int row = ROWS - 1; row >= 0; row--) {
            if (grid[row][col] == 0) {
                grid[row][col] = player;
                return row;
            }
        }
        return -1;
    }

    public boolean checkWin(int player) {
        // Horizontal
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (grid[r][c] == player &&
                    grid[r][c+1] == player &&
                    grid[r][c+2] == player &&
                    grid[r][c+3] == player) return true;
            }
        }
        // Vertical
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r <= ROWS - 4; r++) {
                if (grid[r][c] == player &&
                    grid[r+1][c] == player &&
                    grid[r+2][c] == player &&
                    grid[r+3][c] == player) return true;
            }
        }
        // Diagonals
        for (int r = 3; r < ROWS; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (grid[r][c] == player &&
                    grid[r-1][c+1] == player &&
                    grid[r-2][c+2] == player &&
                    grid[r-3][c+3] == player) return true;
            }
        }
        for (int r = 0; r <= ROWS - 4; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (grid[r][c] == player &&
                    grid[r+1][c+1] == player &&
                    grid[r+2][c+2] == player &&
                    grid[r+3][c+3] == player) return true;
            }
        }
        return false;
    }

    public boolean isFull() {
        for (int c = 0; c < COLS; c++) {
            if (grid[0][c] == 0) return false;
        }
        return true;
    }

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
