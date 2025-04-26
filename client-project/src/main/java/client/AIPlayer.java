package client;

import java.util.concurrent.TimeUnit;

/**
 * Connect-4 AI using minimax with alpha-beta pruning.
 */
public class AIPlayer {
    private final int aiPlayer, humanPlayer, maxDepth;

    public AIPlayer(int aiPlayer, int maxDepth) {
        this.aiPlayer    = aiPlayer;
        this.humanPlayer = 3 - aiPlayer;
        this.maxDepth    = maxDepth;
    }

    /** Returns best column [0..6] for current board. */
    public int chooseColumn(GameBoard board) {
        int bestScore = Integer.MIN_VALUE, bestCol = 0;
        for (int c = 0; c < GameBoard.COLS; c++) {
            if (!board.isValidMove(c)) continue;
            GameBoard b2 = board.copy();
            b2.dropToken(c, aiPlayer);
            int score = minimax(b2, maxDepth - 1,
                                Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            if (score > bestScore) {
                bestScore = score;
                bestCol   = c;
            }
        }
        return bestCol;
    }

    private int minimax(GameBoard board, int depth,
                        int alpha, int beta, boolean maximizing) {
        if (board.checkWin(aiPlayer))    return  1_000_000 + depth;
        if (board.checkWin(humanPlayer)) return -1_000_000 - depth;
        if (board.isFull() || depth == 0) return evaluate(board);

        if (maximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int c = 0; c < GameBoard.COLS; c++) {
                if (!board.isValidMove(c)) continue;
                GameBoard b2 = board.copy();
                b2.dropToken(c, aiPlayer);
                int eval = minimax(b2, depth - 1, alpha, beta, false);
                maxEval = Math.max(maxEval, eval);
                alpha   = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int c = 0; c < GameBoard.COLS; c++) {
                if (!board.isValidMove(c)) continue;
                GameBoard b2 = board.copy();
                b2.dropToken(c, humanPlayer);
                int eval = minimax(b2, depth - 1, alpha, beta, true);
                minEval = Math.min(minEval, eval);
                beta    = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    private int evaluate(GameBoard board) {
        int score = 0;
        score += board.countSequences(aiPlayer,   2) * 10;
        score += board.countSequences(aiPlayer,   3) * 50;
        score -= board.countSequences(humanPlayer,2) * 10;
        score -= board.countSequences(humanPlayer,3) * 50;
        return score;
    }
}
