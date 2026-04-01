import java.util.ArrayList;

public class Player {
    String name;
    ArrayList<Piece> pieces;
    Boolean isWhite;
    public Player(String name, Boolean isWhite) {
        this.name = name;
        this.isWhite = isWhite;
        pieces = new ArrayList<Piece>();
    }

}