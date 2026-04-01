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

        //menu
        JPanel menu = new JPanel(null);
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
                    SwingUtilities.invokeLater(() -> logArea.append("Opponent: " + msg + "\n"));
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> logArea.append("Error: " + e.getMessage() + "\n"));
            }
        }).start();
    }

    public static void main(String[] args) { new GUI(); }
}