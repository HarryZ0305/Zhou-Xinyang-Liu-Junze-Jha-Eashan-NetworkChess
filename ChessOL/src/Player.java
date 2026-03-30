public class Player {
    String name;
    Piece[] pieces;
    Boolean isWhite;
    public Player(String name, Boolean isWhite) {
        this.name = name;
        this.isWhite = isWhite;
        pieces = new Piece[16];
    }
}