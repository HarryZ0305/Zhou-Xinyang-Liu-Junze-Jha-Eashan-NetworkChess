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
                    String[] parts = msg.split(",");
                    if (parts.length < 5) {
                        SwingUtilities.invokeLater(() -> logArea.append("Chat: " + msg + "\n"));
                        continue;
                    }
                    int x1 = Integer.parseInt(parts[0]);
                    int y1 = Integer.parseInt(parts[1]);
                    int x2 = Integer.parseInt(parts[2]);
                    int y2 = Integer.parseInt(parts[3]);
                    boolean isWhite = Boolean.parseBoolean(parts[4]);
                    boolean c=game.canMove(x1, y1, x2, y2, isWhite);
                    if(c){
                        SwingUtilities.invokeLater(() -> logArea.append("True"+ "\n"));
                        game.Move(x1, y1, x2, y2, isWhite);
                    }else{
                        SwingUtilities.invokeLater(() -> logArea.append("False"+ "\n"));
                    }
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> logArea.append("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n"));
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) { new GUI(); }
}