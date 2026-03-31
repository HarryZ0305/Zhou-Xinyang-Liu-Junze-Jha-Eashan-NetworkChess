public class Board {
    Piece[][] board;

    public Board() {
        board = new Piece[8][8];

        for (int i = 0; i < 8; i++) {
            board[1][i] = new Pawn(1, i, false);
            board[6][i] = new Pawn(6, i, true);
        }

        board[0][0] = new Rook(0, 0, false);   board[0][7] = new Rook(0, 7, false);
        board[7][0] = new Rook(7, 0, true);    board[7][7] = new Rook(7, 7, true);

        board[0][1] = new Knight(0, 1, false);  board[0][6] = new Knight(0, 6, false);
        board[7][1] = new Knight(7, 1, true);   board[7][6] = new Knight(7, 6, true);

        board[0][2] = new Bishop(0, 2, false);  board[0][5] = new Bishop(0, 5, false);
        board[7][2] = new Bishop(7, 2, true);   board[7][5] = new Bishop(7, 5, true);

        board[0][3] = new Queen(0, 3, false);   board[7][3] = new Queen(7, 3, true);
        board[0][4] = new King(0, 4, false);    board[7][4] = new King(7, 4, true);
    }
}
