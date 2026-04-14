public class Bishop extends Piece {
    public Bishop(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean checkRule(int toRow, int toCol, Piece[][] board) {
        if(toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
        	return false;
        }
        return Math.abs(toRow - row) == Math.abs(toCol - col); // diagonal
    }
}
