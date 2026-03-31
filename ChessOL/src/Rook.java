public class Rook extends Piece {
    public Rook(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean isValidMove(int toRow, int toCol, Piece[][] board) {
        return toRow == row || toCol == col; // same row OR same column
    }
}
