class Chess {
    constructor(src) {
        if (src) {
            // Deep copy constructor for AI search tree simulations
            this.board = Array.from({ length: 8 }, () => Array(8).fill(null));
            this.whitePieces = [];
            this.blackPieces = [];
            for (let r = 0; r < 8; r++) {
                for (let c = 0; c < 8; c++) {
                    const p = src.board[r][c];
                    if (p) {
                        const np = {
                            type: p.type,
                            isWhite: p.isWhite,
                            moved: p.moved,
                            row: p.row,
                            col: p.col
                        };
                        if (p.type === 'Pawn') {
                            np.firstMoveTwoSquares = p.firstMoveTwoSquares;
                        }
                        if (p.type === 'Rook') {
                            np.isOriginalRook = p.isOriginalRook;
                        }
                        this.board[r][c] = np;
                        if (np.isWhite) this.whitePieces.push(np);
                        else this.blackPieces.push(np);
                    }
                }
            }
            this.whiteTurn = src.whiteTurn;
            this.halfMoveClock = src.halfMoveClock;
            this.positionHistory = [...src.positionHistory];
            this.lastMoveFromRow = src.lastMoveFromRow;
            this.lastMoveFromCol = src.lastMoveFromCol;
            this.lastMoveToRow = src.lastMoveToRow;
            this.lastMoveToCol = src.lastMoveToCol;
            this.sanMoves = [...src.sanMoves];
            
            // Re-clone captured pieces to preserve lists
            this.capturedWhite = src.capturedWhite.map(p => ({ ...p }));
            this.capturedBlack = src.capturedBlack.map(p => ({ ...p }));
        } else {
            // Standard fresh game constructor
            this.board = Array.from({ length: 8 }, () => Array(8).fill(null));
            this.whitePieces = [];
            this.blackPieces = [];
            this.capturedWhite = [];
            this.capturedBlack = [];
            this.halfMoveClock = 0;
            this.positionHistory = [];
            this.lastMoveFromRow = -1;
            this.lastMoveFromCol = -1;
            this.lastMoveToRow = -1;
            this.lastMoveToCol = -1;
            this.sanMoves = [];
            this.whiteTurn = true;
            this.initBoard();
        }
    }

    initBoard() {
        // Place Pawns
        for (let i = 0; i < 8; i++) {
            this.board[1][i] = { type: 'Pawn', row: 1, col: i, isWhite: false, moved: false, firstMoveTwoSquares: false };
            this.board[6][i] = { type: 'Pawn', row: 6, col: i, isWhite: true, moved: false, firstMoveTwoSquares: false };
        }
        // Place Rooks
        this.board[0][0] = { type: 'Rook', row: 0, col: 0, isWhite: false, moved: false, isOriginalRook: true };
        this.board[0][7] = { type: 'Rook', row: 0, col: 7, isWhite: false, moved: false, isOriginalRook: true };
        this.board[7][0] = { type: 'Rook', row: 7, col: 0, isWhite: true, moved: false, isOriginalRook: true };
        this.board[7][7] = { type: 'Rook', row: 7, col: 7, isWhite: true, moved: false, isOriginalRook: true };
        // Place Knights
        this.board[0][1] = { type: 'Knight', row: 0, col: 1, isWhite: false, moved: false };
        this.board[0][6] = { type: 'Knight', row: 0, col: 6, isWhite: false, moved: false };
        this.board[7][1] = { type: 'Knight', row: 7, col: 1, isWhite: true, moved: false };
        this.board[7][6] = { type: 'Knight', row: 7, col: 6, isWhite: true, moved: false };
        // Place Bishops
        this.board[0][2] = { type: 'Bishop', row: 0, col: 2, isWhite: false, moved: false };
        this.board[0][5] = { type: 'Bishop', row: 0, col: 5, isWhite: false, moved: false };
        this.board[7][2] = { type: 'Bishop', row: 7, col: 2, isWhite: true, moved: false };
        this.board[7][5] = { type: 'Bishop', row: 7, col: 5, isWhite: true, moved: false };
        // Place Queens
        this.board[0][3] = { type: 'Queen', row: 0, col: 3, isWhite: false, moved: false };
        this.board[7][3] = { type: 'Queen', row: 7, col: 3, isWhite: true, moved: false };
        // Place Kings
        this.board[0][4] = { type: 'King', row: 0, col: 4, isWhite: false, moved: false };
        this.board[7][4] = { type: 'King', row: 7, col: 4, isWhite: true, moved: false };

        // Populate pieces arrays
        for (let r = 0; r < 8; r++) {
            for (let c = 0; c < 8; c++) {
                const p = this.board[r][c];
                if (p) {
                    if (p.isWhite) this.whitePieces.push(p);
                    else this.blackPieces.push(p);
                }
            }
        }

        this.positionHistory.push(this.stateSignature());
    }

