import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.InputStream;

/**
 * Core model class for a chess match. Maintains the full game state — an 8x8 2D array of
 * Piece objects, both Player objects, side-to-move, and captured-piece lists — and exposes
 * the rule-checking and mutation methods that the GUI and bot drive against it. All board
 * logic (move legality, check detection, castling, en passant, promotion) lives here so
 * that the same authoritative rules are used by local play, networked play, and the search bot.
 */
public class Game {
    // 2D array representing the board: board[row][col]. Null entries are empty squares.
    Piece[][] board;
    Player whitePlayer, blackPlayer;
    boolean whiteTurn;
    // Captured pieces are kept in colour-keyed lists so the GUI can display each side's losses.
    public ArrayList<Piece> capturedWhite = new ArrayList<>();
    public ArrayList<Piece> capturedBlack = new ArrayList<>();

    /**
     * Builds a fresh game in the standard chess starting position. Populates the 2D board
     * array with polymorphic Piece subclasses (Pawn, Rook, Knight, Bishop, Queen, King),
     * registers every piece with its owning Player, and attempts to load custom player
     * names from username.txt — falling back to defaults if the file is missing/malformed.
     */
    public Game(){
        board = new Piece[8][8];
        // Place both pawn ranks. Polymorphism: the array holds Piece references but the
        // actual objects are Pawn subclasses, so each piece carries its own movement rules.
        for (int i = 0; i < 8; i++) {
            board[1][i] = new Pawn(1, i, false);
            board[6][i] = new Pawn(6, i, true);
        }
        // Back-rank major pieces. Row 0 = Black's back rank, row 7 = White's back rank.
        // Each constructor call records the piece's own coordinates and colour so movement
        // logic later can ask the piece where it sits without consulting the board array.
        board[0][0] = new Rook(0, 0, false);
        board[0][7] = new Rook(0, 7, false);
        board[7][0] = new Rook(7, 0, true);
        board[7][7] = new Rook(7, 7, true);
        board[0][1] = new Knight(0, 1, false);
        board[0][6] = new Knight(0, 6, false);
        board[7][1] = new Knight(7, 1, true);
        board[7][6] = new Knight(7, 6, true);
        board[0][2] = new Bishop(0, 2, false);
        board[0][5] = new Bishop(0, 5, false);
        board[7][2] = new Bishop(7, 2, true);
        board[7][5] = new Bishop(7, 5, true);
        board[0][3] = new Queen(0, 3, false);
        board[7][3] = new Queen(7, 3, true);
        board[0][4] = new King(0, 4, false); // Black king on e8 in standard chess notation
        board[7][4] = new King(7, 4, true); // White king on e1 in standard chess notation
        // File I/O: read player names from the resource file. Try-with-resources guarantees
        // both the InputStream and Scanner are closed even if an exception is thrown.
        String name1,name2;
        try (InputStream in = Resources.open("src/username.txt");
            Scanner scanner = new Scanner(in)) {
            name1 = scanner.next();
            name2 = scanner.next();
        } catch (Exception e) {
            // Fallback names so the game remains playable when the file is unavailable.
            name1 = "Player 1";
            name2 = "Player 2";
        }
        whitePlayer = new Player(name1, true);
        blackPlayer = new Player(name2, false);
        // Register every starting piece with its owning Player so each side has a fast
        // lookup of its remaining pieces (used by move generation and check detection).
        for(int i = 0; i < 8; i++){
            whitePlayer.pieces.add(board[6][i]);
            blackPlayer.pieces.add(board[1][i]);
            whitePlayer.pieces.add(board[7][i]);
            blackPlayer.pieces.add(board[0][i]);
        }

        whiteTurn = true; // White moves first by convention; flipping this is the only change needed for random starts.
    }

