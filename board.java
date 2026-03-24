public class board {
    block[][] blocks;
    public board() {
        blocks = new block[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                blocks[i][j] = new block(i, j, (i + j) % 2 == 0);
            }
        }
    }
}