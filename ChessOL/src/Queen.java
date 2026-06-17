public class Queen extends Piece {
    public Queen(int row, int col, boolean isWhite) {
        super(row, col, isWhite);
    }

    @Override
    public boolean checkRule(int toRow, int toCol, Piece[][] board) {
        boolean horizontal = toRow == row;
        boolean vertical = toCol == col;
        boolean diagonal = Math.abs(toRow - row) == Math.abs(toCol - col);
        
        if ((horizontal || vertical || diagonal) && !(toRow == row && toCol == col)) {
            return isPathClear(toRow, toCol, board);
        }
        return false;
    }
}
