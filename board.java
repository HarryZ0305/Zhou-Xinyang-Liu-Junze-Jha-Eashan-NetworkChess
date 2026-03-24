public class Board {
    block[][] blocks;
    public Board() {
        blocks = new block[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                blocks[i][j] = new block(i,j);
            }
        }
        for (int i = 0; i < 8; i++) {
            blocks[1][i].piece = new piece(1, i, "pawn", false);
            blocks[6][i].piece = new piece(6, i, "pawn", true);
        }
        blocks[0][0].piece = new piece(0, 0, "rook", false);
        blocks[0][7].piece = new piece(0, 7, "rook", false);
        blocks[7][0].piece = new piece(7, 0, "rook", true);
        blocks[7][7].piece = new piece(7, 7, "rook", true);
        blocks[0][1].piece = new piece(0, 1, "knight", false);
        blocks[0][6].piece = new piece(0, 6, "knight", false);
        blocks[7][1].piece = new piece(7, 1, "knight", true);
        blocks[7][6].piece = new piece(7, 6, "knight", true);
        blocks[0][2].piece = new piece(0, 2, "bishop", false);
        blocks[0][5].piece = new piece(0, 5, "bishop", false);
        blocks[7][2].piece = new piece(7, 2, "bishop", true);
        blocks[7][5].piece = new piece(7, 5, "bishop", true);
        blocks[0][3].piece = new piece(0, 3, "queen", false);
        blocks[0][4].piece = new piece(0, 4, "king", false);
        blocks[7][3].piece = new piece(7, 3, "queen", true);
        blocks[7][4].piece = new piece(7, 4, "king", true);
    }
}