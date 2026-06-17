class ChessBot {
    static VAL_PAWN = 100;
    static VAL_KNIGHT = 320;
    static VAL_BISHOP = 330;
    static VAL_ROOK = 500;
    static VAL_QUEEN = 900;
    static VAL_KING = 20000;

    static MATE_SCORE = 1000000;
    static INF = 10000000;
    static SEARCH_DEPTH = 3;

    static PST_PAWN = [
        [  0,  0,  0,  0,  0,  0,  0,  0],
        [ 50, 50, 50, 50, 50, 50, 50, 50],
        [ 10, 10, 20, 30, 30, 20, 10, 10],
        [  5,  5, 10, 25, 25, 10,  5,  5],
        [  0,  0,  0, 20, 20,  0,  0,  0],
        [  5, -5,-10,  0,  0,-10, -5,  5],
        [  5, 10, 10,-20,-20, 10, 10,  5],
        [  0,  0,  0,  0,  0,  0,  0,  0]
    ];

    static PST_KNIGHT = [
        [-50,-40,-30,-30,-30,-30,-40,-50],
        [-40,-20,  0,  0,  0,  0,-20,-40],
        [-30,  0, 10, 15, 15, 10,  0,-30],
        [-30,  5, 15, 20, 20, 15,  5,-30],
        [-30,  0, 15, 20, 20, 15,  0,-30],
        [-30,  5, 10, 15, 15, 10,  5,-30],
        [-40,-20,  0,  5,  5,  0,-20,-40],
        [-50,-40,-30,-30,-30,-30,-40,-50]
    ];

    static PST_BISHOP = [
        [-20,-10,-10,-10,-10,-10,-10,-20],
        [-10,  0,  0,  0,  0,  0,  0,-10],
        [-10,  0,  5, 10, 10,  5,  0,-10],
        [-10,  5,  5, 10, 10,  5,  5,-10],
        [-10,  0, 10, 10, 10, 10,  0,-10],
        [-10, 10, 10, 10, 10, 10, 10,-10],
        [-10,  5,  0,  0,  0,  0,  5,-10],
        [-20,-10,-10,-10,-10,-10,-10,-20]
    ];

    static PST_ROOK = [
        [  0,  0,  0,  0,  0,  0,  0,  0],
        [  5, 10, 10, 10, 10, 10, 10,  5],
        [ -5,  0,  0,  0,  0,  0,  0, -5],
        [ -5,  0,  0,  0,  0,  0,  0, -5],
        [ -5,  0,  0,  0,  0,  0,  0, -5],
        [ -5,  0,  0,  0,  0,  0,  0, -5],
        [ -5,  0,  0,  0,  0,  0,  0, -5],
        [  0,  0,  0,  5,  5,  0,  0,  0]
    ];

    static PST_QUEEN = [
        [-20,-10,-10, -5, -5,-10,-10,-20],
        [-10,  0,  0,  0,  0,  0,  0,-10],
        [-10,  0,  5,  5,  5,  5,  0,-10],
        [ -5,  0,  5,  5,  5,  5,  0, -5],
        [  0,  0,  5,  5,  5,  5,  0, -5],
        [-10,  5,  5,  5,  5,  5,  0,-10],
        [-10,  0,  5,  0,  0,  0,  0,-10],
        [-20,-10,-10, -5, -5,-10,-10,-20]
    ];

    static PST_KING = [
        [-30,-40,-40,-50,-50,-40,-40,-30],
        [-30,-40,-40,-50,-50,-40,-40,-30],
        [-30,-40,-40,-50,-50,-40,-40,-30],
        [-30,-40,-40,-50,-50,-40,-40,-30],
        [-20,-30,-30,-40,-40,-30,-30,-20],
        [-10,-20,-20,-20,-20,-20,-20,-10],
        [ 20, 20,  0,  0,  0,  0, 20, 20],
        [ 20, 30, 10,  0,  0, 10, 30, 20]
    ];

    static getMove(game) {
        const side = game.whiteTurn;
        const moves = this.generateMoves(game, side);
        if (moves.length === 0) return null;

        // Shuffle so equal-scoring moves aren't deterministic
        moves.sort(() => Math.random() - 0.5);
        this.orderMoves(game, moves);

        let best = moves[0];
        let bestScore = side ? -this.INF : this.INF;
        let alpha = -this.INF;
        let beta = this.INF;

        for (const mv of moves) {
            const next = new Chess(game);
            this.applyMove(next, mv);
            const score = this.minimax(next, this.SEARCH_DEPTH - 1, alpha, beta);
            if (side) {
                if (score > bestScore) {
                    bestScore = score;
                    best = mv;
                }
                if (bestScore > alpha) alpha = bestScore;
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    best = mv;
                }
                if (bestScore < beta) beta = bestScore;
            }
        }
        return best;
    }

    static minimax(game, depth, alpha, beta) {
        if (depth === 0) return this.evaluate(game);

        const side = game.whiteTurn;
        const moves = this.generateMoves(game, side);

        if (moves.length === 0) {
            const king = game.getKing(side);
            const inCheck = king !== null && game.isInCheckCoords(king.row, king.col, side);
            if (inCheck) {
                return side ? -(this.MATE_SCORE + depth) : (this.MATE_SCORE + depth);
            }
            return 0; // stalemate
        }

        this.orderMoves(game, moves);

        if (side) {
            let max = -this.INF;
            for (const mv of moves) {
                const next = new Chess(game);
                this.applyMove(next, mv);
                const v = this.minimax(next, depth - 1, alpha, beta);
                if (v > max) max = v;
                if (max > alpha) alpha = max;
                if (beta <= alpha) break; // Beta cutoff
            }
            return max;
        } else {
            let min = this.INF;
            for (const mv of moves) {
                const next = new Chess(game);
                this.applyMove(next, mv);
                const v = this.minimax(next, depth - 1, alpha, beta);
                if (v < min) min = v;
                if (min < beta) beta = min;
                if (beta <= alpha) break; // Alpha cutoff
            }
            return min;
        }
    }

    static generateMoves(game, side) {
        const moves = [];
        const snapshot = [...(side ? game.whitePieces : game.blackPieces)];
        for (const p of snapshot) {
            for (let r = 0; r < 8; r++) {
                for (let c = 0; c < 8; c++) {
                    if (!game.canMove(p.row, p.col, r, c, side)) continue;
                    if (p.type === 'Pawn' && (r === 0 || r === 7)) {
                        moves.push([p.row, p.col, r, c, 'Q']);
                    } else {
                        moves.push([p.row, p.col, r, c, null]);
                    }
                }
            }
        }
        return moves;
    }

    static orderMoves(game, moves) {
        moves.sort((a, b) => this.captureScore(game, b) - this.captureScore(game, a));
    }

    static captureScore(game, mv) {
        const target = game.board[mv[2]][mv[3]];
        if (target === null) return 0;
        return this.pieceValue(target);
    }

    static pieceValue(p) {
        switch (p.type) {
            case 'Pawn':   return this.VAL_PAWN;
            case 'Knight': return this.VAL_KNIGHT;
            case 'Bishop': return this.VAL_BISHOP;
            case 'Rook':   return this.VAL_ROOK;
            case 'Queen':  return this.VAL_QUEEN;
            case 'King':   return this.VAL_KING;
            default: return 0;
        }
    }

    static applyMove(game, mv) {
        const side = game.whiteTurn;
        game.Move(mv[0], mv[1], mv[2], mv[3], side);
        if (mv[4]) {
            game.promotion(mv[2], mv[3], side, mv[4]);
        }
        game.whiteTurn = !game.whiteTurn;
    }

    static evaluate(game) {
        let score = 0;
        for (let r = 0; r < 8; r++) {
            for (let c = 0; c < 8; c++) {
                const p = game.board[r][c];
                if (p === null) continue;
                let mat;
                let pst;
                switch (p.type) {
                    case 'Pawn':   mat = this.VAL_PAWN;   pst = this.PST_PAWN;   break;
                    case 'Knight': mat = this.VAL_KNIGHT; pst = this.PST_KNIGHT; break;
                    case 'Bishop': mat = this.VAL_BISHOP; pst = this.PST_BISHOP; break;
                    case 'Rook':   mat = this.VAL_ROOK;   pst = this.PST_ROOK;   break;
                    case 'Queen':  mat = this.VAL_QUEEN;  pst = this.PST_QUEEN;  break;
                    case 'King':   mat = this.VAL_KING;   pst = this.PST_KING;   break;
                    default: continue;
                }
                const pstVal = p.isWhite ? pst[r][c] : pst[7 - r][c];
                const val = mat + pstVal;
                score += p.isWhite ? val : -val;
            }
        }
        return score;
    }
}
