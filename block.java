public class block{
    int row,col;
    Piece piece;
    public block(int row, int col) {
        this.row = row;
        this.col = col;
        this.piece = null;
    }

    public boolean hasPiece() {
        return piece != null;
    }
}