    isPathClear(fromRow, fromCol, toRow, toCol) {
        const rDir = Math.sign(toRow - fromRow);
        const cDir = Math.sign(toCol - fromCol);
        
        let rCheck = fromRow + rDir;
        let cCheck = fromCol + cDir;
        
        while (rCheck !== toRow || cCheck !== toCol) {
            if (this.board[rCheck][cCheck] !== null) {
                return false;
            }
            rCheck += rDir;
            cCheck += cDir;
        }
        return true;
    }

    checkPieceRule(piece, toRow, toCol) {
        const row = piece.row;
        const col = piece.col;
        const isWhite = piece.isWhite;

        switch (piece.type) {
            case 'Pawn': {
                const direction = isWhite ? -1 : 1;
                // Moving straight
                if (col === toCol) {
                    if (toRow === row + direction) {
                        return this.board[toRow][toCol] === null;
                    }
                    if (!piece.moved && toRow === row + direction * 2) {
                        return this.board[row + direction][toCol] === null && this.board[toRow][toCol] === null;
                    }
                    return false;
                }
                // Diagonal capture
                if (Math.abs(toCol - col) === 1 && toRow === row + direction) {
                    const target = this.board[toRow][toCol];
                    if (target !== null && target.isWhite !== isWhite) {
                        return true;
                    }
                    // En Passant
                    const sameRowPiece = this.board[row][toCol];
                    if (target === null && sameRowPiece && sameRowPiece.type === 'Pawn' && sameRowPiece.isWhite !== isWhite) {
                        if (sameRowPiece.firstMoveTwoSquares) {
                            return true;
                        }
                    }
                }
                return false;
            }
            case 'Knight': {
                const rDiff = Math.abs(toRow - row);
                const cDiff = Math.abs(toCol - col);
                return (rDiff === 2 && cDiff === 1) || (rDiff === 1 && cDiff === 2);
            }
            case 'Bishop': {
                if (Math.abs(toRow - row) !== Math.abs(toCol - col)) {
                    return false;
                }
                return this.isPathClear(row, col, toRow, toCol);
            }
            case 'Rook': {
                if (toRow !== row && toCol !== col) {
                    return false;
                }
                return this.isPathClear(row, col, toRow, toCol);
            }
            case 'Queen': {
                const linear = (toRow === row || toCol === col);
                const diagonal = (Math.abs(toRow - row) === Math.abs(toCol - col));
                if (!linear && !diagonal) {
                    return false;
                }
                return this.isPathClear(row, col, toRow, toCol);
            }
            case 'King': {
                if (!piece.moved) {
                    // Castling
                    if (toRow === row && Math.abs(toCol - col) === 2) {
                        const rookCol = toCol === col + 2 ? 7 : 0;
                        const rook = this.board[row][rookCol];
                        if (rook && rook.type === 'Rook' && rook.isOriginalRook && rook.isWhite === isWhite && !rook.moved) {
                            const step = toCol > col ? 1 : -1;
                            for (let c = col + step; c !== rookCol; c += step) {
                                if (this.board[row][c] !== null) {
                                    return false;
                                }
                            }
                            return true;
                        }
                    }
                }
                return Math.abs(toRow - row) <= 1 && Math.abs(toCol - col) <= 1;
            }
            default:
                return false;
        }
    }

