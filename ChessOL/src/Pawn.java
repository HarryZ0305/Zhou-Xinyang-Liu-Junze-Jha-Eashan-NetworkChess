public class Pawn extends Piece {
    public Pawn(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean isValidMove(int toRow, int toCol, Piece[][] board) {
        int direction = isWhite ? -1 : 1; // white moves up, black moves down
        return toCol == col && toRow == row + direction;
    }
}
