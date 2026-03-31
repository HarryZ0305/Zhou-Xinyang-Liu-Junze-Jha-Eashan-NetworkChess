import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;

public class GUI extends JFrame {
    private JPanel cards = new JPanel(new CardLayout());
    private JTextArea logArea = new JTextArea();
    private JTextField inputField = new JTextField();
    private PrintWriter out;

    public GUI() {
        setTitle("Net Skeleton");
        setSize(640, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        //menu
        JPanel menu = new JPanel(null);
        JButton btnServer = new JButton("Server");
        btnServer.setBounds(10, 10, 200, 50);
        JButton btnClient = new JButton("Client");
        btnClient.setBounds(220, 10, 200, 50);

        btnServer.addActionListener(e -> startNetwork(true, null));
        btnClient.addActionListener(e -> {
            String ip = JOptionPane.showInputDialog("Input Server IP:", "127.0.0.1");
            if (ip != null) startNetwork(false, ip);
        });

        menu.add(btnServer);
        menu.add(btnClient);

        //chat
        JPanel chat = new JPanel(new BorderLayout());
        logArea.setEditable(false);
        chat.add(new JScrollPane(logArea), BorderLayout.CENTER);
        chat.add(inputField, BorderLayout.SOUTH);

        //return key sends message
        inputField.addActionListener(e -> {
            if (out != null) {
                String msg = inputField.getText();
                out.println(msg); //send
                logArea.append("Me: " + msg + "\n");
                inputField.setText("");
            }
        });

        cards.add(menu, "MENU");
        cards.add(chat, "CHAT");
        add(cards);
        setVisible(true);
    }

    private void startNetwork(boolean isServer, String ip) {
    ((CardLayout)cards.getLayout()).show(cards, "CHAT");

    new Thread(() -> {
        try {
            Socket s;
            if (isServer) {
                SwingUtilities.invokeLater(() -> logArea.append("Waiting for connection on port 8888...\n"));
                ServerSocket server = new ServerSocket(8888);
                s = server.accept(); //stop until client connects
                SwingUtilities.invokeLater(() -> logArea.append("Client connected!\n"));
            } else {
                SwingUtilities.invokeLater(() -> logArea.append("Connecting to " + ip + "...\n"));
                s = new Socket(ip, 8888);
                SwingUtilities.invokeLater(() -> logArea.append("Connected to Server!\n"));
            }

            //initialization after connection
            out = new PrintWriter(s.getOutputStream(), true);

            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                String received = line;
                SwingUtilities.invokeLater(() -> logArea.append("Opponent: " + received + "\n"));
            }
        } catch (Exception ex) {
            //go back to menu and show error message
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Connection error: " + ex.getMessage());
                ((CardLayout)cards.getLayout()).show(cards, "MENU");
            });
        }
    }).start();
}

    public static void main(String[] args) { new GUI(); }
}