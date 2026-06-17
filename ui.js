// State variables
let game = null;
let network = null;
let playMode = null; // 'local', 'bot', 'multiplayer'
let myColor = 'white'; // 'white' or 'black'
let localUsername = 'Player';
let opponentUsername = 'Opponent';

let selectedSquare = null;
let possibleMoves = [];

let localTimeLeft = 600;
let opponentTimeLeft = 600;
let timerInterval = null;
let gameActive = false;

let pendingPromotion = null; // { fromRow, fromCol, toRow, toCol }

function playSound(type) {
    try {
        const ctx = new (window.AudioContext || window.webkitAudioContext)();
        if (ctx.state === 'suspended') {
            ctx.resume();
        }
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);

        if (type === 'move') {
            osc.type = 'triangle';
            osc.frequency.setValueAtTime(800, ctx.currentTime);
            osc.frequency.exponentialRampToValueAtTime(100, ctx.currentTime + 0.1);
            gain.gain.setValueAtTime(0.08, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.1);
            osc.start();
            osc.stop(ctx.currentTime + 0.1);
        } else if (type === 'capture') {
            osc.type = 'sawtooth';
            osc.frequency.setValueAtTime(420, ctx.currentTime);
            osc.frequency.exponentialRampToValueAtTime(90, ctx.currentTime + 0.15);
            gain.gain.setValueAtTime(0.1, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.15);
            osc.start();
            osc.stop(ctx.currentTime + 0.15);
        } else if (type === 'check') {
            osc.type = 'sine';
            osc.frequency.setValueAtTime(620, ctx.currentTime);
            osc.frequency.setValueAtTime(820, ctx.currentTime + 0.08);
            gain.gain.setValueAtTime(0.12, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.25);
            osc.start();
            osc.stop(ctx.currentTime + 0.25);
        }
    } catch (e) {
        console.warn("Audio Context blocked or not supported: ", e);
    }
}

function getPieceImgSrc(piece) {
    if (!piece) return "";
    const colorDir = piece.isWhite ? 'WhitePieces' : 'BlackPieces';
    const colorPrefix = piece.isWhite ? 'White' : 'Black';
    return `ChessPieces/${colorDir}/${colorPrefix}${piece.type}.png`;
}

function isMyPiece(piece) {
    if (!piece) return false;
    if (playMode === 'local') {
        return piece.isWhite === game.whiteTurn;
    }
    return piece.isWhite === (myColor === 'white');
}

function isMyTurn() {
    if (playMode === 'local') return true;
    return game.whiteTurn === (myColor === 'white');
}

function selectPiece(r, c) {
    const piece = game.board[r][c];
    if (piece && isMyPiece(piece) && isMyTurn()) {
        selectedSquare = { row: r, col: c };
        possibleMoves = [];
        for (let targetR = 0; targetR < 8; targetR++) {
            for (let targetC = 0; targetC < 8; targetC++) {
                if (game.canMove(r, c, targetR, targetC, piece.isWhite)) {
                    possibleMoves.push([targetR, targetC]);
                }
            }
        }
    } else {
        selectedSquare = null;
        possibleMoves = [];
    }
}

function handleSquareClick(r, c) {
    if (!gameActive) return;

    if (selectedSquare && possibleMoves.some(m => m[0] === r && m[1] === c)) {
        tryMove(selectedSquare.row, selectedSquare.col, r, c);
        selectedSquare = null;
        possibleMoves = [];
        renderBoard();
        return;
    }

    const piece = game.board[r][c];
    if (piece && isMyPiece(piece) && isMyTurn()) {
        selectPiece(r, c);
    } else {
        selectedSquare = null;
        possibleMoves = [];
    }
    renderBoard();
}

