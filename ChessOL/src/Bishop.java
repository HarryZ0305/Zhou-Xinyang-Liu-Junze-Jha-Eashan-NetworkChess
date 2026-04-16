public class Bishop extends Piece {
    public Bishop(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean checkRule(int toRow, int toCol, Piece[][] board) {
        
        if(toRow == row || toCol == col) {
        	return false;
        }
        
        if(Math.abs(toRow - row) != Math.abs(toCol - col)) {
        	return false;
        }
        
        int rDir = Integer.compare(toRow, row);
        int cDir = Integer.compare(toCol, col);
        
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
