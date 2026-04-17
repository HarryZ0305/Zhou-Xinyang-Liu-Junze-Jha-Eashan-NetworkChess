public class Pawn extends Piece {
	public Pawn(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean checkRule(int toRow, int toCol, Piece[][] board) {
        
    	if(col == toCol) {
    		int direction = isWhite ? -1 : 1; // white moves up, black moves down
            if(!moved) {
            	if(Math.abs(toRow - row) == 2) {
            		direction *= 2;
            	}
            	moved = true;
            }
            return toCol == col && toRow == row + direction;
    	}
    	
    	if(Math.abs(toCol - col) > 1 || Math.abs(toRow - row) > 1) {
    		return false;
    	}
    	
    	int direction = isWhite ? -1 : 1;
    	
    	if(board[row + direction][toCol] == null) {
    		return false;
    	}
    	
    	if(board[row + direction][toCol].isWhite != isWhite) {
    		moved = true;
    		return true;
    	}
    	
    	return false;
        
    }
}