function tryMove(fromRow, fromCol, toRow, toCol) {
    const piece = game.board[fromRow][fromCol];
    if (!piece) return;

    if (piece.type === 'Pawn' && (toRow === 0 || toRow === 7)) {
        pendingPromotion = { fromRow, fromCol, toRow, toCol };
        showPromotionModal(piece.isWhite);
    } else {
        executeMove(fromRow, fromCol, toRow, toCol, null);
    }
}

function showPromotionModal(isWhite) {
    const colorDir = isWhite ? 'WhitePieces' : 'BlackPieces';
    const colorPrefix = isWhite ? 'White' : 'Black';

    document.getElementById('promo-img-Q').src = `ChessPieces/${colorDir}/${colorPrefix}Queen.png`;
    document.getElementById('promo-img-R').src = `ChessPieces/${colorDir}/${colorPrefix}Rook.png`;
    document.getElementById('promo-img-B').src = `ChessPieces/${colorDir}/${colorPrefix}Bishop.png`;
    document.getElementById('promo-img-N').src = `ChessPieces/${colorDir}/${colorPrefix}Knight.png`;

    document.getElementById('promotion-modal').classList.remove('hidden');
}

function executeMove(fromRow, fromCol, toRow, toCol, promo = null) {
    const isWhite = game.board[fromRow][fromCol].isWhite;
    const san = game.toAlgebraic(fromRow, fromCol, toRow, toCol, isWhite, promo);
    const isCapture = (game.board[toRow][toCol] !== null) || 
                      (game.board[fromRow][fromCol].type === 'Pawn' && fromCol !== toCol);

    game.Move(fromRow, fromCol, toRow, toCol, isWhite);
    if (promo) {
        game.promotion(toRow, toCol, isWhite, promo);
    }
    
    game.whiteTurn = !game.whiteTurn;
    game.recordState();
    game.sanMoves.push(san);

    if (game.isInCheck(game.whiteTurn)) {
        playSound('check');
        addLogEntry('System', `Check! ${game.whiteTurn ? 'White' : 'Black'} king is in check.`, 'check');
    } else if (isCapture) {
        playSound('capture');
    } else {
        playSound('move');
    }

    const playerLabel = isWhite ? 'White' : 'Black';
    const formatted = game.formatMove(san);
    addLogEntry(playerLabel, formatted, isWhite ? 'local' : 'opponent');

    renderBoard();
    updateCapturedLists();
    checkGameEnd();

    // Send the move if multiplayer match
    if (playMode === 'multiplayer' && isWhite === (myColor === 'white')) {
        network.send({
            type: 'MOVE',
            move: { fromRow, fromCol, toRow, toCol, promotion: promo }
        });
    }

    // Trigger Bot Move if playing against Chess Bot
    if (playMode === 'bot' && !game.whiteTurn && gameActive) {
        setTimeout(triggerBotMove, 500);
    }
}

function triggerBotMove() {
    if (!gameActive || game.whiteTurn) return;
    const mv = ChessBot.getMove(game);
    if (mv) {
        executeMove(mv[0], mv[1], mv[2], mv[3], mv[4]);
    } else {
        checkGameEnd();
    }
}

