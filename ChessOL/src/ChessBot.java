import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Local chess bot. Minimax with alpha-beta pruning + material + piece-square tables.
// Plays from whichever side's turn it is in the supplied Game.
public class ChessBot {

    private static final int VAL_PAWN   = 100;
    private static final int VAL_KNIGHT = 320;
    private static final int VAL_BISHOP = 330;
    private static final int VAL_ROOK   = 500;
    private static final int VAL_QUEEN  = 900;
    private static final int VAL_KING   = 20000;

    private static final int MATE_SCORE = 1_000_000;
    private static final int INF        = 10_000_000;

    private static final int SEARCH_DEPTH = 3;

    // PSTs are written from White's view: row 7 = White's back rank (matches board layout).
    // Black mirrors by reading PST[7 - r][c].
    private static final int[][] PST_PAWN = {
        {  0,  0,  0,  0,  0,  0,  0,  0},
        { 50, 50, 50, 50, 50, 50, 50, 50},
        { 10, 10, 20, 30, 30, 20, 10, 10},
        {  5,  5, 10, 25, 25, 10,  5,  5},
        {  0,  0,  0, 20, 20,  0,  0,  0},
        {  5, -5,-10,  0,  0,-10, -5,  5},
        {  5, 10, 10,-20,-20, 10, 10,  5},
        {  0,  0,  0,  0,  0,  0,  0,  0}
    };
    private static final int[][] PST_KNIGHT = {
        {-50,-40,-30,-30,-30,-30,-40,-50},
        {-40,-20,  0,  0,  0,  0,-20,-40},
        {-30,  0, 10, 15, 15, 10,  0,-30},
        {-30,  5, 15, 20, 20, 15,  5,-30},
        {-30,  0, 15, 20, 20, 15,  0,-30},
        {-30,  5, 10, 15, 15, 10,  5,-30},
        {-40,-20,  0,  5,  5,  0,-20,-40},
        {-50,-40,-30,-30,-30,-30,-40,-50}
    };
    private static final int[][] PST_BISHOP = {
        {-20,-10,-10,-10,-10,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5, 10, 10,  5,  0,-10},
        {-10,  5,  5, 10, 10,  5,  5,-10},
        {-10,  0, 10, 10, 10, 10,  0,-10},
        {-10, 10, 10, 10, 10, 10, 10,-10},
        {-10,  5,  0,  0,  0,  0,  5,-10},
        {-20,-10,-10,-10,-10,-10,-10,-20}
    };
    private static final int[][] PST_ROOK = {
        {  0,  0,  0,  0,  0,  0,  0,  0},
        {  5, 10, 10, 10, 10, 10, 10,  5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        {  0,  0,  0,  5,  5,  0,  0,  0}
    };
    private static final int[][] PST_QUEEN = {
        {-20,-10,-10, -5, -5,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5,  5,  5,  5,  0,-10},
        { -5,  0,  5,  5,  5,  5,  0, -5},
        {  0,  0,  5,  5,  5,  5,  0, -5},
        {-10,  5,  5,  5,  5,  5,  0,-10},
        {-10,  0,  5,  0,  0,  0,  0,-10},
        {-20,-10,-10, -5, -5,-10,-10,-20}
    };
    private static final int[][] PST_KING = {
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-20,-30,-30,-40,-40,-30,-30,-20},
        {-10,-20,-20,-20,-20,-20,-20,-10},
        { 20, 20,  0,  0,  0,  0, 20, 20},
        { 20, 30, 10,  0,  0, 10, 30, 20}
    };

    // Returns int[5]: [fromRow, fromCol, toRow, toCol, promoChar or -1].
    // Picks for whichever side has the move in game.
    public static int[] getMove(Game game) {
        boolean side = game.whiteTurn;
        List<int[]> moves = generateMoves(game, side);
        if (moves.isEmpty()) return null;

        // Shuffle so equal-score moves aren't deterministic.
        Collections.shuffle(moves);
        orderMoves(game, moves);

        int[] best = moves.get(0);
        int bestScore = side ? -INF : INF;
        int alpha = -INF;
        int beta  =  INF;

        for (int[] mv : moves) {
            Game next = new Game(game);
            applyMove(next, mv);
            int score = minimax(next, SEARCH_DEPTH - 1, alpha, beta);
            if (side) {
                if (score > bestScore) { bestScore = score; best = mv; }
                if (bestScore > alpha) alpha = bestScore;
            } else {
                if (score < bestScore) { bestScore = score; best = mv; }
                if (bestScore < beta) beta = bestScore;
            }
        }
        return best;
    }

