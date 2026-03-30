public class Piece {
    int row, col;
    String type;
    Boolean isWhite;
    public Piece(int row, int col, String type, Boolean isWhite) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.isWhite = isWhite;
    }
}