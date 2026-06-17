import java.io.*;
import java.net.*;

public class ChessNetwork {
    private Socket socket;
    private ServerSocket serverSocket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread readThread;
    private final ChessNetworkListener listener;
    private volatile boolean isDisconnectExpected = false;

    public ChessNetwork(ChessNetworkListener listener) {
        this.listener = listener;
    }

    public void startServer(int port) {
        isDisconnectExpected = false;
        readThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                socket = serverSocket.accept();
                serverSocket.close(); // Close server socket since we only want one client
                
                setupStreamsAndLoop();
            } catch (Exception e) {
                handleDisconnect(e);
            }
        });
        readThread.start();
    }

    public void startClient(String ip, int port) {
        isDisconnectExpected = false;
        readThread = new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                
                setupStreamsAndLoop();
            } catch (Exception e) {
                handleDisconnect(e);
            }
        });
        readThread.start();
    }

    private void setupStreamsAndLoop() throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        listener.onConnected();
        
        String line;
        while ((line = in.readLine()) != null) {
            parseLine(line);
        }
        
        handleDisconnect(null);
    }

    private void parseLine(String line) {
        try {
            if (line.startsWith("COLOR:")) {
                boolean clientIsWhite = line.substring(6).equals("white");
                listener.onColorAssigned(clientIsWhite);
            } else if (line.startsWith("NAME:")) {
                listener.onNameReceived(line.substring(5));
            } else if (line.startsWith("CHAT:")) {
                listener.onChatReceived(line.substring(5));
            } else if (line.startsWith("MOVE:")) {
                String[] parts = line.substring(5).split(",");
                int fromRow = Integer.parseInt(parts[0]);
                int fromCol = Integer.parseInt(parts[1]);
                int toRow = Integer.parseInt(parts[2]);
                int toCol = Integer.parseInt(parts[3]);
                boolean isWhite = Boolean.parseBoolean(parts[4]);
                String pawnPromotion = parts[5];
                boolean isCheck = Boolean.parseBoolean(parts[6]);
                int peerSig = parts.length >= 8 ? Integer.parseInt(parts[7]) : 0;
                listener.onMoveReceived(fromRow, fromCol, toRow, toCol, isWhite, pawnPromotion, isCheck, peerSig);
            } else if (line.startsWith("GAME:RESIGN")) {
                listener.onResigned();
            } else if (line.startsWith("GAME:DRAW_OFFER")) {
                listener.onDrawOffer();
            } else if (line.startsWith("GAME:DRAW_ACCEPT")) {
                listener.onDrawAccept();
            } else if (line.startsWith("GAME:DRAW_DECLINE")) {
                listener.onDrawDecline();
            } else if (line.startsWith("REMATCH:REQUEST")) {
                listener.onRematchRequest();
            } else if (line.startsWith("REMATCH:ACCEPT")) {
                listener.onRematchAccept();
            } else if (line.startsWith("REMATCH:DECLINE")) {
                listener.onRematchDecline();
            }
        } catch (Exception e) {
            System.err.println("Parse error in network thread: " + e.getMessage());
        }
    }

    public void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    public boolean isSendFailed() {
        return out == null || out.checkError();
    }

    public synchronized void close() {
        isDisconnectExpected = true;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (Exception e) {
            System.err.println("Error closing network: " + e.getMessage());
        }
        out = null;
        in = null;
        socket = null;
        serverSocket = null;
    }

    private void handleDisconnect(Exception e) {
        if (!isDisconnectExpected) {
            String reason = (e != null) ? e.getMessage() : "Connection closed by remote peer.";
            listener.onDisconnected(reason);
        }
        close();
    }
}
