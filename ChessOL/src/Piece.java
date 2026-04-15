public abstract class Piece {
    int row, col;
    boolean isWhite,moved;

    public Piece(int row, int col, boolean isWhite) {
        this.row = row;
        this.col = col;
        this.isWhite = isWhite;
        this.moved=false;
    }

    // Every subclass MUST implement this
    public abstract boolean checkRule(int toRow, int toCol, Piece[][] board);

    public String getType() {
        return this.getClass().getSimpleName(); // returns "Pawn", "Rook", etc.
    }
}