function renderBoard() {
    const boardEl = document.getElementById('chess-board');
    boardEl.innerHTML = '';

    const kingInCheck = game.isInCheck(game.whiteTurn);
    const checkedKing = kingInCheck ? game.getKing(game.whiteTurn) : null;

    for (let displayRow = 0; displayRow < 8; displayRow++) {
        for (let displayCol = 0; displayCol < 8; displayCol++) {
            const r = (myColor === 'black') ? 7 - displayRow : displayRow;
            const c = (myColor === 'black') ? 7 - displayCol : displayCol;

            const squareEl = document.createElement('div');
            const isDark = (r + c) % 2 === 1;
            squareEl.className = `square ${isDark ? 'dark' : 'light'}`;
            squareEl.dataset.row = r;
            squareEl.dataset.col = c;

            // Highlight last move
            if ((r === game.lastMoveFromRow && c === game.lastMoveFromCol) || 
                (r === game.lastMoveToRow && c === game.lastMoveToCol)) {
                squareEl.classList.add('last-move');
            }

            // Highlight selected piece
            if (selectedSquare && selectedSquare.row === r && selectedSquare.col === c) {
                squareEl.classList.add('selected');
            }

            // Highlight checked king
            if (checkedKing && checkedKing.row === r && checkedKing.col === c) {
                squareEl.classList.add('check');
            }

            const piece = game.board[r][c];
            if (piece) {
                const img = document.createElement('img');
                img.src = getPieceImgSrc(piece);
                img.className = 'piece-img';
                img.draggable = isMyPiece(piece) && gameActive && isMyTurn();

                img.addEventListener('dragstart', (e) => {
                    e.dataTransfer.setData('text/plain', JSON.stringify({ r, c }));
                    selectPiece(r, c);
                });

                squareEl.appendChild(img);
            }

            // Render hints
            const hint = possibleMoves.find(m => m[0] === r && m[1] === c);
            if (hint) {
                const hintEl = document.createElement('div');
                const isCapture = (piece !== null) || (selectedSquare && game.board[selectedSquare.row][selectedSquare.col].type === 'Pawn' && selectedSquare.col !== c);
                hintEl.className = isCapture ? 'hint-ring' : 'hint-dot';
                squareEl.appendChild(hintEl);
            }

            squareEl.addEventListener('click', () => {
                handleSquareClick(r, c);
            });

            squareEl.addEventListener('dragover', (e) => {
                if (gameActive && isMyTurn() && possibleMoves.some(m => m[0] === r && m[1] === c)) {
                    e.preventDefault();
                }
            });

            squareEl.addEventListener('drop', (e) => {
                e.preventDefault();
                const data = e.dataTransfer.getData('text/plain');
                if (data) {
                    const src = JSON.parse(data);
                    if (selectedSquare && src.r === selectedSquare.row && src.col === selectedSquare.col) {
                        tryMove(selectedSquare.row, selectedSquare.col, r, c);
                    }
                }
            });

            boardEl.appendChild(squareEl);
        }
    }
}

function updateCapturedLists() {
    const whiteListEl = document.getElementById('captured-white-list');
    const blackListEl = document.getElementById('captured-black-list');
    
    whiteListEl.innerHTML = '';
    blackListEl.innerHTML = '';

    game.capturedWhite.forEach(piece => {
        const img = document.createElement('img');
        img.className = 'captured-icon';
        img.src = getPieceImgSrc(piece);
        whiteListEl.appendChild(img);
    });

    game.capturedBlack.forEach(piece => {
        const img = document.createElement('img');
        img.className = 'captured-icon';
        img.src = getPieceImgSrc(piece);
        blackListEl.appendChild(img);
    });
}

function updateUsernames() {
    const localNameEl = document.getElementById('local-name');
    const opponentNameEl = document.getElementById('opponent-name');

    if (playMode === 'local') {
        localNameEl.textContent = '♔ White (You)';
        opponentNameEl.textContent = '♚ Black';
    } else if (playMode === 'bot') {
        localNameEl.textContent = `♔ ${localUsername} (White)`;
        opponentNameEl.textContent = `🤖 ${opponentUsername} (Black)`;
    } else {
        if (myColor === 'white') {
            localNameEl.textContent = `♔ ${localUsername} (White)`;
            opponentNameEl.textContent = `♚ ${opponentUsername} (Black)`;
        } else {
            localNameEl.textContent = `♚ ${localUsername} (Black)`;
            opponentNameEl.textContent = `♔ ${opponentUsername} (White)`;
        }
    }
}

