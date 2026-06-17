public abstract class Piece {
    int row, col;
    boolean isWhite,moved;
    String type;

    public Piece(int row, int col, boolean isWhite) {
        this.row = row;
        this.col = col;
        this.isWhite = isWhite;
    }

    // Every subclass MUST implement this
    public abstract boolean checkRule(int toRow, int toCol, Piece[][] board);

    public String getType() {
        return this.getClass().getSimpleName(); // returns "Pawn", "Rook", etc.
    }

    protected boolean isPathClear(int toRow, int toCol, Piece[][] board) {
        int rDir = Integer.compare(toRow, row);
        int cDir = Integer.compare(toCol, col);
        
        int rCheck = row + rDir;
        int cCheck = col + cDir;
        
        while (rCheck != toRow || cCheck != toCol) {
            if (board[rCheck][cCheck] != null) {
                return false;
            }
            rCheck += rDir;
            cCheck += cDir;
        }
        return true;
    }
}