    /**
     * Deep-copy constructor used by the search bot to explore hypothetical positions
     * without disturbing the real game. A shallow copy of the board array would still
     * share Piece objects with the original, so a move applied to the copy would mutate
     * piece coordinates in the real game; this constructor instead allocates a fresh
     * 8x8 array AND clones every Piece via {@link #clonePiece(Piece)} so the two Game
     * instances become fully independent. File I/O is skipped and names are inherited directly from the source.
     */
    public Game(Game src) {
        board = new Piece[8][8];
        whitePlayer = new Player(src.whitePlayer.name, true);
        blackPlayer = new Player(src.blackPlayer.name, false);
        // Walk every square in the source board and place an independent clone in the new board.
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = src.board[r][c];
                if (p == null) continue;
                Piece np = clonePiece(p);
                board[r][c] = np;
                // Mirror the source's per-Player piece lists so canMove/isInCheck stay consistent.
                if (np.isWhite) whitePlayer.pieces.add(np);
                else            blackPlayer.pieces.add(np);
            }
        }
        whiteTurn = src.whiteTurn;

        // Captured lists are copied by value (new ArrayList from the source list) so the
        // copy's history can diverge from the original without aliasing.
        capturedWhite = new ArrayList<>(src.capturedWhite);
        capturedBlack = new ArrayList<>(src.capturedBlack);
    }

    /**
     * Helper for the deep-copy constructor. Reads the runtime type of the source Piece via
     * getType() and constructs a matching new subclass instance, then copies over the
     * state flags (moved, and Pawn's firstMoveTwoSquares en-passant marker) that the rule
     * engine relies on. Returns null for an unknown type — the caller skips nulls.
     */
    private static Piece clonePiece(Piece p) {
        Piece np;
        // Dispatch on the runtime piece type. Pawns get extra treatment because their
        // en-passant flag (firstMoveTwoSquares) is part of the legal state and must
        // carry over to the clone — without it the copy would lose en-passant rights.
        switch (p.getType()) {
            case "Pawn":
                Pawn pawn = new Pawn(p.row, p.col, p.isWhite);
                pawn.firstMoveTwoSquares = ((Pawn) p).firstMoveTwoSquares;
                np = pawn;
                break;
            case "Knight": np = new Knight(p.row, p.col, p.isWhite); break;
            case "Bishop": np = new Bishop(p.row, p.col, p.isWhite); break;
            case "Rook":   np = new Rook(p.row, p.col, p.isWhite);   break;
            case "Queen":  np = new Queen(p.row, p.col, p.isWhite);  break;
            case "King":   np = new King(p.row, p.col, p.isWhite);   break;
            default: return null; // unknown type means the source board is corrupt — propagate as null
        }
        np.moved = p.moved; // 'moved' flag is critical for castling rights and pawn double-step legality
        return np;
    }


    /**
     * Central legality test for a candidate move from (fromRow,fromCol) to (toRow,toCol).
     * Combines three layers of validation:
     *  (1) cheap state checks — correct turn, on-board coordinates, source piece exists,
     *      destination is not occupied by a friendly piece;
     *  (2) piece-specific movement rules via the polymorphic checkRule() method, plus the
     *      special-case requirement that a castling king does not pass through check;
     *  (3) a temporary mutation of the board to simulate the move and confirm the player's
     *      own king is not left in check — the move is rolled back before this method returns
     *      so the caller observes no state change regardless of the result.
     * @return true only if the move is fully legal under standard chess rules.
     */
    public boolean canMove(int fromRow, int fromCol, int toRow, int toCol, boolean isWhite) {

        // Reject obviously invalid input before any expensive work: wrong turn, no-op
        // move, or coordinates that would index outside the 8x8 array.
        if (isWhite != whiteTurn) {
            return false;
        }

        if (toRow == fromRow && toCol == fromCol) {
            return false;
        }

        if (toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
            return false;
        }

        if (fromRow < 0 || fromRow > 7 || fromCol < 0 || fromCol > 7) {
            return false;
        }

        Piece piece = board[fromRow][fromCol];

        if (piece == null) {
            return false;
        }

        // The destination must be empty or hold an enemy piece (you cannot capture your own).
        // The piece must also belong to the side that is trying to move it.
        Piece target = board[toRow][toCol];
        if (target == null || target.isWhite != piece.isWhite) {
            if (isWhite == piece.isWhite) {
                // Delegate the piece-specific movement pattern (sliding, L-shape, pawn rules,
                // path blocking, etc.) to the overridden checkRule() method on each subclass.
                if (piece.checkRule(toRow, toCol, board)) {

                    // Castling: king cannot start in check or pass through an attacked square.
                    // Walk every square the king traverses and reject if any is attacked.
                    if (piece instanceof King && Math.abs(toCol - fromCol) == 2) {
                        int step = toCol > fromCol ? 1 : -1;
                        for (int c = fromCol; c != toCol; c += step) {
                            if (this.isInCheck(fromRow, c, isWhite)) {
                                return false;
                            }
                        }
                    }

                    // Simulate move for King safety.
                    // En passant must be detected here because the captured pawn does not sit
                    // on the destination square — it sits one rank behind it — so a plain
                    // swap of source/destination would leave that pawn alive during the test.
                    Piece tempTarget = board[toRow][toCol];
                    boolean isEnPassant = piece instanceof Pawn
                            && fromCol != toCol
                            && tempTarget == null;
                    Piece epCaptured = isEnPassant ? board[fromRow][toCol] : null;

                    // Apply the hypothetical move directly to the live board AND mirror it
                    // in the enemy's piece list, because isInCheck() iterates that list to
                    // detect attackers. Both mutations are reverted further down.
                    board[toRow][toCol] = piece;
                    board[fromRow][fromCol] = null;
                    if (isEnPassant) {
                        board[fromRow][toCol] = null;
                    }
                    ArrayList<Piece> enemyPieces = isWhite ? blackPlayer.pieces : whitePlayer.pieces;
                    if (tempTarget != null) {
                        enemyPieces.remove(tempTarget);
                    }
                    if (epCaptured != null) {
                        enemyPieces.remove(epCaptured);
                    }
                    // Locate the king for the safety test. If the moving piece IS the king,
                    // its new square is used directly — looking it up via getKing() would
                    // still return the right object, but reading toRow/toCol avoids the scan.
                    int kingRow, kingCol;
                    if (piece instanceof King) {
                        kingRow = toRow;
                        kingCol = toCol;
                    } else {
                        Piece king = getKing(isWhite);
                        kingRow = king.row;
                        kingCol = king.col;
                    }

                    boolean putsKingInCheck = this.isInCheck(kingRow, kingCol, isWhite);

                    // Revert simulated move so the caller observes an unchanged board.
                    // Every mutation above is undone in reverse: square contents, captured
                    // piece restoration, and re-adding any pieces removed from the enemy list.
                    board[toRow][toCol] = tempTarget;
                    board[fromRow][fromCol] = piece;
                    if (isEnPassant) {
                        board[fromRow][toCol] = epCaptured;
                    }
                    if (tempTarget != null) {
                        enemyPieces.add(tempTarget);
                    }
                    if (epCaptured != null) {
                        enemyPieces.add(epCaptured);
                    }
                    // A move that exposes one's own king is illegal even if it satisfies
                    // every other rule (this is the "pinned piece" case).
                    if (putsKingInCheck) {
                        return false;
                    }

                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Commits a move that has already been validated by canMove(). Performs four state
     * updates in order: (1) handles castling by sliding the rook into its post-castle
     * square, (2) clears the en-passant window from every pawn before re-arming it on
     * the moving pawn if it advanced two squares, (3) routes any capture (normal or en
     * passant) through capture() so the piece lists stay in sync, and (4) physically
     * moves the piece on the 2D board array and updates its row/col/moved fields.
     */
    public void Move(int fromRow, int fromCol, int toRow, int toCol, boolean isWhite){

    	// Castling Logic — only triggered when the king slides two squares horizontally.
    	// The rook on the matching side is hopped over to land directly next to the king's
    	// destination, mirroring the standard chess castling motion.
    	if(Math.abs(toCol - fromCol) == 2 && board[fromRow][fromCol] instanceof King && !board[fromRow][fromCol].moved){
            int rookCol = toCol > fromCol ? 7 : 0;
            if(board[fromRow][rookCol] instanceof Rook && !board[fromRow][rookCol].moved){
                int newRookCol = toCol > fromCol ? toCol - 1 : toCol + 1;
                board[fromRow][newRookCol] = board[fromRow][rookCol];
                board[fromRow][rookCol] = null;
                board[fromRow][newRookCol].col = newRookCol;
                board[fromRow][newRookCol].moved = true;
            }
        }

        // En-passant is only legal on the move immediately after a pawn double-step, so
        // clear every pawn's two-square flag first; the moving pawn (if applicable) will
        // re-arm its own flag below.
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] instanceof Pawn) {
                    ((Pawn) board[r][c]).firstMoveTwoSquares = false;
                }
            }
        }

    	if(board[fromRow][fromCol] instanceof Pawn && Math.abs(toRow - fromRow) == 2) {
    		((Pawn) board[fromRow][fromCol]).firstMoveTwoSquares = true;
    	}

    	// Capture routing — a pawn moving diagonally is always a capture, but the captured
    	// piece sits on a different square for en passant (one rank behind the destination)
    	// than for a normal pawn capture.
    	if(board[fromRow][fromCol] instanceof Pawn && fromCol != toCol) {
    		if(board[toRow][toCol] == null) {
				capture(fromRow, toCol, isWhite); //capturing en passant
				board[fromRow][toCol] = null; //Removing the captured pawn
    		}else {
    			capture(toRow, toCol, isWhite); //Normal capture
    		}
    	} else if(board[toRow][toCol] != null) {
    		capture(toRow, toCol, isWhite);
    	}

        // Movement mapping — update the 2D board array and synchronise the piece's own
        // cached coordinates. The 'moved' flag is set so future castling/double-step rules
        // can reject this piece on subsequent turns.
        board[toRow][toCol] = board[fromRow][fromCol];
        board[fromRow][fromCol] = null;
        board[toRow][toCol].row = toRow;
        board[toRow][toCol].col = toCol;
        board[toRow][toCol].moved = true;
    }

    /**
     * Removes the piece on (toRow,toCol) from its owner's active list and appends it to
     * the matching captured list so the GUI's CapturedPanel can display it. Returns the
     * captured Piece in case the caller needs to reference it.
     */

    public Piece capture(int toRow, int toCol, boolean isWhite){
        Piece captured = board[toRow][toCol];
        if(isWhite){
            blackPlayer.pieces.remove(captured);
            capturedBlack.add(captured);
        }else{
            whitePlayer.pieces.remove(captured);
            capturedWhite.add(captured); 
        }
        return captured;
    }

    /**
     * Reports whether the given square would be attacked by any enemy piece. Iterates the
     * opposing Player's piece list and asks each piece whether it could legally reach
     * (row,col). Pawns are special-cased because their attack pattern (diagonal) differs
     * from their movement pattern (forward), so checkRule() — which models movement — would not catch them.
     */
    public boolean isInCheck(int row, int col, boolean isWhite){
        ArrayList<Piece> enemy = !isWhite ? whitePlayer.pieces : blackPlayer.pieces;
        for (Piece p : enemy) {
            if (p instanceof Pawn) {
                //Pawns attack the two diagonals one rank ahead
                int direction = p.isWhite ? -1 : 1;
                if (row == p.row + direction && Math.abs(col - p.col) == 1) {
                    return true;
                }
            } else if (p.checkRule(row, col, board)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convenience overload — locates the player's king and tests whether its current square
     * is attacked. Null-safe: if the king has somehow been removed (defensive guard) the
     * method reports "not in check" rather than throwing a NullPointerException.
     */
    public boolean isInCheck(boolean isWhite) {
        Piece king = getKing(isWhite);
        if (king == null) return false;
        return isInCheck(king.row, king.col, isWhite);
    }

    /**
     * Brute-force search for any legal move available to the given side. Used by the GUI
     * to detect checkmate vs. stalemate at the end of each turn: combined with isInCheck(),
     * "no legal moves" + "in check" = checkmate, while "no legal moves" + "not in check"
     * = stalemate. A snapshot copy of the piece list is iterated because canMove()
     * temporarily mutates that list during its safety simulation.
     */
    public boolean hasLegalMove(boolean isWhite) {
        ArrayList<Piece> snapshot = new ArrayList<>(
                isWhite ? whitePlayer.pieces : blackPlayer.pieces);
        for (Piece p : snapshot) {
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (canMove(p.row, p.col, r, c, isWhite)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Replaces the pawn on (row,col) with a freshly constructed piece of the player's
     * chosen type. The old pawn is removed from its Player's piece list and the new
     * piece is registered, preserving the invariant that the per-player list mirrors
     * the board. The promo code is a single character: Q, R, B, or N.
     */
    public void promotion(int row, int col, boolean isWhite,String promo){
        ArrayList<Piece> playerPieces = isWhite ? whitePlayer.pieces : blackPlayer.pieces;
        // Remove the original pawn from the player's tracked piece list so isInCheck() and
        // the move-generation pass do not see two pieces claiming the same square.
        for(Piece p:playerPieces){
            if(p.row==row&&p.col==col){
                playerPieces.remove(p);
                break; // there can only be one piece per square — stop scanning once found
            }
        }
        // Construct the chosen promotion piece and register it in BOTH the board array
        // and the player's piece list so the two views of the game state stay aligned.
        switch(promo){
            case "Q":
                board[row][col] = new Queen(row, col, isWhite);
                playerPieces.add(board[row][col]);
                break;
            case "R":
                board[row][col] = new Rook(row, col, isWhite);
                playerPieces.add(board[row][col]);
                break;
            case "B":
                board[row][col] = new Bishop(row, col, isWhite);
                playerPieces.add(board[row][col]);
                break;
            case "N":
                board[row][col] = new Knight(row, col, isWhite);
                playerPieces.add(board[row][col]);
                break;
        }
    }

    /**
     * Linear search through the given side's piece list for the King. Returns null if no
     * king is present (the canMove simulation deliberately avoids triggering an NPE here
     * by routing around getKing when the moving piece is itself the king).
     */
    public Piece getKing(boolean isWhite){
        ArrayList<Piece> playerPieces = isWhite ? whitePlayer.pieces : blackPlayer.pieces;
        Piece king=null;
        for(int i = 0; i < playerPieces.size(); i++){
            if(playerPieces.get(i).getType().equals("King")){
                king=playerPieces.get(i);
            }
        }
        return king;
    }

    /**
     * Builds a deterministic string that captures every aspect of the board state which
     * affects future legality: occupancy and identity/colour of every piece, castling
     * rights (marked with '*' on unmoved king/rook), pending en-passant windows (marked
     * with '!' on a pawn that just double-stepped), and the side to move. The hash of
     * this string is exchanged between client and server after each move so any divergence
     * between the two boards is detected immediately — a critical safeguard for network play.
     */
    public String stateSignature() {
        StringBuilder sb = new StringBuilder(80); // pre-sized so the StringBuilder rarely has to grow
        // Walk the board in row-major order so two boards in identical positions always
        // produce the same string. Empty squares are encoded as '.' to preserve spacing.
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p == null) { sb.append('.'); continue; }
                char ch;
                switch (p.getType()) {
                    case "Pawn":   ch = 'p'; break;
                    case "Knight": ch = 'n'; break;
                    case "Bishop": ch = 'b'; break;
                    case "Rook":   ch = 'r'; break;
                    case "Queen":  ch = 'q'; break;
                    case "King":   ch = 'k'; break;
                    default:       ch = '?'; break;
                }
                sb.append(p.isWhite ? Character.toUpperCase(ch) : ch); // uppercase for White, lowercase for Black
                if ((p instanceof King || p instanceof Rook) && !p.moved) sb.append('*'); // marks pieces still eligible for castling
                if (p instanceof Pawn && ((Pawn) p).firstMoveTwoSquares) sb.append('!'); // marks a pawn that just double-stepped, vulnerable to en passant
            }
        }
        sb.append(whiteTurn ? 'w' : 'b'); // side-to-move terminator so the same board with different turn flags hashes differently
        return sb.toString();
    }


    public static void main(String[] args) {
        
    }
}