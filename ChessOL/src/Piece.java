public abstract class Piece {
    int row, col;
    boolean isWhite;

    public Piece(int row, int col, boolean isWhite) {
        this.row = row;
        this.col = col;
        this.isWhite = isWhite;
    }

    // Every subclass MUST implement this
    public abstract boolean isValidMove(int toRow, int toCol, Piece[][] board);

    public String getType() {
        return this.getClass().getSimpleName(); // returns "Pawn", "Rook", etc.
    }
}
