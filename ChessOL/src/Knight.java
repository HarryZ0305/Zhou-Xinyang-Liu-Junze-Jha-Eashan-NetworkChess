public class Knight extends Piece {
    public Knight(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean checkRule(int toRow, int toCol, Piece[][] board) {
        if(toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
        	return false;
        }
        int dr = Math.abs(toRow - row);
        int dc = Math.abs(toCol - col);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }
}
