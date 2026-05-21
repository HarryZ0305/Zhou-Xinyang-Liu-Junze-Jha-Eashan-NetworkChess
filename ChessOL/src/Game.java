import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

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
        String name1,name2;
        try{
            Scanner scanner = new Scanner(new File("src/username.txt"));
            name1=scanner.next();
            name2=scanner.next();
            scanner.close();
        }catch(Exception e){
            name1="Default Player 1";
            name2="Default Player 2";
        }
        whitePlayer = new Player(name1, true);
        blackPlayer = new Player(name2, false);
        for(int i = 0; i < 8; i++){
            whitePlayer.pieces.add(board[6][i]);
            blackPlayer.pieces.add(board[1][i]);
            whitePlayer.pieces.add(board[7][i]);
            blackPlayer.pieces.add(board[0][i]);
        }

        whiteTurn = true;//can change to random
    }


    public boolean canMove(int fromRow, int fromCol, int toRow, int toCol, boolean isWhite) {
        
        if (isWhite != whiteTurn) {
            return false;
        }

        if (toRow == fromRow && toCol == fromCol) {
            return false;
        }
        
        if (toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
            return false;
        }

        if (fromRow < 0 || fromRow > 7 || fromCol < 0 || fromCol > 7) {
            return false;
        }

        Piece piece = board[fromRow][fromCol];
        
        if (piece == null) {
            return false;
        }

        Piece target = board[toRow][toCol];
        if (target == null || target.isWhite != piece.isWhite) {
            if (isWhite == piece.isWhite) {
                if (piece.checkRule(toRow, toCol, board)) {
                    
                    // Castling: king cannot start in check or pass through an attacked square.
                    if (piece instanceof King && Math.abs(toCol - fromCol) == 2) {
                        int step = toCol > fromCol ? 1 : -1;
                        for (int c = fromCol; c != toCol; c += step) {
                            if (this.isInCheck(fromRow, c, isWhite)) {
                                return false;
                            }
                        }
                    }

                    //Simulate move for King safety
                    Piece tempTarget = board[toRow][toCol];
                    boolean isEnPassant = piece instanceof Pawn
                            && fromCol != toCol
                            && tempTarget == null;
                    Piece epCaptured = isEnPassant ? board[fromRow][toCol] : null;

                    board[toRow][toCol] = piece;
                    board[fromRow][fromCol] = null;
                    if (isEnPassant) {
                        board[fromRow][toCol] = null;
                    }
                    ArrayList<Piece> enemyPieces = isWhite ? blackPlayer.pieces : whitePlayer.pieces;
                    if (tempTarget != null) {
                        enemyPieces.remove(tempTarget);
                    }
                    if (epCaptured != null) {
                        enemyPieces.remove(epCaptured);
                    }
                    int kingRow, kingCol;
                    if (piece instanceof King) {
                        kingRow = toRow;
                        kingCol = toCol;
                    } else {
                        Piece king = getKing(isWhite);
                        kingRow = king.row;
                        kingCol = king.col;
                    }

                    boolean putsKingInCheck = this.isInCheck(kingRow, kingCol, isWhite);

                    //Revert simulated move
                    board[toRow][toCol] = tempTarget;
                    board[fromRow][fromCol] = piece;
                    if (isEnPassant) {
                        board[fromRow][toCol] = epCaptured;
                    }
                    if (tempTarget != null) {
                        enemyPieces.add(tempTarget);
                    }
                    if (epCaptured != null) {
                        enemyPieces.add(epCaptured);
                    }
                    if (putsKingInCheck) {
                        return false;
                    }
                    
                    return true;
                }
            }
        }
        return false;
    }

    public void Move(int fromRow, int fromCol, int toRow, int toCol, boolean isWhite){

    	//Castling Logic
    	if(Math.abs(toCol - fromCol) == 2 && board[fromRow][fromCol] instanceof King && !board[fromRow][fromCol].moved){
            int rookCol = toCol > fromCol ? 7 : 0;
            if(board[fromRow][rookCol] instanceof Rook && !board[fromRow][rookCol].moved){
                int newRookCol = toCol > fromCol ? toCol - 1 : toCol + 1;
                board[fromRow][newRookCol] = board[fromRow][rookCol];
                board[fromRow][rookCol] = null;
                board[fromRow][newRookCol].col = newRookCol;
                board[fromRow][newRookCol].moved = true;
            }
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
				capture(fromRow, toCol, isWhite); //capturing en passant
				board[fromRow][toCol] = null; //Removing the captured pawn
    		}else {
    			capture(toRow, toCol, isWhite); //Normal capture
    		}
    	} else if(board[toRow][toCol] != null) {
    		capture(toRow, toCol, isWhite);
    	}
    		
        //Movement mapping
        board[toRow][toCol] = board[fromRow][fromCol];
        board[fromRow][fromCol] = null;
        board[toRow][toCol].row = toRow;
        board[toRow][toCol].col = toCol;
        board[toRow][toCol].moved = true;
    }

    public Piece capture(int toRow, int toCol, boolean isWhite){
        Piece captured = board[toRow][toCol];
        if(isWhite){
            blackPlayer.pieces.remove(captured);
        }else{
            whitePlayer.pieces.remove(captured);
        }
        return captured;
    }

    public boolean isInCheck(int row, int col, boolean isWhite){
        ArrayList<Piece> enemy = !isWhite ? whitePlayer.pieces : blackPlayer.pieces;
        for (Piece p : enemy) {
            if (p instanceof Pawn) {
                //Pawns attack the two diagonals one rank ahead
                int direction = p.isWhite ? -1 : 1;
                if (row == p.row + direction && Math.abs(col - p.col) == 1) {
                    return true;
                }
            } else if (p.checkRule(row, col, board)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasLegalMove(boolean isWhite) {
        ArrayList<Piece> snapshot = new ArrayList<>(
                isWhite ? whitePlayer.pieces : blackPlayer.pieces);
        for (Piece p : snapshot) {
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (canMove(p.row, p.col, r, c, isWhite)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void promotion(int row, int col, boolean isWhite,String promo){
        ArrayList<Piece> playerPieces = isWhite ? whitePlayer.pieces : blackPlayer.pieces;
        for(Piece p:playerPieces){
            if(p.row==row&&p.col==col){
                playerPieces.remove(p);
                break;
            }
        }
        switch(promo){
            case "Q":
                board[row][col] = new Queen(row, col, isWhite);
                playerPieces.add(board[row][col]);
                break;
            case "R":
                board[row][col] = new Rook(row, col, isWhite);
                playerPieces.add(board[row][col]);
                break;
            case "B":
                board[row][col] = new Bishop(row, col, isWhite);
                playerPieces.add(board[row][col]);
                break;
            case "N":
                board[row][col] = new Knight(row, col, isWhite);
                playerPieces.add(board[row][col]);
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