function updateTurnIndicatorAndClocks() {
    const localCard = document.getElementById('local-card');
    const opponentCard = document.getElementById('opponent-card');
    const localClock = document.getElementById('local-clock');
    const opponentClock = document.getElementById('opponent-clock');

    const formatTime = (secs) => {
        const m = Math.floor(secs / 60);
        const s = secs % 60;
        return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    };

    if (playMode === 'local') {
        localClock.textContent = formatTime(localTimeLeft);
        opponentClock.textContent = formatTime(opponentTimeLeft);
        
        if (game.whiteTurn) {
            localCard.classList.add('active-turn');
            opponentCard.classList.remove('active-turn');
            document.getElementById('game-status').textContent = 'STATUS: WHITE\'S TURN';
        } else {
            opponentCard.classList.add('active-turn');
            localCard.classList.remove('active-turn');
            document.getElementById('game-status').textContent = 'STATUS: BLACK\'S TURN';
        }
    } else {
        const whiteTime = (myColor === 'white') ? localTimeLeft : opponentTimeLeft;
        const blackTime = (myColor === 'white') ? opponentTimeLeft : localTimeLeft;

        localClock.textContent = formatTime((myColor === 'white') ? whiteTime : blackTime);
        opponentClock.textContent = formatTime((myColor === 'white') ? blackTime : whiteTime);

        const isMyTurnActive = (game.whiteTurn === (myColor === 'white'));
        if (isMyTurnActive) {
            localCard.classList.add('active-turn');
            opponentCard.classList.remove('active-turn');
            document.getElementById('game-status').textContent = 'STATUS: YOUR TURN';
        } else {
            opponentCard.classList.add('active-turn');
            localCard.classList.remove('active-turn');
            document.getElementById('game-status').textContent = 'STATUS: OPPONENT\'S TURN';
        }
    }
}

function startTimer() {
    if (timerInterval) clearInterval(timerInterval);
    timerInterval = setInterval(() => {
        if (!gameActive) return;

        if (playMode === 'local') {
            if (game.whiteTurn) {
                localTimeLeft--;
                if (localTimeLeft <= 0) handleTimeOut('White');
            } else {
                opponentTimeLeft--;
                if (opponentTimeLeft <= 0) handleTimeOut('Black');
            }
        } else {
            if (game.whiteTurn) {
                if (myColor === 'white') {
                    localTimeLeft--;
                    if (localTimeLeft <= 0) handleTimeOut('White');
                } else {
                    opponentTimeLeft--;
                    if (opponentTimeLeft <= 0) handleTimeOut('White');
                }
            } else {
                if (myColor === 'black') {
                    localTimeLeft--;
                    if (localTimeLeft <= 0) handleTimeOut('Black');
                } else {
                    opponentTimeLeft--;
                    if (opponentTimeLeft <= 0) handleTimeOut('Black');
                }
            }
        }
        updateTurnIndicatorAndClocks();
    }, 1000);
}

function handleTimeOut(color) {
    gameActive = false;
    clearInterval(timerInterval);
    
    const loser = color;
    const winner = (loser === 'White') ? 'Black' : 'White';
    const msg = `${loser} ran out of time. ${winner} wins!`;
    addLogEntry('System', `Game over: ${msg}`, 'system');
    
    showGameOverModal('Time Out!', msg);
}

function checkGameEnd() {
    const activeTurn = game.whiteTurn;
    const hasMoves = game.hasLegalMove(activeTurn);
    const inCheck = game.isInCheck(activeTurn);
    
    let gameOver = false;
    let title = "";
    let message = "";

    if (!hasMoves) {
        gameOver = true;
        gameActive = false;
        clearInterval(timerInterval);
        
        if (inCheck) {
            title = "Checkmate!";
            message = `${activeTurn ? 'Black' : 'White'} won by checkmate.`;
            addLogEntry('System', `Game over: Checkmate! ${activeTurn ? 'Black' : 'White'} wins.`, 'system');
        } else {
            title = "Draw";
            message = "Stalemate - no legal moves.";
            addLogEntry('System', "Game over: Draw by stalemate.", 'system');
        }
    } else {
        const drawReason = game.getDrawReason();
        if (drawReason) {
            gameOver = true;
            gameActive = false;
            clearInterval(timerInterval);
            title = "Draw";
            if (drawReason === 'InsufficientMaterial') {
                message = "Draw by insufficient material.";
            } else if (drawReason === 'FiftyMoves') {
                message = "Draw by 50-move rule.";
            } else if (drawReason === 'Repetition') {
                message = "Draw by threefold repetition.";
            }
            addLogEntry('System', `Game over: Draw by ${drawReason}.`, 'system');
        }
    }

    if (gameOver) {
        showGameOverModal(title, message);
    }
}

