public class Bishop extends Piece {
    public Bishop(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean checkRule(int toRow, int toCol, Piece[][] board) {
        if (toRow == row || toCol == col) {
            return false;
        }
        if (Math.abs(toRow - row) != Math.abs(toCol - col)) {
            return false;
        }
        return isPathClear(toRow, toCol, board);
    }
}
