public class Rook extends Piece {
    public boolean isOriginalRook = true;

    public Rook(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean checkRule(int toRow, int toCol, Piece[][] board) {
        if (toRow != row && toCol != col) {
            return false;
        }
        return isPathClear(toRow, toCol, board);
    }
}
