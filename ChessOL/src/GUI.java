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
        ActiveBoardPanel activeBoard = new ActiveBoardPanel();
        workPanel.add(activeBoard, BorderLayout.CENTER); 
        workPanel.add(inputField, BorderLayout.SOUTH);
        //send
        inputField.addActionListener(e -> {
            if (out != null) {
                String in=inputField.getText();
                String[] parts = in.split(",");
                if (parts.length < 5) {
                    SwingUtilities.invokeLater(() -> logArea.append("Chat: " + in + "\n"));
                }
                int x1 = Integer.parseInt(parts[0]);
                int y1 = Integer.parseInt(parts[1]);
                int x2 = Integer.parseInt(parts[2]);
                int y2 = Integer.parseInt(parts[3]);
                boolean isWhite = Boolean.parseBoolean(parts[4]);
                Player player=game.whiteTurn?game.whitePlayer:game.blackPlayer;
                boolean c=game.canMove(x1, y1, x2, y2, isWhite,player.isInCheck);
                if (out.checkError()) {
                    logArea.append("System: Send failed, connection lost.\n");
                } else {
                    if(!c){
                        logArea.append("Invalid move\n");
                    }else{
                        String msg = in;
                        String promo = "None";
                        if((x2==0&&isWhite)||(x2==7&&!isWhite)){
                            String prompt = "Promote to (Q/R/B/N):";
                            while (true) {
                                promo = JOptionPane.showInputDialog(this, prompt, "Pawn Promotion", JOptionPane.PLAIN_MESSAGE);
                                if (promo == null) { prompt = "Invalid piece type. Promote to (Q/R/B/N):"; continue; }
                                promo = promo.trim().toUpperCase();
                                if (promo.equals("Q") || promo.equals("R") || promo.equals("B") || promo.equals("N")) break;
                                prompt = "Invalid piece type. Promote to (Q/R/B/N):";
                            }
                            msg += "," + promo;
                        }else{
                            msg += ",None";
                        }
                        game.Move(x1, y1, x2, y2, isWhite);
                        if(!promo.equals("None")){
                            game.promotion(x2, y2, isWhite, promo);
                        }
                        Piece king = game.getKing(!isWhite);
                        if(game.isInCheck(king.row, king.col, !isWhite)){
                            logArea.append("Check!\n");
                            msg+=",true";
                        }else{
                            msg+=",false";
                        }
                        out.println(msg);
                        logArea.append(msg + "\n");
                        game.whiteTurn=!game.whiteTurn;
                    }
                }
                inputField.setText("");
            }
        });
        cards.add(menu, "MENU");
        cards.add(workPanel, "WORK");
        add(cards);
        setVisible(true);
    }
    private void startNetwork(boolean isServer, String ip) {
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
                    String msg = line;
                    String[] parts = msg.split(",");
                    if (parts.length < 5) {
                        SwingUtilities.invokeLater(() -> logArea.append("Chat: " + msg + "\n"));
                        continue;
                    }
                    try {
                        int x1 = Integer.parseInt(parts[0]);
                        int y1 = Integer.parseInt(parts[1]);
                        int x2 = Integer.parseInt(parts[2]);
                        int y2 = Integer.parseInt(parts[3]);
                        boolean isWhite = Boolean.parseBoolean(parts[4]);
                        String promo =parts[5];
                        boolean isCheck = Boolean.parseBoolean(parts[6]);
                        SwingUtilities.invokeLater(() -> logArea.append("Moved:"+msg+ "\n"));
                        if(!promo.equals("None")){
                            // Handle promotion if needed (not implemented in this snippet)
                            game.promotion(x1, y1, isWhite,promo);
                        }
                        if(isCheck){
                            SwingUtilities.invokeLater(() -> logArea.append("In Check!"+"\n"));
                            Player player=isWhite?game.whitePlayer:game.blackPlayer;
                            player.isInCheck=true;
                        }
                        game.Move(x1, y1, x2, y2, isWhite);
                        game.whiteTurn=!game.whiteTurn;
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

        public ActiveBoardPanel(){
            loadImages();
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

