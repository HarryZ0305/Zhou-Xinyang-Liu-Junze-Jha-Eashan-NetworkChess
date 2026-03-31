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
    
    // 【新增】保存 64 个按钮，方便收到信息后找到对应的格子
    private JButton[] boardButtons = new JButton[64];
    private boolean isMyTurn = false; // 控制回合

    public GUI() {
        setTitle("ChessOL");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        JPanel menuPanel = createMenuPanel();
        JPanel gamePanel = createGamePanel();

        cards.add(menuPanel, "MENU");
        cards.add(gamePanel, "GAME");

        add(cards);
        setVisible(true);
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        JButton btnServer = new JButton("Create Match (Server)");
        JButton btnClient = new JButton("Join Match (Client)");
        logArea = new JTextArea("Waiting for selection...");
        logArea.setEditable(false);

        // 【修改】Server 通常不需要输 IP，它是被连接的对象
        btnServer.addActionListener(e -> startNetwork(true, null));
        
        btnClient.addActionListener(e -> {
            String ip = JOptionPane.showInputDialog("Input Server IP:", "127.0.0.1");
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
            
            boardButtons[i] = btn; // 【新增】存入数组
            int finalI = i;
            btn.addActionListener(e -> handleMove(finalI));
            panel.add(btn);
        }
        return panel;
    }

    private void startNetwork(boolean isServer, String ip) {
        new Thread(() -> {
            try {
                if (isServer) {
                    logArea.append("\nWaiting for connection...");
                    ServerSocket server = new ServerSocket(8888);
                    socket = server.accept();
                    isMyTurn = true; // 【新增】服务器默认先手
                } else {
                    logArea.append("\nConnecting to: " + ip);
                    socket = new Socket(ip, 8888);
                    isMyTurn = false; // 【新增】客户端默认后手
                }
                
                out = new PrintWriter(socket.getOutputStream(), true);
                // 【新增】获取输入流
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                SwingUtilities.invokeLater(() -> cardLayout.show(cards, "GAME"));
                
                // 【关键新增】启动接收线程
                receiveMessages(in);
                
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> logArea.append("\nError: " + ex.getMessage()));
            }
        }).start();
    }

    // 【新增】这个方法负责“感知”对方的动作
    private void receiveMessages(BufferedReader in) {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("MOVE:")) {
                        int index = Integer.parseInt(line.substring(5));
                        
                        // 在 UI 线程更新棋盘
                        SwingUtilities.invokeLater(() -> {
                            // 暂时用设置文字来模拟落子，之后可以换成图片
                            boardButtons[index].setIcon(new ImageIcon("./ChessPieces/BlackPieces/BlackBishop.png")); 
                            boardButtons[index].setForeground(Color.RED);
                            isMyTurn = true; // 对方下完了，轮到我了
                            setTitle("ChessOL - Your Turn");
                        });
                    }
                }
            } catch (IOException e) {
                System.out.println("Disconnected.");
            }
        }).start();
    }

    private void handleMove(int index) {
        // 【修改】增加回合判断，且不能点已经有子的格子
        if (out != null && isMyTurn && boardButtons[index].getText().equals("")) {
            boardButtons[index].setText("X"); // 本地画下棋子
            boardButtons[index].setForeground(Color.BLUE);
            
            out.println("MOVE:" + index); // 发送给对方
            isMyTurn = false; // 进入等待状态
            setTitle("ChessOL - Opponent's Turn");
        }
    }

    public static void main(String[] args) {
        new GUI();
    }
}