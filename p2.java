public class p2 {
    public static void main(String[] args) {
        Board board = new Board();
        Player whitePlayer = new Player("White", true);
        Player blackPlayer = new Player("Black", false);
        whitePlayer.pieces = new Piece[]{board.blocks[6][0].piece, board.blocks[6][1].piece, board.blocks[6][2].piece, board.blocks[6][3].piece, board.blocks[6][4].piece, board.blocks[6][5].piece, board.blocks[6][6].piece, board.blocks[6][7].piece, board.blocks[7][0].piece, board.blocks[7][1].piece, board.blocks[7][2].piece, board.blocks[7][3].piece, board.blocks[7][4].piece, board.blocks[7][5].piece, board.blocks[7][6].piece, board.blocks[7][7].piece};
        blackPlayer.pieces = new Piece[]{board.blocks[1][0].piece, board.blocks[1][1].piece, board.blocks[1][2].piece, board.blocks[1][3].piece, board.blocks[1][4].piece, board.blocks[1][5].piece, board.blocks[1][6].piece, board.blocks[1][7].piece, board.blocks[0][0].piece, board.blocks[0][1].piece, board.blocks[0][2].piece, board.blocks[0][3].piece, board.blocks[0][4].piece, board.blocks[0][5].piece, board.blocks[0][6].piece, board.blocks[0][7].piece};
    }
}