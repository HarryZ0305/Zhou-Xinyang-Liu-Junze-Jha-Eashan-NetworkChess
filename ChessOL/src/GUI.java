import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;

public class GUI extends JFrame {
    private JTextArea log = new JTextArea(); // 显示消息记录
    private JTextField input = new JTextField(); // 输入框
    private PrintWriter out;

    public GUI() {
        setTitle("Net Skeleton");
        setSize(640, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // 布局：上面显示，下面输入
        add(new JScrollPane(log), BorderLayout.CENTER);
        add(input, BorderLayout.SOUTH);

        // 回车发送消息
        input.addActionListener(e -> {
            if (out != null) {
                out.println(input.getText()); // 【发送逻辑】
                log.append("Me: " + input.getText() + "\n");
                input.setText("");
            }
        });

        setVisible(true);
        setupConnection(); // 启动联网
    }

    private void setupConnection() {
        String mode = JOptionPane.showInputDialog("Type 's' (Server) or 'c' (Client):");
        
        new Thread(() -> {
            try {
                Socket s;
                if ("s".equals(mode)) {
                    log.append("Waiting for client...\n");
                    s = new ServerSocket(8888).accept();
                } else {
                    s = new Socket("127.0.0.1", 8888);
                }
                
                log.append("Connected!\n");
                out = new PrintWriter(s.getOutputStream(), true);
                
                // 【核心接收逻辑】：死循环监听输入流
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    String received = line;
                    // 回到 UI 线程更新显示
                    SwingUtilities.invokeLater(() -> log.append("Opponent: " + received + "\n"));
                }
            } catch (Exception e) {
                log.append("Error: " + e.getMessage());
            }
        }).start();
    }

    public static void main(String[] args) {
        new GUI(); 
    }
}