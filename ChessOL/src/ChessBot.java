import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Local chess bot used by the "Play with Bot" mode. Implements a fixed-depth minimax
 * search with alpha-beta pruning. The evaluation function combines two terms: raw
 * material value (queen worth more than rook, etc.) and a piece-square table bonus
 * that encourages classical positional play (knights toward the centre, kings tucked
 * away during the opening/middlegame, pawns advanced toward promotion).
 *
 * The bot plays from whichever side's turn it is in the supplied Game — it does not
 * own a colour. Move generation reuses the Game's own canMove() so the search is
 * guaranteed to respect every rule the human player respects.
 */
public class ChessBot {

    // Standard centipawn material values used across most modern chess engines.
    // Knight and bishop are deliberately close (320/330) so the bot mildly prefers the bishop pair.
    private static final int VAL_PAWN   = 100;
    private static final int VAL_KNIGHT = 320;
    private static final int VAL_BISHOP = 330;
    private static final int VAL_ROOK   = 500;
    private static final int VAL_QUEEN  = 900;
    private static final int VAL_KING   = 20000; // King value dwarfs everything else so losing it is unconditional disaster.

    private static final int MATE_SCORE = 1_000_000; // Score returned for forced mate; large enough to swamp any material swing.
    private static final int INF        = 10_000_000; // Sentinel used as the initial alpha/beta bound — must exceed MATE_SCORE.

    private static final int SEARCH_DEPTH = 3; // Plies of look-ahead. Raising this exponentially increases compute time.

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

    /**
     * Public entry point: picks the best move for whichever side has the turn in the
     * supplied Game. Generates every legal move, then for each one builds a deep copy
     * of the Game, applies the move on the copy, and asks the minimax search to score
     * the resulting position. White seeks the maximum score, Black the minimum. The
     * initial shuffle keeps the bot's play interesting when several moves tie.
     * @return int[5] = {fromRow, fromCol, toRow, toCol, promoChar or -1}, or null if
     *         the side has no legal moves (the caller treats that as game-over).
     */
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

