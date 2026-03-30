public class Board {
    Piece[][] board;
    public Board() {
        board = new Piece[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = null;
            }
        }
        for (int i = 0; i < 8; i++) {
            board[1][i] = new Piece(1, i, "pawn", false);
            board[6][i] = new Piece(6, i, "pawn", true);
        }
        board[0][0] = new Piece(0, 0, "rook", false);
        board[0][7] = new Piece(0, 7, "rook", false);
        board[7][0] = new Piece(7, 0, "rook", true);
        board[7][7] = new Piece(7, 7, "rook", true);
        board[0][1] = new Piece(0, 1, "knight", false);
        board[0][6] = new Piece(0, 6, "knight", false);
        board[7][1] = new Piece(7, 1, "knight", true);
        board[7][6] = new Piece(7, 6, "knight", true);
        board[0][2] = new Piece(0, 2, "bishop", false);
        board[0][5] = new Piece(0, 5, "bishop", false);
        board[7][2] = new Piece(7, 2, "bishop", true);
        board[7][5] = new Piece(7, 5, "bishop", true);
        board[0][3] = new Piece(0, 3, "queen", false);
        board[0][4] = new Piece(0, 4, "king", false);
        board[7][3] = new Piece(7, 3, "queen", true);
        board[7][4] = new Piece(7, 4, "king", true);
    }
}