    canMove(fromRow, fromCol, toRow, toCol, isWhite) {
        if (isWhite !== this.whiteTurn) {
            return false;
        }
        if (toRow === fromRow && toCol === fromCol) {
            return false;
        }
        if (toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
            return false;
        }
        if (fromRow < 0 || fromRow > 7 || fromCol < 0 || fromCol > 7) {
            return false;
        }
        const piece = this.board[fromRow][fromCol];
        if (piece === null) {
            return false;
        }
        const target = this.board[toRow][toCol];
        if (target === null || target.isWhite !== piece.isWhite) {
            if (isWhite === piece.isWhite) {
                if (this.checkPieceRule(piece, toRow, toCol)) {
                    // Castling: king cannot start in check or pass through an attacked square
                    if (piece.type === 'King' && Math.abs(toCol - fromCol) === 2) {
                        const step = toCol > fromCol ? 1 : -1;
                        for (let c = fromCol; c !== toCol; c += step) {
                            if (this.isInCheckCoords(fromRow, c, isWhite)) {
                                return false;
                            }
                        }
                    }

                    // Simulate move for King safety
                    const tempTarget = this.board[toRow][toCol];
                    const isEnPassant = piece.type === 'Pawn' && fromCol !== toCol && tempTarget === null;
                    const epCaptured = isEnPassant ? this.board[fromRow][toCol] : null;

                    // Apply hypothetical move
                    this.board[toRow][toCol] = piece;
                    this.board[fromRow][fromCol] = null;
                    if (isEnPassant) {
                        this.board[fromRow][toCol] = null;
                    }
                    const enemyPieces = isWhite ? this.blackPieces : this.whitePieces;
                    if (tempTarget !== null) {
                        const idx = enemyPieces.indexOf(tempTarget);
                        if (idx !== -1) enemyPieces.splice(idx, 1);
                    }
                    if (epCaptured !== null) {
                        const idx = enemyPieces.indexOf(epCaptured);
                        if (idx !== -1) enemyPieces.splice(idx, 1);
                    }

                    // Locate king
                    let kingRow, kingCol;
                    if (piece.type === 'King') {
                        kingRow = toRow;
                        kingCol = toCol;
                    } else {
                        const king = this.getKing(isWhite);
                        if (king === null) {
                            kingRow = -1;
                            kingCol = -1;
                        } else {
                            kingRow = king.row;
                            kingCol = king.col;
                        }
                    }

                    let putsKingInCheck = false;
                    if (kingRow !== -1) {
                        putsKingInCheck = this.isInCheckCoords(kingRow, kingCol, isWhite);
                    }

                    // Revert hypothetical move
                    this.board[toRow][toCol] = tempTarget;
                    this.board[fromRow][fromCol] = piece;
                    if (isEnPassant) {
                        this.board[fromRow][toCol] = epCaptured;
                    }
                    if (tempTarget !== null) {
                        enemyPieces.push(tempTarget);
                    }
                    if (epCaptured !== null) {
                        enemyPieces.push(epCaptured);
                    }

                    if (putsKingInCheck) {
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    isInCheckCoords(row, col, isWhite) {
        const enemy = !isWhite ? this.whitePieces : this.blackPieces;
        for (const p of enemy) {
            if (p.type === 'Pawn') {
                const direction = p.isWhite ? -1 : 1;
                if (row === p.row + direction && Math.abs(col - p.col) === 1) {
                    return true;
                }
            } else if (p.type === 'King') {
                if (Math.abs(row - p.row) <= 1 && Math.abs(col - p.col) <= 1) {
                    return true;
                }
            } else if (this.checkPieceRule(p, row, col)) {
                return true;
            }
        }
        return false;
    }

    isInCheck(isWhite) {
        const king = this.getKing(isWhite);
        if (king === null) return false;
        return this.isInCheckCoords(king.row, king.col, isWhite);
    }

    getKing(isWhite) {
        const list = isWhite ? this.whitePieces : this.blackPieces;
        for (const p of list) {
            if (p.type === 'King') {
                return p;
            }
        }
        return null;
    }

    Move(fromRow, fromCol, toRow, toCol, isWhite) {
        const movingPiece = this.board[fromRow][fromCol];
        if (movingPiece !== null) {
            const isCapture = (this.board[toRow][toCol] !== null) || 
                              (movingPiece.type === 'Pawn' && fromCol !== toCol);
            if (movingPiece.type === 'Pawn' || isCapture) {
                this.halfMoveClock = 0;
            } else {
                this.halfMoveClock++;
            }
        }
        
        this.lastMoveFromRow = fromRow;
        this.lastMoveFromCol = fromCol;
        this.lastMoveToRow = toRow;
        this.lastMoveToCol = toCol;

        // Castling logic
        if (Math.abs(toCol - fromCol) === 2 && movingPiece && movingPiece.type === 'King' && !movingPiece.moved) {
            const rookCol = toCol > fromCol ? 7 : 0;
            const rook = this.board[fromRow][rookCol];
            if (rook && rook.type === 'Rook' && rook.isOriginalRook && rook.isWhite === isWhite && !rook.moved) {
                const newRookCol = toCol > fromCol ? toCol - 1 : toCol + 1;
                this.board[fromRow][newRookCol] = rook;
                this.board[fromRow][rookCol] = null;
                rook.col = newRookCol;
                rook.moved = true;
            }
        }

        // Clear double-step en-passant flag on pawns
        for (let r = 0; r < 8; r++) {
            for (let c = 0; c < 8; c++) {
                const p = this.board[r][c];
                if (p && p.type === 'Pawn') {
                    p.firstMoveTwoSquares = false;
                }
            }
        }

        if (movingPiece && movingPiece.type === 'Pawn' && Math.abs(toRow - fromRow) === 2) {
            movingPiece.firstMoveTwoSquares = true;
        }

        // Capture routing
        if (movingPiece && movingPiece.type === 'Pawn' && fromCol !== toCol) {
            if (this.board[toRow][toCol] === null) {
                this.capture(fromRow, toCol, isWhite);
                this.board[fromRow][toCol] = null;
            } else {
                this.capture(toRow, toCol, isWhite);
            }
        } else if (this.board[toRow][toCol] !== null) {
            this.capture(toRow, toCol, isWhite);
        }

        // Move piece
        this.board[toRow][toCol] = movingPiece;
        this.board[fromRow][fromCol] = null;
        if (movingPiece) {
            movingPiece.row = toRow;
            movingPiece.col = toCol;
            movingPiece.moved = true;
        }
    }

    capture(toRow, toCol, isWhite) {
        const captured = this.board[toRow][toCol];
        if (captured) {
            if (isWhite) {
                const idx = this.blackPieces.indexOf(captured);
                if (idx !== -1) this.blackPieces.splice(idx, 1);
                this.capturedBlack.push(captured);
            } else {
                const idx = this.whitePieces.indexOf(captured);
                if (idx !== -1) this.whitePieces.splice(idx, 1);
                this.capturedWhite.push(captured);
            }
        }
        return captured;
    }

    hasLegalMove(isWhite) {
        const list = isWhite ? this.whitePieces : this.blackPieces;
        const snapshot = [...list];
        for (const p of snapshot) {
            for (let r = 0; r < 8; r++) {
                for (let c = 0; c < 8; c++) {
                    if (this.canMove(p.row, p.col, r, c, isWhite)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    promotion(row, col, isWhite, promo) {
        const playerPieces = isWhite ? this.whitePieces : this.blackPieces;
        // Remove the original pawn
        for (let i = 0; i < playerPieces.length; i++) {
            const p = playerPieces[i];
            if (p.row === row && p.col === col) {
                playerPieces.splice(i, 1);
                break;
            }
        }
        let newPiece;
        switch (promo) {
            case 'Q':
                newPiece = { type: 'Queen', row, col, isWhite, moved: true };
                break;
            case 'R':
                newPiece = { type: 'Rook', row, col, isWhite, moved: true, isOriginalRook: false };
                break;
            case 'B':
                newPiece = { type: 'Bishop', row, col, isWhite, moved: true };
                break;
            case 'N':
                newPiece = { type: 'Knight', row, col, isWhite, moved: true };
                break;
        }
        this.board[row][col] = newPiece;
        playerPieces.push(newPiece);
    }

    stateSignature() {
        let sb = "";
        for (let r = 0; r < 8; r++) {
            for (let c = 0; c < 8; c++) {
                const p = this.board[r][c];
                if (p === null) {
                    sb += '.';
                    continue;
                }
                let ch;
                switch (p.type) {
                    case 'Pawn':   ch = 'p'; break;
                    case 'Knight': ch = 'n'; break;
                    case 'Bishop': ch = 'b'; break;
                    case 'Rook':   ch = 'r'; break;
                    case 'Queen':  ch = 'q'; break;
                    case 'King':   ch = 'k'; break;
                    default:       ch = '?'; break;
                }
                sb += p.isWhite ? ch.toUpperCase() : ch;
                if ((p.type === 'King' || p.type === 'Rook') && !p.moved) sb += '*';
                if (p.type === 'Pawn' && p.firstMoveTwoSquares) sb += '!';
            }
        }
        sb += this.whiteTurn ? 'w' : 'b';
        return sb;
    }

    recordState() {
        this.positionHistory.push(this.stateSignature());
    }

    isInsufficientMaterial() {
        let whiteKnights = 0, blackKnights = 0;
        let whiteBishops = 0, blackBishops = 0;
        let otherPieces = 0;
        
        let whiteBishopSquareColor = -1;
        let blackBishopSquareColor = -1;
        
        for (let r = 0; r < 8; r++) {
            for (let c = 0; c < 8; c++) {
                const p = this.board[r][c];
                if (p === null) continue;
                const type = p.type;
                if (type === 'King') continue;
                
                if (p.isWhite) {
                    if (type === 'Knight') {
                        whiteKnights++;
                    } else if (type === 'Bishop') {
                        whiteBishops++;
                        whiteBishopSquareColor = (r + c) % 2;
                    } else {
                        otherPieces++;
                    }
                } else {
                    if (type === 'Knight') {
                        blackKnights++;
                    } else if (type === 'Bishop') {
                        blackBishops++;
                        blackBishopSquareColor = (r + c) % 2;
                    } else {
                        otherPieces++;
                    }
                }
            }
        }
        
        if (otherPieces > 0) return false;
        
        if (whiteKnights === 0 && blackKnights === 0 && whiteBishops === 0 && blackBishops === 0) {
            return true;
        }
        
        if (whiteBishops === 1 && whiteKnights === 0 && blackBishops === 0 && blackKnights === 0) {
            return true;
        }
        if (blackBishops === 1 && blackKnights === 0 && whiteBishops === 0 && whiteKnights === 0) {
            return true;
        }
        
        if (whiteKnights === 1 && whiteBishops === 0 && blackBishops === 0 && blackKnights === 0) {
            return true;
        }
        if (blackKnights === 1 && blackBishops === 0 && whiteBishops === 0 && whiteKnights === 0) {
            return true;
        }
        
        if (whiteBishops === 1 && blackBishops === 1 && whiteKnights === 0 && blackKnights === 0) {
            return whiteBishopSquareColor === blackBishopSquareColor;
        }
        
        return false;
    }

    getDrawReason() {
        if (this.isInsufficientMaterial()) {
            return "InsufficientMaterial";
        }
        if (this.halfMoveClock >= 100) {
            return "FiftyMoves";
        }
        if (this.positionHistory.length >= 3) {
            const currentSig = this.stateSignature();
            let count = 0;
            for (const sig of this.positionHistory) {
                if (sig === currentSig) {
                    count++;
                }
            }
            if (count >= 3) {
                return "Repetition";
            }
        }
        return null;
    }

    formatMove(san) {
        if (this.sanMoves.length % 2 === 0) {
            return (Math.floor(this.sanMoves.length / 2) + 1) + ". " + san;
        } else {
            return (Math.floor(this.sanMoves.length / 2) + 1) + "... " + san;
        }
    }

    getPieceLetter(type) {
        switch (type) {
            case 'Knight': return "N";
            case 'Bishop': return "B";
            case 'Rook':   return "R";
            case 'Queen':  return "Q";
            case 'King':   return "K";
            default:       return "";
        }
    }

    toAlgebraic(fromRow, fromCol, toRow, toCol, isWhite, promotion) {
        const piece = this.board[fromRow][fromCol];
        if (piece === null) return "";

        if (piece.type === 'King' && Math.abs(toCol - fromCol) === 2) {
            return toCol > fromCol ? "O-O" : "O-O-O";
        }

        let sb = "";
        const type = piece.type;
        if (type === 'Pawn') {
            if (fromCol !== toCol) {
                sb += String.fromCharCode(97 + fromCol);
            }
        } else {
            sb += this.getPieceLetter(type);
            
            let sameFile = false;
            let sameRank = false;
            let ambiguous = false;
            
            const friendlyPieces = isWhite ? this.whitePieces : this.blackPieces;
            for (const p of friendlyPieces) {
                if (p !== piece && p.type === type) {
                    const savedTurn = this.whiteTurn;
                    this.whiteTurn = isWhite;
                    const canMoveResult = this.canMove(p.row, p.col, toRow, toCol, isWhite);
                    this.whiteTurn = savedTurn;
                    if (canMoveResult) {
                        ambiguous = true;
                        if (p.col === fromCol) {
                            sameFile = true;
                        }
                        if (p.row === fromRow) {
                            sameRank = true;
                        }
                    }
                }
            }
            
            if (ambiguous) {
                if (!sameFile) {
                    sb += String.fromCharCode(97 + fromCol);
                } else if (!sameRank) {
                    sb += String.fromCharCode(56 - fromRow);
                } else {
                    sb += String.fromCharCode(97 + fromCol);
                    sb += String.fromCharCode(56 - fromRow);
                }
            }
        }

        const isCapture = (this.board[toRow][toCol] !== null) || 
                          (type === 'Pawn' && fromCol !== toCol);
        if (isCapture) {
            if (type === 'Pawn' && fromCol === toCol) {
                // no-op
            } else {
                sb += 'x';
            }
        }

        sb += String.fromCharCode(97 + toCol);
        sb += String.fromCharCode(56 - toRow);

        if (promotion && promotion !== "None" && promotion !== "") {
            sb += '=' + promotion;
        }

        // Simulate move to check if opponent is in check/mate
        const tempTarget = this.board[toRow][toCol];
        const isEnPassant = piece.type === 'Pawn' && fromCol !== toCol && tempTarget === null;
        const epCaptured = isEnPassant ? this.board[fromRow][toCol] : null;

        // Save original coordinates of the moving piece
        const origRow = piece.row;
        const origCol = piece.col;
        const origMoved = piece.moved;

        // Temporarily update piece coordinates
        piece.row = toRow;
        piece.col = toCol;
        piece.moved = true;

        this.board[toRow][toCol] = piece;
        this.board[fromRow][fromCol] = null;
        if (isEnPassant) {
            this.board[fromRow][toCol] = null;
        }
        
        const enemyPieces = isWhite ? this.blackPieces : this.whitePieces;
        if (tempTarget !== null) {
            const idx = enemyPieces.indexOf(tempTarget);
            if (idx !== -1) enemyPieces.splice(idx, 1);
        }
        if (epCaptured !== null) {
            const idx = enemyPieces.indexOf(epCaptured);
            if (idx !== -1) enemyPieces.splice(idx, 1);
        }

        const friendlyPieces = isWhite ? this.whitePieces : this.blackPieces;
        let pawnIdx = -1;
        let promotedPiece = null;

        if (promotion && promotion !== "None" && promotion !== "") {
            if (promotion === "Q") promotedPiece = { type: 'Queen', row: toRow, col: toCol, isWhite, moved: true };
            else if (promotion === "R") promotedPiece = { type: 'Rook', row: toRow, col: toCol, isWhite, moved: true, isOriginalRook: false };
            else if (promotion === "B") promotedPiece = { type: 'Bishop', row: toRow, col: toCol, isWhite, moved: true };
            else if (promotion === "N") promotedPiece = { type: 'Knight', row: toRow, col: toCol, isWhite, moved: true };
            
            this.board[toRow][toCol] = promotedPiece;

            pawnIdx = friendlyPieces.indexOf(piece);
            if (pawnIdx !== -1) {
                friendlyPieces[pawnIdx] = promotedPiece;
            }
        }

        const opponentInCheck = this.isInCheck(!isWhite);
        
        const oldTurn = this.whiteTurn;
        this.whiteTurn = !isWhite;
        const opponentHasMoves = this.hasLegalMove(!isWhite);
        this.whiteTurn = oldTurn;

        // Restore friendly pieces and board
        if (promotedPiece !== null && pawnIdx !== -1) {
            friendlyPieces[pawnIdx] = piece;
        }

        piece.row = origRow;
        piece.col = origCol;
        piece.moved = origMoved;

        this.board[toRow][toCol] = tempTarget;
        this.board[fromRow][fromCol] = piece;
        if (isEnPassant) {
            this.board[fromRow][toCol] = epCaptured;
        }
        if (tempTarget !== null) {
            enemyPieces.push(tempTarget);
        }
        if (epCaptured !== null) {
            enemyPieces.push(epCaptured);
        }

        if (opponentInCheck) {
            if (!opponentHasMoves) {
                sb += '#';
            } else {
                sb += '+';
            }
        }

        return sb;
    }

    exportToPGN(whiteName = "White", blackName = "Black") {
        let sb = "";
        sb += '[Event "ChessOL Match"]\n';
        sb += '[Site "Network/Local"]\n';
        const date = new Date().toISOString().slice(0,10).replace(/-/g, '.');
        sb += `[Date "${date}"]\n`;
        sb += '[Round "1"]\n';
        sb += `[White "${whiteName}"]\n`;
        sb += `[Black "${blackName}"]\n`;
        
        let result = "*";
        const blackMated = this.isInCheck(false) && !this.hasLegalMove(false);
        const whiteMated = this.isInCheck(true) && !this.hasLegalMove(true);
        const draw = this.getDrawReason() !== null || (!this.isInCheck(true) && !this.hasLegalMove(true)) || (!this.isInCheck(false) && !this.hasLegalMove(false));
        if (blackMated) result = "1-0";
        else if (whiteMated) result = "0-1";
        else if (draw) result = "1/2-1/2";
        sb += `[Result "${result}"]\n\n`;

        for (let i = 0; i < this.sanMoves.length; i++) {
            if (i % 2 === 0) {
                sb += (Math.floor(i / 2) + 1) + ". ";
            }
            sb += this.sanMoves[i] + " ";
        }
        sb += result + "\n";
        return sb;
    }
}
