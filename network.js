class ChessNetwork {
    constructor() {
        this.peer = null;
        this.conn = null;
        this.isHost = false;
        this.handlers = {};
    }

    on(event, handler) {
        this.handlers[event] = handler;
    }

    trigger(event, ...args) {
        if (this.handlers[event]) {
            this.handlers[event](...args);
        }
    }

    hostGame(myUsername) {
        this.isHost = true;
        this.peer = new Peer();

        this.peer.on('open', (id) => {
            this.trigger('host_ready', id);
        });

        this.peer.on('connection', (connection) => {
            if (this.conn) {
                // Reject secondary connections if game already full
                connection.on('open', () => {
                    connection.send({ type: 'REJECT', reason: 'Game already full' });
                    setTimeout(() => connection.close(), 500);
                });
                return;
            }
            this.conn = connection;
            this.setupConnection(myUsername);
        });

        this.peer.on('error', (err) => {
            console.error('PeerJS Host Error:', err);
            this.trigger('error', err);
        });
    }

    joinGame(targetId, myUsername) {
        this.isHost = false;
        this.peer = new Peer();

        this.peer.on('open', () => {
            this.conn = this.peer.connect(targetId);
            this.setupConnection(myUsername);
        });

        this.peer.on('error', (err) => {
            console.error('PeerJS Join Error:', err);
            this.trigger('error', err);
        });
    }

    setupConnection(myUsername) {
        this.conn.on('open', () => {
            this.trigger('connected');
            // Share local player name
            this.send({ type: 'NAME', name: myUsername });
            
            if (this.isHost) {
                // Host dictates black player color assignment
                this.send({ type: 'COLOR', color: 'black' });
                this.trigger('color_assigned', true); // Host is White
            }
        });

        this.conn.on('data', (data) => {
            if (data && typeof data === 'object') {
                this.handleMessage(data);
            }
        });

        this.conn.on('close', () => {
            this.trigger('disconnected', 'Connection closed by remote peer.');
            this.close();
        });

        this.conn.on('error', (err) => {
            this.trigger('disconnected', err.message || 'Connection encountered an error.');
            this.close();
        });
    }

    handleMessage(data) {
        switch (data.type) {
            case 'REJECT':
                this.trigger('disconnected', data.reason);
                break;
            case 'COLOR':
                // Client is assigned color (normally white=false, i.e., black)
                const isWhite = (data.color === 'white');
                this.trigger('color_assigned', isWhite);
                break;
            case 'NAME':
                this.trigger('name_received', data.name);
                break;
            case 'CHAT':
                this.trigger('chat_received', data.text);
                break;
            case 'MOVE':
                this.trigger('move_received', data.move); // move: { fromRow, fromCol, toRow, toCol, promotion, isCheck, stateSig }
                break;
            case 'RESIGN':
                this.trigger('resigned');
                break;
            case 'DRAW_OFFER':
                this.trigger('draw_offer');
                break;
            case 'DRAW_ACCEPT':
                this.trigger('draw_accept');
                break;
            case 'DRAW_DECLINE':
                this.trigger('draw_decline');
                break;
            case 'REMATCH_REQUEST':
                this.trigger('rematch_request');
                break;
            case 'REMATCH_ACCEPT':
                this.trigger('rematch_accept');
                break;
            case 'REMATCH_DECLINE':
                this.trigger('rematch_decline');
                break;
        }
    }

    send(data) {
        if (this.conn && this.conn.open) {
            this.conn.send(data);
        }
    }

    close() {
        if (this.conn) {
            try { this.conn.close(); } catch(e){}
            this.conn = null;
        }
        if (this.peer) {
            try { this.peer.destroy(); } catch(e){}
            this.peer = null;
        }
    }
}
