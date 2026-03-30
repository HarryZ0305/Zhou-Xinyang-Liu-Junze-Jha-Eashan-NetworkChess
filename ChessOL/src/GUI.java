import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;

public class GUI extends JFrame {
    private JPanel cards;
    private CardLayout cardLayout;
    private JTextArea logArea;
    private Socket socket;
    private PrintWriter out;

    public GUI() {
        setTitle("ChessOL");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        //menu
        JPanel menuPanel = createMenuPanel();
        //board
        JPanel gamePanel = createGamePanel();

        cards.add(menuPanel, "MENU");
        cards.add(gamePanel, "GAME");

        add(cards);
        setVisible(true);
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        JButton btnServer = new JButton("create match (Server)");
        JButton btnClient = new JButton("join match (Client)");
        logArea = new JTextArea("waiting for selection...");
        logArea.setEditable(false);

        btnServer.addActionListener(e -> {
            String ip = JOptionPane.showInputDialog("Input host IP:", "127.0.0.1");
            if (ip != null) startNetwork(true, ip);
        });
        btnClient.addActionListener(e -> {
            String ip = JOptionPane.showInputDialog("Input client IP:", "127.0.0.1");
            if (ip != null) startNetwork(false, ip);
        });

        panel.add(btnServer);
        panel.add(btnClient);
        panel.add(new JScrollPane(logArea));
        return panel;
    }

    private JPanel createGamePanel() {
        JPanel panel = new JPanel(new GridLayout(8, 8));
        for (int i = 0; i < 64; i++) {
            JButton btn = new JButton();
            if ((i / 8 + i % 8) % 2 == 0) btn.setBackground(Color.LIGHT_GRAY);
            else btn.setBackground(Color.WHITE);
            
            int finalI = i;
            btn.addActionListener(e -> handleMove(finalI));
            panel.add(btn);
        }
        return panel;
    }

    // networking logic
    private void startNetwork(boolean isServer, String ip) {
        new Thread(() -> {
            try {
                if (isServer) {
                    logArea.append("\nwaiting for connection...");
                    ServerSocket server = new ServerSocket(8888);
                    socket = server.accept();
                } else {
                    logArea.append("\nconnecting to: " + ip);
                    socket = new Socket(ip, 8888);
                }
                
                out = new PrintWriter(socket.getOutputStream(), true);
                SwingUtilities.invokeLater(() -> cardLayout.show(cards, "GAME"));
                
                logArea.append("\nconnected game started!");
                
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> logArea.append("\nerror: " + ex.getMessage()));
            }
        }).start();
    }

    private void handleMove(int index) {
        if (out != null) {
            out.println("MOVE:" + index); 
            System.out.println("send: " + index);
        }
    }

    public static void main(String[] args) {
        new GUI();
    }
}
