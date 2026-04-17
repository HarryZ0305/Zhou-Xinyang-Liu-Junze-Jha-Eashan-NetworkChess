import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;

public class GUI extends JFrame {
    private JPanel cards = new JPanel(new CardLayout());
    private JTextArea logArea = new JTextArea();
    private JTextField inputField = new JTextField();
    private PrintWriter out;
    Game game=new Game();

    public GUI() {
        setTitle("Network Skeleton");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        //menu — replaced plain panel with chess board home screen
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
        logArea.setEditable(false);
        workPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
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
                boolean c=game.canMove(x1, y1, x2, y2, isWhite);
                if (out.checkError()) {
                    logArea.append("System: Send failed, connection lost.\n");
                } else {
                    if(!c){
                        logArea.append("Invalid move\n");
                    }else{
                        String msg = in;
                        if((x2==0&&isWhite)||(x2==7&&!isWhite)){
                            String prompt = "Promote to (Q/R/B/N):";
                            String promo;
                            while (true) {
                                promo = JOptionPane.showInputDialog(this, prompt, "Pawn Promotion", JOptionPane.PLAIN_MESSAGE);
                                if (promo == null) { prompt = "Invalid piece type. Promote to (Q/R/B/N):"; continue; }
                                promo = promo.trim().toUpperCase();
                                if (promo.equals("Q") || promo.equals("R") || promo.equals("B") || promo.equals("N")) break;
                                prompt = "Invalid piece type. Promote to (Q/R/B/N):";
                            }
                            msg += "," + promo;
                            game.promotion(x1, y1, isWhite,promo);
                        }else{
                            msg += ",None";
                        }
                        game.Move(x1, y1, x2, y2, isWhite);
                        out.println(msg);
                        logArea.append(msg + "\n");
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
                    logArea.append("Waiting for client on 443...\n");
                    try (ServerSocket ss = new ServerSocket(443)) { s = ss.accept(); }
                } else {
                    logArea.append("Connecting to " + ip + "...\n");
                    s = new Socket(ip, 443);
                }
                
                s.setKeepAlive(true);
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
                        SwingUtilities.invokeLater(() -> logArea.append("Moved:"+msg+ "\n"));
                        if(!promo.equals("None")){
                            // Handle promotion if needed (not implemented in this snippet)
                            game.promotion(x1, y1, isWhite,promo);
                        }
                        game.Move(x1, y1, x2, y2, isWhite);
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> logArea.append("MoveError: " + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "\n"));
                    }
                }
                SwingUtilities.invokeLater(() -> logArea.append("System: Connection closed.\n"));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> logArea.append("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n"));
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) { new GUI(); }


    // ── Inner class: paints a chess board as the menu background ─────────────
    // Only change to the menu: JPanel menu = new ChessBoardMenuPanel()
    // Everything else (btnServer, btnClient, bounds, listeners) is untouched
    private static class ChessBoardMenuPanel extends JPanel {

        private static final Color LIGHT  = new Color(240, 217, 181); // cream
        private static final Color DARK   = new Color(181, 136,  99); // brown

        ChessBoardMenuPanel() {
            setLayout(null); // null layout preserved — setBounds() on buttons still works
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

            // Draw 8x8 chess squares across the whole panel
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    g2.setColor((row + col) % 2 == 0 ? LIGHT : DARK);
                    g2.fillRect(col * sq, row * sq, sq, sq);
                }
            }

            
        }
    }
}
