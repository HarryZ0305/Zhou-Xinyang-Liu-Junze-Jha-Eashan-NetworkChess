public class GameTest {
    private Game game;

    public static void main(String[] args) {
        GameTest runner = new GameTest();
        int passed = 0;
        int failed = 0;

        try { 
            runner.setUp(); 
            runner.testPawnMovement(); 
            passed++; 
            System.out.println("testPawnMovement: PASSED"); 
        } catch (Throwable e) { 
            failed++; 
            System.err.println("testPawnMovement: FAILED"); 
            e.printStackTrace(); 
        }

        try { 
            runner.setUp(); 
            runner.testBishopMovementAndPathClearance(); 
            passed++; 
            System.out.println("testBishopMovementAndPathClearance: PASSED"); 
        } catch (Throwable e) { 
            failed++; 
            System.err.println("testBishopMovementAndPathClearance: FAILED"); 
            e.printStackTrace(); 
        }

        try { 
            runner.setUp(); 
            runner.testKingCheckCastlingBug(); 
            passed++; 
            System.out.println("testKingCheckCastlingBug: PASSED"); 
        } catch (Throwable e) { 
            failed++; 
            System.err.println("testKingCheckCastlingBug: FAILED"); 
            e.printStackTrace(); 
        }

        try { 
            runner.setUp(); 
            runner.testCastlingSuccess(); 
            passed++; 
            System.out.println("testCastlingSuccess: PASSED"); 
        } catch (Throwable e) { 
            failed++; 
            System.err.println("testCastlingSuccess: FAILED"); 
            e.printStackTrace(); 
        }

        try { 
            runner.setUp(); 
            runner.testCastlingBlockedByCheck(); 
            passed++; 
            System.out.println("testCastlingBlockedByCheck: PASSED"); 
        } catch (Throwable e) { 
            failed++; 
            System.err.println("testCastlingBlockedByCheck: FAILED"); 
            e.printStackTrace(); 
        }

        try { 
            runner.setUp(); 
            runner.testEnPassant(); 
            passed++; 
            System.out.println("testEnPassant: PASSED"); 
        } catch (Throwable e) { 
            failed++; 
            System.err.println("testEnPassant: FAILED"); 
            e.printStackTrace(); 
        }

        try { 
            runner.setUp(); 
            runner.testDrawByFiftyMoves(); 
            passed++; 
            System.out.println("testDrawByFiftyMoves: PASSED"); 
        } catch (Throwable e) { 
            failed++; 
            System.err.println("testDrawByFiftyMoves: FAILED"); 
            e.printStackTrace(); 
        }

        try { 
            runner.setUp(); 
            runner.testDrawByRepetition(); 
            passed++; 
            System.out.println("testDrawByRepetition: PASSED"); 
        } catch (Throwable e) { 
            failed++; 
            System.err.println("testDrawByRepetition: FAILED"); 
            e.printStackTrace(); 
        }

        try { 
            runner.setUp(); 
            runner.testDrawByInsufficientMaterial(); 
            passed++; 
            System.out.println("testDrawByInsufficientMaterial: PASSED"); 
        } catch (Throwable e) { 
            failed++; 
            System.err.println("testDrawByInsufficientMaterial: FAILED"); 
            e.printStackTrace(); 
        }

        try { 
            runner.setUp(); 
            runner.testAlgebraicNotation(); 
            passed++; 
            System.out.println("testAlgebraicNotation: PASSED"); 
        } catch (Throwable e) { 
            failed++; 
            System.err.println("testAlgebraicNotation: FAILED"); 
            e.printStackTrace(); 
        }

        System.out.println("\n--- TEST SUMMARY ---");
        System.out.println("Passed: " + passed + "/" + (passed + failed));
        if (failed > 0) {
            System.err.println("SOME TESTS FAILED!");
            System.exit(1);
        } else {
            System.out.println("ALL TESTS PASSED!");
            System.exit(0);
        }
    }

    public void setUp() {
        game = new Game();
    }

    private void clearBoard() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                game.board[r][c] = null;
            }
        }
        game.whitePlayer.pieces.clear();
        game.blackPlayer.pieces.clear();
    }

    private void assertTrue(boolean val) {
        if (!val) throw new AssertionError("Expected true, got false");
    }

    private void assertFalse(boolean val) {
        if (val) throw new AssertionError("Expected false, got true");
    }

    private void assertEquals(Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private void assertNotNull(Object val) {
        if (val == null) throw new AssertionError("Expected not null");
    }

    private void assertNull(Object val) {
        if (val != null) throw new AssertionError("Expected null, got " + val);
    }

    public void testPawnMovement() {
        clearBoard();
        Pawn pawn = new Pawn(6, 4, true);
        game.board[6][4] = pawn;
        game.whitePlayer.pieces.add(pawn);

        assertTrue(game.canMove(6, 4, 5, 4, true));
        assertTrue(game.canMove(6, 4, 4, 4, true));
        assertFalse(game.canMove(6, 4, 3, 4, true));

        game.Move(6, 4, 4, 4, true);
        game.whiteTurn = false;

        assertTrue(pawn.firstMoveTwoSquares);
        assertEquals(4, pawn.row);
        assertEquals(4, pawn.col);
    }

    public void testBishopMovementAndPathClearance() {
        clearBoard();
        Bishop bishop = new Bishop(4, 4, true);
        game.board[4][4] = bishop;
        game.whitePlayer.pieces.add(bishop);

        assertTrue(game.canMove(4, 4, 2, 2, true));
        assertTrue(game.canMove(4, 4, 6, 6, true));
        assertFalse(game.canMove(4, 4, 4, 6, true));

        Pawn blocker = new Pawn(3, 3, true);
        game.board[3][3] = blocker;
        game.whitePlayer.pieces.add(blocker);

        assertFalse(game.canMove(4, 4, 2, 2, true));
        assertFalse(game.canMove(4, 4, 3, 3, true));
    }

    public void testKingCheckCastlingBug() {
        clearBoard();
        King whiteKing = new King(7, 4, true);
        King blackKing = new King(7, 6, false);
        game.board[7][4] = whiteKing;
        game.board[7][6] = blackKing;
        game.whitePlayer.pieces.add(whiteKing);
        game.blackPlayer.pieces.add(blackKing);

        assertFalse(game.isInCheck(7, 4, true));
    }

    public void testCastlingSuccess() {
        clearBoard();
        King king = new King(7, 4, true);
        Rook rook = new Rook(7, 7, true);
        game.board[7][4] = king;
        game.board[7][7] = rook;
        game.whitePlayer.pieces.add(king);
        game.whitePlayer.pieces.add(rook);

        assertTrue(game.canMove(7, 4, 7, 6, true));
        
        game.Move(7, 4, 7, 6, true);
        assertEquals(7, game.board[7][6].row);
        assertEquals(6, game.board[7][6].col);
        assertNotNull(game.board[7][5]);
        assertTrue(game.board[7][5] instanceof Rook);
        assertNull(game.board[7][7]);
    }

    public void testCastlingBlockedByCheck() {
        clearBoard();
        King king = new King(7, 4, true);
        Rook rook = new Rook(7, 7, true);
        Rook enemyRook = new Rook(0, 5, false);
        game.board[7][4] = king;
        game.board[7][7] = rook;
        game.board[0][5] = enemyRook;
        game.whitePlayer.pieces.add(king);
        game.whitePlayer.pieces.add(rook);
        game.blackPlayer.pieces.add(enemyRook);

        assertFalse(game.canMove(7, 4, 7, 6, true));
    }

    public void testEnPassant() {
        clearBoard();
        Pawn whitePawn = new Pawn(4, 4, true);
        Pawn blackPawn = new Pawn(4, 3, false);
        blackPawn.firstMoveTwoSquares = true;

        game.board[4][4] = whitePawn;
        game.board[4][3] = blackPawn;
        game.whitePlayer.pieces.add(whitePawn);
        game.blackPlayer.pieces.add(blackPawn);

        assertTrue(game.canMove(4, 4, 3, 3, true));

        game.Move(4, 4, 3, 3, true);
        assertNull(game.board[4][3]);
        assertEquals(whitePawn, game.board[3][3]);
    }

    public void testDrawByFiftyMoves() {
        assertEquals(0, game.halfMoveClock);

        clearBoard();
        King wk = new King(7, 4, true);
        King bk = new King(0, 4, false);
        Pawn wp = new Pawn(6, 0, true);
        Pawn bp = new Pawn(1, 0, false);
        game.board[7][4] = wk;
        game.board[0][4] = bk;
        game.board[6][0] = wp;
        game.board[1][0] = bp;
        game.whitePlayer.pieces.add(wk);
        game.whitePlayer.pieces.add(wp);
        game.blackPlayer.pieces.add(bk);
        game.blackPlayer.pieces.add(bp);

        game.Move(7, 4, 7, 5, true);
        assertEquals(1, game.halfMoveClock);

        game.halfMoveClock = 100;
        assertEquals("FiftyMoves", game.getDrawReason());
    }

    public void testDrawByRepetition() {
        clearBoard();
        King wk = new King(7, 4, true);
        King bk = new King(0, 4, false);
        Pawn wp = new Pawn(6, 0, true);
        Pawn bp = new Pawn(1, 0, false);
        game.board[7][4] = wk;
        game.board[0][4] = bk;
        game.board[6][0] = wp;
        game.board[1][0] = bp;
        game.whitePlayer.pieces.add(wk);
        game.whitePlayer.pieces.add(wp);
        game.blackPlayer.pieces.add(bk);
        game.blackPlayer.pieces.add(bp);

        String sig = game.stateSignature();
        game.positionHistory.add(sig);
        game.positionHistory.add(sig);
        game.positionHistory.add(sig);

        assertEquals("Repetition", game.getDrawReason());
    }

    public void testDrawByInsufficientMaterial() {
        clearBoard();
        King wk = new King(7, 4, true);
        King bk = new King(0, 4, false);
        game.board[7][4] = wk;
        game.board[0][4] = bk;
        game.whitePlayer.pieces.add(wk);
        game.blackPlayer.pieces.add(bk);

        assertTrue(game.isInsufficientMaterial());

        Bishop bishop = new Bishop(7, 5, true);
        game.board[7][5] = bishop;
        game.whitePlayer.pieces.add(bishop);
        assertTrue(game.isInsufficientMaterial());
        
        game.board[7][5] = null;
        game.whitePlayer.pieces.remove(bishop);
        Knight knight = new Knight(7, 5, true);
        game.board[7][5] = knight;
        game.whitePlayer.pieces.add(knight);
        assertTrue(game.isInsufficientMaterial());

        game.board[7][5] = null;
        game.whitePlayer.pieces.remove(knight);
        
        Bishop whiteBishop = new Bishop(7, 5, true);
        Bishop blackBishop = new Bishop(0, 2, false);
        game.board[7][5] = whiteBishop;
        game.board[0][2] = blackBishop;
        game.whitePlayer.pieces.add(whiteBishop);
        game.blackPlayer.pieces.add(blackBishop);
        assertTrue(game.isInsufficientMaterial());
    }

    public void testAlgebraicNotation() {
        clearBoard();
        Pawn pawn = new Pawn(6, 4, true);
        game.board[6][4] = pawn;
        game.whitePlayer.pieces.add(pawn);

        String san1 = game.toAlgebraic(6, 4, 4, 4, true, "None");
        assertEquals("e4", san1);

        clearBoard();
        Pawn whitePawn = new Pawn(4, 4, true);
        Pawn blackPawn = new Pawn(3, 3, false);
        game.board[4][4] = whitePawn;
        game.board[3][3] = blackPawn;
        game.whitePlayer.pieces.add(whitePawn);
        game.blackPlayer.pieces.add(blackPawn);

        String san2 = game.toAlgebraic(4, 4, 3, 3, true, "None");
        assertEquals("exd5", san2);
    }
}
