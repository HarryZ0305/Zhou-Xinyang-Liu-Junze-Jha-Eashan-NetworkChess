import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class GUI extends JFrame {
    
	private JPanel cards = new JPanel(new CardLayout());
    private JTextArea logArea = new JTextArea();
    private JTextField inputField = new JTextField();
    private PrintWriter out;
    Game game = new Game();
    ActiveBoardPanel activeBoard;
    private boolean isServer;
    
    public GUI() {
        setTitle("Network Skeleton");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        //menu — now uses ChessBoardMenuPanel for the home screen background
        JPanel menu = new ChessBoardMenuPanel();
        JButton btnServer = new JButton("Server");
        btnServer.setBounds(50, 50, 150, 40);
        JButton btnClient = new JButton("Client");
        btnClient.setBounds(250, 50, 150, 40);
        btnServer.addActionListener(e -> startNetwork(true, null));
        btnClient.addActionListener(e -> {
            String ip = JOptionPane.showInputDialog("Host IP:", "127.0.0.1");
            if (ip != null) startNetwork(false, ip);
        });
        menu.add(btnServer); menu.add(btnClient);
        //board
        JPanel workPanel = new JPanel(new BorderLayout());
        activeBoard = new ActiveBoardPanel();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        workPanel.add(activeBoard, BorderLayout.CENTER);
        workPanel.add(inputField, BorderLayout.SOUTH);
        workPanel.add(logScroll, BorderLayout.EAST);
        //send
        inputField.addActionListener(e -> {
            if (out != null) {
                String msg = inputField.getText();
                out.println(msg); 
                logArea.append("You: " + msg + "\n");
                inputField.setText("");
            }
        });
        cards.add(menu, "MENU");
        cards.add(workPanel, "WORK");
        add(cards);
        setVisible(true);
    }

    private void attemptMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (out == null) {
            logArea.append("System: Not connected yet.\n");
            return;
        }
        
        Piece p = game.board[fromRow][fromCol];
        if (p == null){
            return;
        } 
        boolean isWhite = p.isWhite;
        
        Player player = game.whiteTurn ? game.whitePlayer : game.blackPlayer;
        boolean canMove = game.canMove(fromRow, fromCol, toRow, toCol, isWhite, player.isInCheck);
        
        if (!canMove) {
            logArea.append("Invalid move\n");
        } else {
            String pawnPromotion = "None";
            if (game.board[fromRow][fromCol] instanceof Pawn && ((toRow == 0 && isWhite) || (toRow == 7 && !isWhite))) {
                String prompt = "Promote to (Q/R/B/N):";
                while (true) {
                    pawnPromotion= JOptionPane.showInputDialog(this, prompt, "Pawn Promotion", JOptionPane.PLAIN_MESSAGE);
                    if (pawnPromotion == null) { 
                        pawnPromotion = "Q"; break; 
                    }
                    pawnPromotion = pawnPromotion.trim().toUpperCase();
                    if (pawnPromotion.equals("Q") || pawnPromotion.equals("R") || pawnPromotion.equals("B") || pawnPromotion.equals("N")) break;
                    prompt = "Invalid piece type. Promote to (Q/R/B/N):";
                }
            }
            
            String message = fromRow + "," + fromCol + "," + toRow + "," + toCol + "," + isWhite + "," + pawnPromotion
            
            game.Move(fromRow, fromCol, toRow, toCol, isWhite);
            if (!pawnPromotion.equals("None")) {
                game.promotion(toRow, toCol, isWhite, pawnPromotion);
            }
            
            Piece king = game.getKing(!isWhite);
            if (game.isInCheck(king.row, king.col, !isWhite)) {
                logArea.append("Check!\n");
                message += ",true";
            } else {
                message += ",false";
            }
            
            out.println(message);
            if (out.checkError()) {
                logArea.append("System: Send failed, connection lost.\n");
            } else {
                logArea.append("Moved: " + message + "\n");
                game.whiteTurn = !game.whiteTurn;
                activeBoard.repaint();
            }
        }
    }

    private void startNetwork(boolean isServer, String ip) {
        this.isServer = isServer;
        //switch page
        ((CardLayout)cards.getLayout()).show(cards, "WORK");
        new Thread(() -> {
            try {
                Socket s;
                if (isServer) {
                    logArea.append("Waiting for client on 8888...\n");
                    s = new ServerSocket(8888).accept();
                } else {
                    logArea.append("Connecting to " + ip + "...\n");
                    s = new Socket(ip, 8888);
                }
                out = new PrintWriter(s.getOutputStream(), true);
                logArea.append("System: Connected!\n");
                //recive
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    String message = line;
                    String[] parts = message.split(",");
                    if (parts.length < 5) {
                        SwingUtilities.invokeLater(() -> logArea.append("Chat: " + message + "\n"));
                        continue;
                    }
                    try {
                        int fromRow = Integer.parseInt(parts[0]);
                        int fromCol = Integer.parseInt(parts[1]);
                        int toRow = Integer.parseInt(parts[2]);
                        int toCol = Integer.parseInt(parts[3]);
                        boolean isWhite = Boolean.parseBoolean(parts[4]);
                        String pawnPromotion= parts[5];
                        boolean isCheck = Boolean.parseBoolean(parts[6]);
                        SwingUtilities.invokeLater(() -> {
                            logArea.append("Moved:" + message + "\n");
                            if (!game.canMove(fromRow, fromCol, toRow, toCol, isWhite, false)) {
                                logArea.append("Invalid move received from peer\n");
                                return;
                            }
                            if (!pawnPromotion.equals("None")) {
                                game.promotion(fromRow, fromCol, isWhite, pawnPromotion);
                            }
                            if (isCheck) {
                                logArea.append("In Check!\n");
                                Player player = isWhite ? game.whitePlayer : game.blackPlayer;
                                player.isInCheck = true;
                            }
                            game.Move(fromRow, fromCol, toRow, toCol, isWhite);
                            game.whiteTurn = !game.whiteTurn;
                            activeBoard.repaint();
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> logArea.append("MoveError: " + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "\n"));
                    }
                }
                SwingUtilities.invokeLater(() -> logArea.append("System: Connection closed.\n"));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> logArea.append("Error: " + e.getMessage() + "\n"));
            }
        }).start();
    }
    
    public static void main(String[] args) { 
            new GUI(); 
    }

    private static class ChessBoardMenuPanel extends JPanel {

        private static final Color LIGHT = new Color(240, 217, 181); // cream squares
        private static final Color DARK  = new Color(181, 136,  99); // brown squares

        ChessBoardMenuPanel() {
            setLayout(null); // preserves null layout — button .setBounds() unchanged
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w  = getWidth();
            int h  = getHeight();
            int sq = Math.min(w, h) / 8; 

            // 8x8 board squares filling the whole panel
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    g2.setColor((row + col) % 2 == 0 ? LIGHT : DARK);
                    g2.fillRect(col * sq, row * sq, sq, sq);
                }
            }

            // Semi-transparent overlay so buttons + text are readable on top
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRect(0, 0, w, h);

            // Game title
            g2.setFont(new Font("Serif", Font.BOLD, 38));
            g2.setColor(new Color(240, 210, 150));
            String title = "ChessOL";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, w / 2 - fm.stringWidth(title) / 2, h / 2 - 15);

            // Subtitle
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(new Color(200, 185, 155));
            String sub = "Select Server or Client to begin";
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(sub, w / 2 - fm2.stringWidth(sub) / 2, h / 2 + 12);
        }
    }

    private class ActiveBoardPanel extends JPanel {
        private static final Color light = new Color(240, 217, 181);
        private static final Color dark  = new Color(181, 136,  99);
        private HashMap<String, Image> pieceImages = new HashMap<>();
        private int selectedRow = -1;
        private int selectedCol = -1;

        public ActiveBoardPanel(){
            loadImages();

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    if (game == null || out == null){
                        return;
                    } 
                    
                    int w = getWidth();
                    int h = getHeight();
                    int sq = Math.min(w, h) / 8;
                    
                    int col = e.getX() / sq;
                    int row = e.getY() / sq;
                    
                    if (col >= 8 || row >= 8){
                        return; // Prevent out of bounds clicks
                    }
                    
                    if (selectedRow == -1) {
                        Piece p = game.board[row][col];
                        if (p != null && p.isWhite == game.whiteTurn && p.isWhite == isServer) {
                            selectedRow = row;
                            selectedCol = col;
                            repaint();
                        }
                    } else {
                        // Deselect if clicking the same square
                        if (selectedRow == row && selectedCol == col) {
                            selectedRow = -1;
                            selectedCol = -1;
                            repaint();
                        } else {
                            attemptMove(selectedRow, selectedCol, row, col);
                            selectedRow = -1;
                            selectedCol = -1;
                            repaint();
                        }
                    }
                }
            });
        }

        private void loadImages(){
            String[] pieces = {"Pawn", "Rook", "Knight", "Bishop", "Queen", "King"};
            String[] colors = {"White", "Black"};

            try{
                for(String pieceColor: colors){
                    for(String pieceType: pieces){
                        String fileName = "ChessPieces/" + pieceColor + "Pieces/" + pieceColor + pieceType + ".png";
                        Image imagePiece = ImageIO.read(new File(fileName));
                        pieceImages.put(pieceColor + pieceType, imagePiece);
                    }
                }
            } catch(IOException e){
                System.out.println("Error loading images: " + e.getMessage());
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int sq = Math.min(w, h) / 8;

            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    g2.setColor((row + col) % 2 == 0 ? light : dark);
                    g2.fillRect(col * sq, row * sq, sq, sq);
                }
            }

            if (selectedRow != -1 && selectedCol != -1) {
                g2.setColor(new Color(255, 255, 50, 120)); 
                g2.fillRect(selectedCol * sq, selectedRow * sq, sq, sq);
            }

            if (game != null && game.board != null) {
                for (int row = 0; row < 8; row++) {
                    for (int col = 0; col < 8; col++) {
                        Piece p = game.board[row][col];
                        if (p != null) {
                            String colorStr = p.isWhite ? "White" : "Black";
                            String key = colorStr + p.getType();
                            Image img = pieceImages.get(key);
                            if (img != null) {
                                g2.drawImage(img, col * sq, row * sq, sq, sq, this);
                            }
                        }
                    }
                }
            }
        }  
    }
}

