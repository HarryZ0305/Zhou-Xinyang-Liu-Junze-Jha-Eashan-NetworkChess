import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;
public class GUI extends JFrame {
    private JPanel cards = new JPanel(new CardLayout());
    private JTextArea logArea = new JTextArea();
    private JTextField inputField = new JTextField();
    private PrintWriter out;
    Game game;
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
        logArea.setEditable(false);
        workPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        workPanel.add(inputField, BorderLayout.SOUTH);
        //send
        inputField.addActionListener(e -> {
            if (out != null) {
                out.println(inputField.getText());
                logArea.append("Me: " + inputField.getText() + "\n");
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
                    SwingUtilities.invokeLater(() -> logArea.append("Opponent: " + parts[0] + "\n"));
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> logArea.append("Error: " + e.getMessage() + "\n"));
            }
        }).start();
    }
    public static void main(String[] args) { new GUI(); 
    }

    // ── Home screen: chess board drawn as background behind the buttons ───────
    // Only change to the menu block above is: new JPanel(null) → new ChessBoardMenuPanel()
    // null layout is kept inside so setBounds() on btnServer/btnClient still works
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
            int sq = Math.min(w, h) / 8; // square size — 1/8th of shorter dimension

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
}