        // Iterate every candidate root move, recursing into minimax on a deep-copied Game
        // so the real board is never mutated. Alpha and beta tighten as better moves are
        // found, allowing minimax() to prune entire subtrees that cannot improve the result.
        for (int[] mv : moves) {
            Game next = new Game(game); // Deep copy so the real board is untouched by the simulation.
            applyMove(next, mv); // Play the candidate move on the copy and flip the turn.
            int score = minimax(next, SEARCH_DEPTH - 1, alpha, beta); // Recurse one ply shallower to score this branch.
            if (side) {
                if (score > bestScore) { bestScore = score; best = mv; } // White seeks the highest scoring branch.
                if (bestScore > alpha) alpha = bestScore; // Tighten alpha so weaker siblings can be pruned.
            } else {
                if (score < bestScore) { bestScore = score; best = mv; } // Black seeks the lowest scoring branch.
                if (bestScore < beta) beta = bestScore; // Tighten beta from the minimiser's side.
            }
        }
        return best;
    }

    /**
     * Recursive minimax search with alpha-beta pruning. White is the maximiser, Black
     * the minimiser. At depth 0 the position is statically evaluated; at intermediate
     * depths the search descends one ply per recursive call. An empty move list at any
     * node means the side to move is either checkmated (return a large +/- score that
     * favours faster mates) or stalemated (return 0).
     */
    private static int minimax(Game game, int depth, int alpha, int beta) {
        if (depth == 0) return evaluate(game); // Leaf node — static evaluation is the only signal available.

        boolean side = game.whiteTurn;
        List<int[]> moves = generateMoves(game, side);

        // No legal moves means the position is terminal — either checkmate or stalemate.
        if (moves.isEmpty()) {
            Piece king = game.getKing(side);
            boolean inCheck = king != null && game.isInCheck(king.row, king.col, side);
            if (inCheck) {
                // Side to move is mated. Adding 'depth' rewards mating sooner because shallower mates leave a larger remaining depth.
                return side ? -(MATE_SCORE + depth) : (MATE_SCORE + depth);
            }
            return 0; // stalemate is scored neutrally — neither side wins material
        }

        orderMoves(game, moves);

        // Maximiser (White) and minimiser (Black) branches are mirror images. The
        // "beta <= alpha" check is the alpha-beta cutoff: as soon as one branch proves
        // the opponent has a better alternative elsewhere, the remaining siblings can be
        // skipped without affecting the final answer.
        if (side) {
            int max = -INF;
            for (int[] mv : moves) {
                Game next = new Game(game);
                applyMove(next, mv);
                int v = minimax(next, depth - 1, alpha, beta);
                if (v > max) max = v; // Track the best score the maximiser has found in this subtree.
                if (max > alpha) alpha = max; // Raise alpha so deeper recursions can prune anything worse.
                if (beta <= alpha) break; // Beta cutoff — opponent already has a refutation, no point exploring more.
            }
            return max;
        } else {
            int min = INF;
            for (int[] mv : moves) {
                Game next = new Game(game);
                applyMove(next, mv);
                int v = minimax(next, depth - 1, alpha, beta);
                if (v < min) min = v; // Track the best score the minimiser has found.
                if (min < beta) beta = min; // Lower beta so the maximiser knows the ceiling has dropped.
                if (beta <= alpha) break; // Alpha cutoff — symmetric to the maximiser's pruning.
            }
            return min;
        }
    }

    /**
     * Enumerates every legal move available to the given side by iterating each of its
     * pieces over all 64 destination squares and filtering with Game.canMove(). Promoting
     * pawns are only generated as queen promotions — under-promotions are theoretically
     * possible but almost never optimal, and skipping them cuts the branching factor by
     * roughly 4x at promotion nodes for a noticeable search-speed win.
     */
    private static List<int[]> generateMoves(Game game, boolean side) {
        List<int[]> moves = new ArrayList<>();
        // Snapshot the piece list because canMove() temporarily mutates that list during
        // its king-safety simulation; iterating the live list would risk a ConcurrentModificationException.
        ArrayList<Piece> snapshot = new ArrayList<>(
            side ? game.whitePlayer.pieces : game.blackPlayer.pieces);
        for (Piece p : snapshot) {
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (!game.canMove(p.row, p.col, r, c, side)) continue; // Skip illegal squares immediately.
                    // Only consider queen promotion — almost always optimal and 4x faster than enumerating Q/R/B/N.
                    if (p instanceof Pawn && (r == 0 || r == 7)) {
                        moves.add(new int[]{p.row, p.col, r, c, 'Q'}); // 'Q' marks promotion in the int[] schema.
                    } else {
                        moves.add(new int[]{p.row, p.col, r, c, -1}); // -1 sentinel means "no promotion".
                    }
                }
            }
        }
        return moves;
    }

    /**
     * Sorts moves so captures are explored first. Alpha-beta pruning is most effective
     * when strong moves are tried early — once a strong move tightens alpha/beta, weak
     * sibling subtrees can be cut without exploring them.
     */
    private static void orderMoves(Game game, List<int[]> moves) {
        moves.sort((a, b) -> captureScore(game, b) - captureScore(game, a));
    }

    /** Returns the material value of the piece sitting on the destination square (0 for a quiet move). */
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

    /**
     * Plays a move on the supplied Game (typically a deep copy) and hands the turn over to the other side.
     * Also handles the promotion follow-up if the move list contained a promotion character.
     */
    private static void applyMove(Game game, int[] mv) {
        boolean side = game.whiteTurn;
        game.Move(mv[0], mv[1], mv[2], mv[3], side);
        if (mv[4] != -1) {
            game.promotion(mv[2], mv[3], side, String.valueOf((char) mv[4]));
        }
        game.whiteTurn = !game.whiteTurn;
    }

    /**
     * Static evaluation function: returns a single integer score where positive favours
     * White. For every piece on the board it adds (or subtracts) the material value plus
     * the piece-square table bonus for that square. Black reads the PSTs mirrored across
     * the centre rank so each side is "encouraged" toward its own version of good squares.
     */
    private static int evaluate(Game game) {
        int score = 0;
        // Walk every square in the 2D board array and accumulate a signed total: White's
        // contributions add to the score, Black's subtract. A purely material engine would
        // skip the PST lookup, but adding the table makes the bot prefer good piece placement.
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = game.board[r][c];
                if (p == null) continue;
                int mat;
                int[][] pst;
                // Pair each piece type with its material value and matching positional table.
                switch (p.getType()) {
                    case "Pawn":   mat = VAL_PAWN;   pst = PST_PAWN;   break;
                    case "Knight": mat = VAL_KNIGHT; pst = PST_KNIGHT; break;
                    case "Bishop": mat = VAL_BISHOP; pst = PST_BISHOP; break;
                    case "Rook":   mat = VAL_ROOK;   pst = PST_ROOK;   break;
                    case "Queen":  mat = VAL_QUEEN;  pst = PST_QUEEN;  break;
                    case "King":   mat = VAL_KING;   pst = PST_KING;   break;
                    default: continue;
                }
                // Mirror the PST vertically for Black so each side reads the table from its own perspective.
                int pstVal = p.isWhite ? pst[r][c] : pst[7 - r][c];
                int val = mat + pstVal;
                score += p.isWhite ? val : -val; // Sign convention: positive = White is winning.
            }
        }
        return score;
    }
}
