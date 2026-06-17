public class King extends Piece {
    public King(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean checkRule(int toRow, int toCol, Piece[][] board) {
    	
    		if(!this.moved){
            //castling
            if(toRow == row && Math.abs(toCol - col) == 2){
            		int rookCol = toCol == col + 2 ? 7 : 0;
            		Piece rook = board[row][rookCol];
            		if(rook instanceof Rook && ((Rook) rook).isOriginalRook && rook.isWhite == this.isWhite && !rook.moved){
            			//check if path is clear
            			int step = toCol > col ? 1 : -1;
            			for(int c = col + step; c != rookCol; c += step){
            				if(board[row][c] != null){
            					return false;
            				}
            			}
            			return true;
            		}
            }
        } 
        return Math.abs(toRow - row) <= 1 && Math.abs(toCol - col) <= 1;
    }
}
