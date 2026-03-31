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

        // 1. 初始菜单界面
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

        // 2. 收发消息界面
        JPanel chat = new JPanel(new BorderLayout());
        logArea.setEditable(false);
        chat.add(new JScrollPane(logArea), BorderLayout.CENTER);
        chat.add(inputField, BorderLayout.SOUTH);

        // 发送逻辑：回车触发
        inputField.addActionListener(e -> {
            if (out != null) {
                String msg = inputField.getText();
                out.println(msg); // 【发送】
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
        new Thread(() -> {
            try {
                Socket s;
                if (isServer) {
                    SwingUtilities.invokeLater(() -> logArea.append("Waiting for connection...\n"));
                    s = new ServerSocket(8888).accept();
                } else {
                    s = new Socket(ip, 8888);
                }

                // 连接成功，切换界面
                out = new PrintWriter(s.getOutputStream(), true);
                SwingUtilities.invokeLater(() -> ((CardLayout)cards.getLayout()).show(cards, "CHAT"));

                // 【接收逻辑】：开启监听死循环
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    String received = line;
                    // 将收到的信息返回到 GUI
                    SwingUtilities.invokeLater(() -> logArea.append("Opponent: " + received + "\n"));
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Connection error: " + ex.getMessage());
            }
        }).start();
    }

    public static void main(String[] args) { new GUI(); }
}