public class Pawn extends Piece {
    public Pawn(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean checkRule(int toRow, int toCol, Piece[][] board) {
        if(toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
        	return false;
        }
        int direction = isWhite ? -1 : 1; // white moves up, black moves down
        return toCol == col && toRow == row + direction;
    }
}
