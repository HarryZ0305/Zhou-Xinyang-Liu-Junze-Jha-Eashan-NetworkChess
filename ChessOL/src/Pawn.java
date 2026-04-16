public class Pawn extends Piece {
    boolean firstMove;
	public Pawn(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean checkRule(int toRow, int toCol, Piece[][] board) {
        
        int direction = isWhite ? -1 : 1; // white moves up, black moves down
        if(!moved) {
        	if(Math.abs(toRow - row) == 2) {
        		direction *= 2;
        	}
        	moved = true;
        }
        return toCol == col && toRow == row + direction;
    }
}
