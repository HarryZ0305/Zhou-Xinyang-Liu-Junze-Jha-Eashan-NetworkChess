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
        }

        whiteTurn = true;//can change to random
    }


    public boolean canMove(int fromRow, int fromCol, int toRow, int toCol, boolean isWhiteTurn){
    	
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
        	if(isWhiteTurn == piece.isWhite){
        		if(piece.checkRule(toRow, toCol, board)){
                    if(piece instanceof King){
                        int step = toCol > fromCol ? 1 : -1;
                        for(int c = fromCol; c != toCol - step; c += step){
                            if(isInCheck(fromRow, c, isWhiteTurn)){
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

    public void Move(int x1, int y1, int x2, int y2, boolean isWhite){
        if(Math.abs(y2 - y1) == 2 && board[x1][y1] instanceof King){
            int rookCol = y2 > y1 ? 7 : 0;
            int newRookCol = y2 > y1 ? y2 - 1 : y2 + 1;
            board[x1][newRookCol] = board[x1][rookCol];
            board[x1][rookCol] = null;
            board[x1][newRookCol].col = newRookCol;
            board[x1][newRookCol].moved = true;
        }

        if(board[x2][y2] != null){
            capture(x1, y1, x2, y2, isWhite);
        }
        board[x2][y2] = board[x1][y1];
        board[x1][y1] = null;
        board[x2][y2].row = x2;
        board[x2][y2].col = y2;
        board[x2][y2].moved = true;
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
        ArrayList<Piece> player=isWhite?whitePlayer.pieces:blackPlayer.pieces;
        for(Piece p : player){
            if(p.checkRule(x, y, board)){
                return true;
            }
        }
        return false;
    }

    public void castling(){
        
    }

    public static void main(String[] args) {
        
    }
}