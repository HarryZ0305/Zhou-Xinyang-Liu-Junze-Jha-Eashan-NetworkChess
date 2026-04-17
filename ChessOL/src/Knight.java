public class Knight extends Piece {
    public Knight(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean checkRule(int toRow, int toCol, Piece[][] board) {
        
    	int rDir = Math.abs(toRow - row);
        int cDir = Math.abs(toCol - col);
        
        return (rDir == 2 && cDir == 1) || (rDir == 1 && cDir == 2);
    }
}
