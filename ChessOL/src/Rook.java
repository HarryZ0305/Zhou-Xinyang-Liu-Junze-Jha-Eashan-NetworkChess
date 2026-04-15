public class Rook extends Piece {
    public Rook(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean checkRule(int toRow, int toCol, Piece[][] board) {
        //Check if the move is valid

        if(toRow == row && toCol == col) {
        	return false;
        }
        
    	if(toRow != row && toCol != col) {
        	return false;
        }
    	
        //One of them will be 0
        int rDir = Integer.compare(toRow, row);
        int cDir = Integer.compare(toCol, col);
        
        //Move one tile in the direction of where it wants to go
        int rCheck = row + rDir;
        int cCheck = col + cDir;
        
        while(rCheck != toRow || cCheck != toCol) {
        	if(board[rCheck][cCheck] != null) {
        		return false; //Another piece in its path
        	}
        	//Move again
        	rCheck += rDir;
        	cCheck += cDir;
        }
            	
    	return true;
    }
}