    private static int minimax(Game game, int depth, int alpha, int beta) {
        if (depth == 0) return evaluate(game);

        boolean side = game.whiteTurn;
        List<int[]> moves = generateMoves(game, side);

        if (moves.isEmpty()) {
            Piece king = game.getKing(side);
            boolean inCheck = king != null && game.isInCheck(king.row, king.col, side);
            if (inCheck) {
                // Side to move is mated. Prefer faster mates.
                return side ? -(MATE_SCORE + depth) : (MATE_SCORE + depth);
            }
            return 0; // stalemate
        }

        orderMoves(game, moves);

        if (side) {
            int max = -INF;
            for (int[] mv : moves) {
                Game next = new Game(game);
                applyMove(next, mv);
                int v = minimax(next, depth - 1, alpha, beta);
                if (v > max) max = v;
                if (max > alpha) alpha = max;
                if (beta <= alpha) break;
            }
            return max;
        } else {
            int min = INF;
            for (int[] mv : moves) {
                Game next = new Game(game);
                applyMove(next, mv);
                int v = minimax(next, depth - 1, alpha, beta);
                if (v < min) min = v;
                if (min < beta) beta = min;
                if (beta <= alpha) break;
            }
            return min;
        }
    }

    private static List<int[]> generateMoves(Game game, boolean side) {
        List<int[]> moves = new ArrayList<>();
        ArrayList<Piece> snapshot = new ArrayList<>(
            side ? game.whitePlayer.pieces : game.blackPlayer.pieces);
        for (Piece p : snapshot) {
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (!game.canMove(p.row, p.col, r, c, side)) continue;
                    // Only consider queen promotion — almost always optimal and 4x faster.
                    if (p instanceof Pawn && (r == 0 || r == 7)) {
                        moves.add(new int[]{p.row, p.col, r, c, 'Q'});
                    } else {
                        moves.add(new int[]{p.row, p.col, r, c, -1});
                    }
                }
            }
        }
        return moves;
    }

    // Sort captures first to improve alpha-beta cutoffs.
    private static void orderMoves(Game game, List<int[]> moves) {
        moves.sort((a, b) -> captureScore(game, b) - captureScore(game, a));
    }

    private static int captureScore(Game game, int[] mv) {
        Piece target = game.board[mv[2]][mv[3]];
        if (target == null) return 0;
        return pieceValue(target);
    }

    private static int pieceValue(Piece p) {
        switch (p.getType()) {
            case "Pawn":   return VAL_PAWN;
            case "Knight": return VAL_KNIGHT;
            case "Bishop": return VAL_BISHOP;
            case "Rook":   return VAL_ROOK;
            case "Queen":  return VAL_QUEEN;
            case "King":   return VAL_KING;
            default: return 0;
        }
    }

    private static void applyMove(Game game, int[] mv) {
        boolean side = game.whiteTurn;
        game.Move(mv[0], mv[1], mv[2], mv[3], side);
        if (mv[4] != -1) {
            game.promotion(mv[2], mv[3], side, String.valueOf((char) mv[4]));
        }
        game.whiteTurn = !game.whiteTurn;
    }

    // Positive = good for White.
    private static int evaluate(Game game) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = game.board[r][c];
                if (p == null) continue;
                int mat;
                int[][] pst;
                switch (p.getType()) {
                    case "Pawn":   mat = VAL_PAWN;   pst = PST_PAWN;   break;
                    case "Knight": mat = VAL_KNIGHT; pst = PST_KNIGHT; break;
                    case "Bishop": mat = VAL_BISHOP; pst = PST_BISHOP; break;
                    case "Rook":   mat = VAL_ROOK;   pst = PST_ROOK;   break;
                    case "Queen":  mat = VAL_QUEEN;  pst = PST_QUEEN;  break;
                    case "King":   mat = VAL_KING;   pst = PST_KING;   break;
                    default: continue;
                }
                int pstVal = p.isWhite ? pst[r][c] : pst[7 - r][c];
                int val = mat + pstVal;
                score += p.isWhite ? val : -val;
            }
        }
        return score;
    }
}