function showGameOverModal(title, message) {
    document.getElementById('gameover-title').textContent = title;
    document.getElementById('gameover-message').textContent = message;
    document.getElementById('gameover-modal').classList.remove('hidden');
}

function startGame(mode) {
    playMode = mode;
    game = new Chess();
    selectedSquare = null;
    possibleMoves = [];
    localTimeLeft = 600;
    opponentTimeLeft = 600;
    gameActive = true;
    
    document.getElementById('log-area').innerHTML = '';
    addLogEntry('System', 'Game started.', 'system');

    if (playMode === 'multiplayer') {
        document.getElementById('chat-input').disabled = false;
        document.getElementById('btn-send-chat').disabled = false;
    } else {
        document.getElementById('chat-input').disabled = true;
        document.getElementById('btn-send-chat').disabled = true;
    }

    updateUsernames();
    
    document.getElementById('menu-screen').classList.remove('active');
    document.getElementById('game-screen').classList.add('active');

    startTimer();
    renderBoard();
    updateCapturedLists();
    updateTurnIndicatorAndClocks();
}

function sendChat() {
    const input = document.getElementById('chat-input');
    const text = input.value.trim();
    if (text === '') return;
    
    input.value = '';
    addLogEntry(localUsername, text, 'local');
    
    if (playMode === 'multiplayer' && network) {
        network.send({ type: 'CHAT', text });
    }
}

function addLogEntry(sender, text, type = '') {
    const logArea = document.getElementById('log-area');
    const entry = document.createElement('div');
    entry.className = `log-entry ${type}`;
    
    if (type === 'system') {
        entry.innerHTML = `&lt;${text}&gt;`;
    } else {
        entry.innerHTML = `<strong>${sender}:</strong> ${text}`;
    }
    
    logArea.appendChild(entry);
    logArea.scrollTop = logArea.scrollHeight;
}

