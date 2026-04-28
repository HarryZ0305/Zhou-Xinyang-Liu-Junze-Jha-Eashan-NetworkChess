public class Pawn extends Piece {
	
	public boolean firstMoveTwoSquares = false;
	
	public Pawn(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean checkRule(int toRow, int toCol, Piece[][] board) {
        
    	int direction = isWhite ? -1 : 1; // white moves up, black moves down
    	
    	//Moving straight
    	if(col == toCol) {
            
    		if(toRow == row + direction) {
            	return board[toRow][toCol] == null;
            }
    		
    		//First move can move 2 squares
    		if(!moved && toRow == row + direction * 2) {
    			return board[row + direction][toCol] == null && board[toRow][toCol] == null;
    		}
    		
    		return false;
    	}
    	
    	//Moving diagonal to capture
    if(Math.abs(toCol - col) == 1 && toRow == row + direction) {
            
        	Piece target = board[toRow][toCol];
            
        	//Normal capture
        	if(target != null && target.isWhite != this.isWhite) {
                return true;
        	}
            
        	//En Passant
        	Piece sameRowPiece = board[row][toCol]; //The piece in the same row but in another column
        	if(target == null && sameRowPiece instanceof Pawn && sameRowPiece.isWhite != this.isWhite) {
        		Pawn sameRowPawn = (Pawn) sameRowPiece;
                if(sameRowPawn.firstMoveTwoSquares) {
                    return true;
                }
            }
            
        }
        return false;
    }
}
