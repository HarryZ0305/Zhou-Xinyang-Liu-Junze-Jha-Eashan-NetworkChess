public class Pawn extends Piece {
    boolean firstMove;
	public Pawn(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
        firstMove = true;
    }

    @Override
    public boolean checkRule(int toRow, int toCol, Piece[][] board) {
        if(toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
        	return false;
        }
        int direction = isWhite ? -1 : 1; // white moves up, black moves down
        if(firstMove) {
        	if(Math.abs(toRow - row) == 2) {
        		direction *= 2;
        	}
        	firstMove = false;
        }
        return toCol == col && toRow == row + direction;
    }
}