function exportPGN() {
    const whiteName = (playMode === 'local') ? 'White' : ((myColor === 'white') ? localUsername : opponentUsername);
    const blackName = (playMode === 'local') ? 'Black' : ((myColor === 'white') ? opponentUsername : localUsername);
    const pgnText = game.exportToPGN(whiteName, blackName);
    
    const blob = new Blob([pgnText], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `chessol_match_${new Date().toISOString().slice(0,10).replace(/\./g, '-')}.pgn`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
}

function exitToMenu() {
    gameActive = false;
    clearInterval(timerInterval);
    if (network) {
        network.close();
        network = null;
    }
    
    document.getElementById('btn-host').disabled = false;
    document.getElementById('btn-join').disabled = false;
    document.getElementById('btn-bot').disabled = false;
    document.getElementById('btn-local').disabled = false;

    document.getElementById('btn-rematch').textContent = '🔄 Rematch';
    document.getElementById('btn-rematch').disabled = false;

    document.getElementById('promotion-modal').classList.add('hidden');
    document.getElementById('gameover-modal').classList.add('hidden');
    document.getElementById('host-status').classList.add('hidden');

    document.getElementById('game-screen').classList.remove('active');
    document.getElementById('menu-screen').classList.add('active');
}

function initNetworkListeners() {
    network.on('host_ready', (id) => {
        document.getElementById('my-code').textContent = id;
        document.getElementById('host-status').classList.remove('hidden');
    });

    network.on('connected', () => {
        addLogEntry('System', 'Connected to peer.', 'system');
    });

    network.on('color_assigned', (isWhite) => {
        myColor = isWhite ? 'white' : 'black';
        updateUsernames();
    });

    network.on('name_received', (name) => {
        opponentUsername = name;
        updateUsernames();
        document.getElementById('host-status').classList.add('hidden');
        startGame('multiplayer');
    });

    network.on('move_received', (move) => {
        executeMove(move.fromRow, move.fromCol, move.toRow, move.toCol, move.promotion);
    });

    network.on('chat_received', (text) => {
        addLogEntry(opponentUsername, text, 'opponent');
    });

    network.on('resigned', () => {
        gameActive = false;
        clearInterval(timerInterval);
        showGameOverModal('Opponent Resigned', `${opponentUsername} has resigned. You win!`);
        addLogEntry('System', `${opponentUsername} resigned. Game over.`, 'system');
    });

    network.on('draw_offer', () => {
        const accept = confirm(`${opponentUsername} offers a draw. Do you accept?`);
        if (accept) {
            network.send({ type: 'DRAW_ACCEPT' });
            gameActive = false;
            clearInterval(timerInterval);
            showGameOverModal('Draw', 'Draw by agreement.');
            addLogEntry('System', 'Game drawn by agreement.', 'system');
        } else {
            network.send({ type: 'DRAW_DECLINE' });
            addLogEntry('System', 'You declined the draw offer.', 'system');
        }
    });

    network.on('draw_accept', () => {
        gameActive = false;
        clearInterval(timerInterval);
        showGameOverModal('Draw', 'Opponent accepted draw offer. Draw by agreement.');
        addLogEntry('System', 'Draw offer accepted.', 'system');
    });

    network.on('draw_decline', () => {
        alert(`${opponentUsername} declined the draw offer.`);
        addLogEntry('System', `${opponentUsername} declined the draw offer.`, 'system');
    });

    network.on('rematch_request', () => {
        const accept = confirm(`${opponentUsername} requests a rematch. Do you accept?`);
        if (accept) {
            network.send({ type: 'REMATCH_ACCEPT' });
            myColor = (myColor === 'white') ? 'black' : 'white';
            startGame('multiplayer');
            document.getElementById('gameover-modal').classList.add('hidden');
        } else {
            network.send({ type: 'REMATCH_DECLINE' });
        }
    });

    network.on('rematch_accept', () => {
        document.getElementById('gameover-modal').classList.add('hidden');
        myColor = (myColor === 'white') ? 'black' : 'white';
        startGame('multiplayer');
    });

    network.on('rematch_decline', () => {
        alert(`${opponentUsername} declined the rematch request.`);
        document.getElementById('btn-rematch').textContent = '🔄 Rematch';
        document.getElementById('btn-rematch').disabled = false;
    });

    network.on('disconnected', (reason) => {
        alert(`Disconnected: ${reason}`);
        exitToMenu();
    });

    network.on('error', (err) => {
        alert(`Network error: ${err.message || err}`);
        exitToMenu();
    });
}

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('btn-host').addEventListener('click', () => {
        readUsername();
        network = new ChessNetwork();
        initNetworkListeners();
        network.hostGame(localUsername);
        
        document.getElementById('btn-host').disabled = true;
        document.getElementById('btn-join').disabled = true;
        document.getElementById('btn-bot').disabled = true;
        document.getElementById('btn-local').disabled = true;
    });

    document.getElementById('btn-join').addEventListener('click', () => {
        const targetId = document.getElementById('join-id-input').value.trim();
        if (targetId === '') {
            alert('Please enter an opponent code.');
            return;
        }
        readUsername();
        network = new ChessNetwork();
        initNetworkListeners();
        network.joinGame(targetId, localUsername);

        document.getElementById('btn-host').disabled = true;
        document.getElementById('btn-join').disabled = true;
        document.getElementById('btn-bot').disabled = true;
        document.getElementById('btn-local').disabled = true;
    });

    document.getElementById('btn-bot').addEventListener('click', () => {
        readUsername();
        opponentUsername = 'Chess Bot';
        myColor = 'white';
        startGame('bot');
    });

    document.getElementById('btn-local').addEventListener('click', () => {
        readUsername();
        myColor = 'white';
        startGame('local');
    });

    document.getElementById('btn-copy-code').addEventListener('click', () => {
        const codeText = document.getElementById('my-code').textContent;
        navigator.clipboard.writeText(codeText).then(() => {
            const copyBtn = document.getElementById('btn-copy-code');
            copyBtn.textContent = '✅';
            setTimeout(() => copyBtn.textContent = '📋', 1500);
        });
    });

    document.getElementById('btn-send-chat').addEventListener('click', sendChat);
    document.getElementById('chat-input').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') sendChat();
    });

    document.getElementById('btn-offer-draw').addEventListener('click', () => {
        if (!gameActive) return;
        if (playMode === 'multiplayer') {
            network.send({ type: 'DRAW_OFFER' });
            addLogEntry('System', 'Draw offer sent to opponent.', 'system');
        } else if (playMode === 'bot') {
            addLogEntry('System', 'Draw offer sent to bot.', 'system');
            setTimeout(() => {
                addLogEntry('Chess Bot', 'I decline the draw offer.', 'opponent');
            }, 800);
        } else {
            const accept = confirm('Black: White offers a draw. Do you accept?');
            if (accept) {
                gameActive = false;
                clearInterval(timerInterval);
                showGameOverModal('Draw', 'Draw by agreement.');
                addLogEntry('System', 'Game drawn by agreement.', 'system');
            } else {
                addLogEntry('System', 'Black declined the draw offer.', 'system');
            }
        }
    });

    document.getElementById('btn-resign').addEventListener('click', () => {
        if (!gameActive) return;
        const confirmResign = confirm('Are you sure you want to resign?');
        if (!confirmResign) return;

        if (playMode === 'multiplayer') {
            network.send({ type: 'RESIGN' });
            gameActive = false;
            clearInterval(timerInterval);
            showGameOverModal('You Resigned', `You resigned. ${opponentUsername} wins!`);
            addLogEntry('System', 'You resigned. Game over.', 'system');
        } else if (playMode === 'bot') {
            gameActive = false;
            clearInterval(timerInterval);
            showGameOverModal('You Resigned', 'You resigned. Chess Bot wins!');
            addLogEntry('System', 'You resigned. Game over.', 'system');
        } else {
            gameActive = false;
            clearInterval(timerInterval);
            const winner = game.whiteTurn ? 'Black' : 'White';
            showGameOverModal('Resignation', `${game.whiteTurn ? 'White' : 'Black'} resigned. ${winner} wins!`);
            addLogEntry('System', `${game.whiteTurn ? 'White' : 'Black'} resigned. Game over.`, 'system');
        }
    });

    document.getElementById('btn-rematch').addEventListener('click', () => {
        if (playMode === 'multiplayer') {
            network.send({ type: 'REMATCH_REQUEST' });
            document.getElementById('btn-rematch').textContent = 'Waiting for peer...';
            document.getElementById('btn-rematch').disabled = true;
        } else {
            document.getElementById('gameover-modal').classList.add('hidden');
            startGame(playMode);
        }
    });

    document.getElementById('btn-export-pgn').addEventListener('click', exportPGN);
    document.getElementById('btn-exit-menu').addEventListener('click', exitToMenu);

    document.querySelectorAll('.promo-option').forEach(option => {
        option.addEventListener('click', () => {
            if (!pendingPromotion) return;
            const promoType = option.dataset.promo;
            document.getElementById('promotion-modal').classList.add('hidden');
            executeMove(pendingPromotion.fromRow, pendingPromotion.fromCol, pendingPromotion.toRow, pendingPromotion.toCol, promoType);
            pendingPromotion = null;
        });
    });
});
