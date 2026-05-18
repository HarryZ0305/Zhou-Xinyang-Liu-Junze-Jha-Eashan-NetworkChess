import java.util.ArrayList;

public class Game {
    Piece[][] board;
    Player whitePlayer, blackPlayer;
    boolean whiteTurn;
    public Game(){
        board = new Piece[8][8];
        for (int i = 0; i < 8; i++) {
            board[1][i] = new Pawn(1, i, false);
            board[6][i] = new Pawn(6, i, true);
        }
        board[0][0] = new Rook(0, 0, false);
        board[0][7] = new Rook(0, 7, false);
        board[7][0] = new Rook(7, 0, true);
        board[7][7] = new Rook(7, 7, true);
        board[0][1] = new Knight(0, 1, false);
        board[0][6] = new Knight(0, 6, false);
        board[7][1] = new Knight(7, 1, true);
        board[7][6] = new Knight(7, 6, true);
        board[0][2] = new Bishop(0, 2, false);
        board[0][5] = new Bishop(0, 5, false);
        board[7][2] = new Bishop(7, 2, true);
        board[7][5] = new Bishop(7, 5, true);
        board[0][3] = new Queen(0, 3, false);
        board[7][3] = new Queen(7, 3, true);
        board[0][4] = new King(0, 4, false);
        board[7][4] = new King(7, 4, true);

        whitePlayer = new Player("White", true);
        blackPlayer = new Player("Black", false);
        for(int i = 0; i < 8; i++){
            whitePlayer.pieces.add(board[6][i]);
            blackPlayer.pieces.add(board[1][i]);
            whitePlayer.pieces.add(board[7][i]);
            blackPlayer.pieces.add(board[0][i]);
        }

        whiteTurn = true;//can change to random
    }


    public boolean canMove(int fromRow, int fromCol, int toRow, int toCol, boolean isWhite,boolean isInCheck){
    	
        if(isWhite != whiteTurn) {
            return false;
        }

    	if(toRow == fromRow && toCol == fromCol) {
        	return false;
        }
    	
    	if(toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
        	return false;
        }
    	
    	Piece piece = board[fromRow][fromCol];
        
        if(piece == null) {
        	return false;
        }

        	Piece target = board[toRow][toCol];
        	if(target == null || target.isWhite != piece.isWhite) {
        		if(isWhite == piece.isWhite){
        			if(piece.checkRule(toRow, toCol, board)){
                    if(piece instanceof King){
                        int step = toCol > fromCol ? 1 : -1;
                        for(int c = fromCol; c != toCol; c += step){
                            if(isInCheck(fromRow, c, isWhite)){
                                return false;
                            }
                        }
                    }
        			return true;
        			}
        		}
        }
        return false;
    }

    public void Move(int fromRow, int fromCol, int toRow, int toCol, boolean isWhite){

    	//Castling Logic
    	if(Math.abs(toCol - fromCol) == 2 && board[fromRow][fromCol] instanceof King){
            int rookCol = toCol > fromCol ? 7 : 0;
            int newRookCol = toCol > fromCol ? toCol - 1 : toCol + 1;
            board[fromRow][newRookCol] = board[fromRow][rookCol];
            board[fromRow][rookCol] = null;
            board[fromRow][newRookCol].col = newRookCol;
            board[fromRow][newRookCol].moved = true;
        }
    		
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] instanceof Pawn) {
                    ((Pawn) board[r][c]).firstMoveTwoSquares = false;
                }
            }
        }
        
    	if(board[fromRow][fromCol] instanceof Pawn && Math.abs(toRow - fromRow) == 2) {
    		((Pawn) board[fromRow][fromCol]).firstMoveTwoSquares = true;
    	}
    		
    	if(board[fromRow][fromCol] instanceof Pawn && fromCol != toCol) {
    		if(board[toRow][toCol] == null) {
				capture(fromRow, fromCol, fromRow, toCol, isWhite); //capturing en passant
				board[fromRow][toCol] = null; //Removing the captured pawn
    		}else {
    			capture(fromRow, fromCol, toRow, toCol, isWhite); //Normal capture
    		}
    	} else if(board[toRow][toCol] != null) {
    		capture(fromRow, fromCol, toRow, toCol, isWhite);
    	} 
    		
        //Movement mapping
        board[toRow][toCol] = board[fromRow][fromCol];
        board[fromRow][fromCol] = null;
        board[toRow][toCol].row = toRow;
        board[toRow][toCol].col = toCol;
        board[toRow][toCol].moved = true;
    }

    public Piece capture(int x1, int y1, int x2, int y2, boolean isWhite){
        Piece captured = board[x2][y2];
        if(isWhite){
            blackPlayer.pieces.remove(captured);
        }else{
            whitePlayer.pieces.remove(captured);
        }
        return captured;
    }

    public boolean isInCheck(int x,int y,boolean isWhite){
        ArrayList<Piece> player=!isWhite?whitePlayer.pieces:blackPlayer.pieces;
        for(Piece p : player){
            if(p.checkRule(x, y, board)){
                return true;
            }
        }
        return false;
    }

    public void promotion(int x, int y, boolean isWhite,String promo){
        ArrayList<Piece> playerPieces = isWhite ? whitePlayer.pieces : blackPlayer.pieces;
        for(Piece p:playerPieces){
            if(p.row==x&&p.col==y){
                playerPieces.remove(p);
                break;
            }
        }
        switch(promo){
            case "Q":
                board[x][y] = new Queen(x, y, isWhite);
                playerPieces.add(board[x][y]);
                break;
            case "R":
                board[x][y] = new Rook(x, y, isWhite);
                playerPieces.add(board[x][y]);
                break;
            case "B":
                board[x][y] = new Bishop(x, y, isWhite);
                playerPieces.add(board[x][y]);
                break;
            case "N":
                board[x][y] = new Knight(x, y, isWhite);
                playerPieces.add(board[x][y]);
                break;
        }
    }

    public Piece getKing(boolean isWhite){
        ArrayList<Piece> playerPieces = isWhite ? whitePlayer.pieces : blackPlayer.pieces;
        Piece king=null;
        for(int i = 0; i < playerPieces.size(); i++){
            if(playerPieces.get(i).getType().equals("King")){
                king=playerPieces.get(i);
            }
        }
        return king;
    }

    public static void main(String[] args) {
        
    }
}