public class Queen extends Piece {
    public Queen(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean isValidMove(int toRow, int toCol, Piece[][] board) {
        // Queen = Rook + Bishop combined
        boolean straightLine = toRow == row || toCol == col;
        boolean diagonal = Math.abs(toRow - row) == Math.abs(toCol - col);
        return straightLine || diagonal;
    }
}
