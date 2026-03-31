public class King extends Piece {
    public King(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean isValidMove(int toRow, int toCol, Piece[][] board) {
        return Math.abs(toRow - row) <= 1 && Math.abs(toCol - col) <= 1;
    }
}
