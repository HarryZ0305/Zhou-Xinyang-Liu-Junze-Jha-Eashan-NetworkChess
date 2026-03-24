public class Board {
    block[][] blocks;
    public Board() {
        blocks = new block[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                blocks[i][j] = new block(i,j,false);
            }
        }
    }
}