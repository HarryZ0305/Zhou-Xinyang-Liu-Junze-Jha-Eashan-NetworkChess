public class Game {
    Board board;
    Player whitePlayer, blackPlayer;
    boolean whiteTurn;
    public Game(){
        board = new Board();
        whiteTurn = true;
        whitePlayer = new Player("White", true);
        blackPlayer = new Player("Black", false);
        whitePlayer.pieces = new Piece[]{board.board[6][0], board.board[6][1], board.board[6][2], board.board[6][3], board.board[6][4], board.board[6][5], board.board[6][6], board.board[6][7], board.board[7][0], board.board[7][1], board.board[7][2], board.board[7][3], board.board[7][4], board.board[7][5], board.board[7][6], board.board[7][7]};
        blackPlayer.pieces = new Piece[]{board.board[1][0], board.board[1][1], board.board[1][2], board.board[1][3], board.board[1][4], board.board[1][5], board.board[1][6], board.board[1][7], board.board[0][0], board.board[0][1], board.board[0][2], board.board[0][3], board.board[0][4], board.board[0][5], board.board[0][6], board.board[0][7]};
    }

    public boolean canMove(int x1, int y1, int x2, int y2, boolean isWhite){
        Piece piece = board.board[x1][y1];
        return piece.isValidMove(x2, y2, board.board);
    }

    public void Move(int x1, int y1, int x2, int y2, boolean isWhite){
        
    }
    public static void main(String[] args) {
        
    }
}