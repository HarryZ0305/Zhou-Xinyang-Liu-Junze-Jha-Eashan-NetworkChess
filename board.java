public class Board {
    Block[][] blocks;
    public Board() {
        blocks = new Block[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                blocks[i][j] = new Block(i,j);
            }
        }
        for (int i = 0; i < 8; i++) {
            blocks[1][i].piece = new Piece(1, i, "pawn", false);
            blocks[6][i].piece = new Piece(6, i, "pawn", true);
        }
        blocks[0][0].piece = new Piece(0, 0, "rook", false);
        blocks[0][7].piece = new Piece(0, 7, "rook", false);
        blocks[7][0].piece = new Piece(7, 0, "rook", true);
        blocks[7][7].piece = new Piece(7, 7, "rook", true);
        blocks[0][1].piece = new Piece(0, 1, "knight", false);
        blocks[0][6].piece = new Piece(0, 6, "knight", false);
        blocks[7][1].piece = new Piece(7, 1, "knight", true);
        blocks[7][6].piece = new Piece(7, 6, "knight", true);
        blocks[0][2].piece = new Piece(0, 2, "bishop", false);
        blocks[0][5].piece = new Piece(0, 5, "bishop", false);
        blocks[7][2].piece = new Piece(7, 2, "bishop", true);
        blocks[7][5].piece = new Piece(7, 5, "bishop", true);
        blocks[0][3].piece = new Piece(0, 3, "queen", false);
        blocks[0][4].piece = new Piece(0, 4, "king", false);
        blocks[7][3].piece = new Piece(7, 3, "queen", true);
        blocks[7][4].piece = new Piece(7, 4, "king", true);
    }
}