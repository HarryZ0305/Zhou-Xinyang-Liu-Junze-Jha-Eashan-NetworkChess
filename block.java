public class Block{
    int row,col;
    Piece piece;
    public Block(int row, int col) {
        this.row = row;
        this.col = col;
        piece = null;
    }

    public boolean hasPiece() {
        return piece != null;
    }
}