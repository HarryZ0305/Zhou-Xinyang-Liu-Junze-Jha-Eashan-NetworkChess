import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import javax.imageio.ImageIO;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Top-level Swing window for the chess application. Inherits from JFrame and owns every
 * piece of the user-facing state: the menu screen, the game screen (board + dashboard),
 * the network socket, the bot adapter, the chess clock, and the captured-piece panels.
 *
 * The class drives three game modes through a single workflow — local play vs. a Bot,
 * networked host (Server), and networked guest (Client). The board (ActiveBoardPanel)
 * and capture displays (CapturedPanel) are inner classes so they can read the enclosing
 * Game and image cache directly without verbose wiring.
 */
public class GUI extends JFrame {

    // CardLayout lets the same JFrame swap between the main-menu screen and the game screen.
    private JPanel cards = new JPanel(new CardLayout());
    private LogPane logArea = new LogPane(); // Scrolling log holding chat and move history; also the source for exportHistory().
    private JTextField inputField = new JTextField(); // Single-line chat input; Enter triggers a CHAT: packet.
    private ChessNetwork network; // Decoupled network manager.
    Game game = new Game(); // The authoritative model. Re-created on rematch/new connection.
    ActiveBoardPanel activeBoard; // Custom-painted board panel; constructed inside the constructor.
    private HashMap<String, Image> pieceImages = new HashMap<>(); // Cache of sprite images keyed by "<Color><PieceType>".
    private boolean gameOver = false; // Global gate that suppresses input, ticks, and dialogs after end-of-game.
    private boolean playingWhite = true; // Which colour the local client controls; flipped for the joining peer.
    private boolean vsBot = false; // True iff the current game is local-vs-bot rather than networked.
    private JLabel statusLabel = new JLabel("STATUS: WAITING...", SwingConstants.CENTER);

    private JLabel whiteNameLabel = new JLabel("White");
    private JLabel blackNameLabel = new JLabel("Black");
    private JLabel whiteClockLabel = new JLabel("10:00");
    private JLabel blackClockLabel = new JLabel("10:00");
    private JPanel whitePlayerCard;
    private JPanel blackPlayerCard;
    private long whiteTimeMs = 0; // Remaining millis on White's clock; counted down by tickClock().
    private long blackTimeMs = 0; // Remaining millis on Black's clock.
    private final java.util.Random botClockRng = new java.util.Random(); // RNG that fakes a humanlike thinking time for the bot.
    private long lastTickMs = 0; // Wall-clock timestamp of the previous tick — used to compute elapsed delta.
    private Timer chessClock; // Swing Timer firing every 200 ms on the EDT.
    private static final long INITIAL_TIME_MS = 10L * 60 * 1000; // 10 minutes per side — standard rapid-game budget.

    private String localPlayerName;
    private String opponentPlayerName;

    CapturedPanel whiteCapturedPanel;
    CapturedPanel blackCapturedPanel;

    /**
     * Constructor — assembles the entire UI. Loads the piece sprite images, builds the
     * menu screen (with a custom-painted battle background), builds the game screen
     * (board on the left, dashboard with player cards, captured pieces, status, chat
     * log and action buttons on the right), and wires every button/input listener.
     */
    public GUI() {
        loadImages();
        setTitle("ChessOL");
        setSize(900, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel menu = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBattleBackground((Graphics2D) g, getWidth(), getHeight());
            }
        };
        menu.setOpaque(true);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;

        JLabel title = new JLabel("ChessOL", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 38));
        title.setForeground(new Color(255, 235, 180));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        menu.add(title, gbc);

        JButton btnServer = new JButton("Server");
        JButton btnClient = new JButton("Client");
        JButton btnBot    = new JButton("Play with Bot");

        for (JButton btn : new JButton[]{btnServer, btnClient, btnBot}) {
            btn.setBackground(new Color(60, 30, 10, 200));
            btn.setForeground(new Color(255, 235, 180));
            btn.setFont(new Font("Serif", Font.BOLD, 16));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 160, 80), 2),
                BorderFactory.createEmptyBorder(6, 18, 6, 18)
            ));
    
            // Add these two lines:
            btn.setContentAreaFilled(false); 
            btn.setOpaque(false); 
    
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        btnServer.addActionListener(e -> {
            String portStr = JOptionPane.showInputDialog("Host on Port:", "8888");
            if (portStr != null) {
                try {
                    int port = Integer.parseInt(portStr);
                    startNetwork(true, null, port); // Update your startNetwork method to accept a port parameter!
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid Port Number.");
                }
            }
        });

        btnClient.addActionListener(e -> {
            String ip = JOptionPane.showInputDialog("Host IP:", "127.0.0.1");
            if (ip != null) {
                String portStr = JOptionPane.showInputDialog("Connect to Port:", "8888");
                if (portStr != null) {
                    try {
                        int port = Integer.parseInt(portStr);
                        startNetwork(false, ip, port);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Invalid Port Number.");
                    }
                }
            }
        });
        btnBot.addActionListener(e -> startBotGame());

        gbc.gridwidth = 1; gbc.gridy = 1; gbc.gridx = 0;
        menu.add(btnServer, gbc);
        gbc.gridx = 1;
        menu.add(btnClient, gbc);
        gbc.gridwidth = 2; gbc.gridy = 2; gbc.gridx = 0;
        menu.add(btnBot, gbc);

        JPanel workPanel = new JPanel(new GridBagLayout());
        workPanel.setBackground(new Color(30, 32, 36)); // Dark Slate

        //Left: Board Container
        JPanel boardContainer = new JPanel(new GridBagLayout()); 
        boardContainer.setBackground(new Color(30, 32, 36));
        activeBoard = new ActiveBoardPanel();
        activeBoard.setPreferredSize(new Dimension(500, 500));
        activeBoard.setMinimumSize(new Dimension(400, 400));
        
        GridBagConstraints bc = new GridBagConstraints();
        bc.weightx = 1.0; bc.weighty = 1.0; bc.fill = GridBagConstraints.BOTH;
        bc.insets = new Insets(20, 20, 20, 20);
        boardContainer.add(activeBoard, bc);

        //Right: Dashboard Container
        JPanel dashboardPanel = new JPanel(new BorderLayout(0, 10));
        dashboardPanel.setBackground(new Color(45, 55, 72));
        dashboardPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(74, 85, 104)), 
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        //Dashboard top section
        JPanel topContainer = new JPanel(new BorderLayout(0, 10));
        topContainer.setOpaque(false);
        
        whitePlayerCard = buildPlayerCard(whiteNameLabel, whiteClockLabel);
        blackPlayerCard = buildPlayerCard(blackNameLabel, blackClockLabel);

        //1. Player Cards go at the very top
        JPanel topInfo = new JPanel();
        topInfo.setOpaque(false);
        topInfo.setLayout(new BoxLayout(topInfo, BoxLayout.Y_AXIS));
        topInfo.add(blackPlayerCard);
        topInfo.add(Box.createVerticalStrut(6));
        topInfo.add(whitePlayerCard);
        topContainer.add(topInfo, BorderLayout.NORTH);

        //2. Status Label and Captured Pieces go below the cards
        JPanel statusAndCaptures = new JPanel(new BorderLayout(0, 5));
        statusAndCaptures.setOpaque(false);
        
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        statusLabel.setForeground(new Color(247, 250, 252));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(26, 32, 44));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        statusAndCaptures.add(statusLabel, BorderLayout.NORTH);

        whiteCapturedPanel = new CapturedPanel(true);
        blackCapturedPanel = new CapturedPanel(false);
        JPanel capturedContainer = new JPanel(new GridLayout(2, 1, 0, 5));
        capturedContainer.setOpaque(false);
        capturedContainer.add(whiteCapturedPanel); 
        capturedContainer.add(blackCapturedPanel); 
        statusAndCaptures.add(capturedContainer, BorderLayout.CENTER);

        topContainer.add(statusAndCaptures, BorderLayout.CENTER);
        
        //Add the unified container safely to the dashboard
        dashboardPanel.add(topContainer, BorderLayout.NORTH);

        //Middle: Chat & Logs
        logArea.setEditable(false);
        logArea.setBackground(new Color(26, 32, 44));
        logArea.setForeground(new Color(208, 212, 219));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        ((javax.swing.text.DefaultCaret)logArea.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.ALWAYS_UPDATE);
        
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createLineBorder(new Color(74, 85, 104), 1));
        
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabs.addTab("CHAT & MOVES", logScroll);
        dashboardPanel.add(tabs, BorderLayout.CENTER);

        //Bottom: Input & Actions
        JPanel controlPanel = new JPanel(new BorderLayout(0, 10));
        controlPanel.setOpaque(false);

        inputField.setBackground(new Color(26, 32, 44));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(74, 85, 104), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        controlPanel.add(inputField, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setOpaque(false);
        JButton btnDraw = new JButton("🤝 Offer Draw");
        JButton btnResign = new JButton("🏳️ Resign");
        
        for (JButton btn : new JButton[]{btnDraw, btnResign}) {
            btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btn.setBackground(new Color(74, 85, 104));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            buttonPanel.add(btn);
        }
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);
        dashboardPanel.add(controlPanel, BorderLayout.SOUTH);

        GridBagConstraints gbcWork = new GridBagConstraints();
        gbcWork.gridx = 0; gbcWork.gridy = 0;
        gbcWork.weightx = 0.65; gbcWork.weighty = 1.0;
        gbcWork.fill = GridBagConstraints.BOTH;
        workPanel.add(boardContainer, gbcWork);

        gbcWork.gridx = 1;
        gbcWork.weightx = 0.35;
        workPanel.add(dashboardPanel, gbcWork);

        btnResign.addActionListener(e -> {
            if (network != null && !gameOver && !vsBot) {
                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to resign?", "Resign", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    network.sendMessage("GAME:RESIGN");
                    showGameOverDialog("You resigned. Game Over.");
                }
            } else if (vsBot && !gameOver) {
                showGameOverDialog("You resigned. Bot wins.");
            }
        });

        btnDraw.addActionListener(e -> {
            if (network != null && !gameOver && !vsBot) {
                network.sendMessage("GAME:DRAW_OFFER");
                logArea.append("System: Draw offer sent...\n");
            } else if (vsBot && !gameOver) {
                logArea.append("System: Bot accepted your draw offer.\n");
                showGameOverDialog("Draw agreed.");
            }
        });

        inputField.addActionListener(e -> {
            if (network != null) {
                String msg = inputField.getText();
                network.sendMessage("CHAT:" + msg);
                logArea.append("You: " + msg + "\n");
                inputField.setText("");
            }
        });

        cards.add(menu, "MENU");
        cards.add(workPanel, "WORK");
        add(cards);
        setVisible(true);

    }
    
    /**
     * Refreshes the STATUS banner to show whose turn it is from this client's perspective.
     * Marshalled onto the Event Dispatch Thread because it may be called from the
     * networking thread, and Swing components are not thread-safe.
     */
    public void updateStatus() {
        if (gameOver){
            return;
        } 
        
        SwingUtilities.invokeLater(() -> {
            if (game.whiteTurn == playingWhite) {
                statusLabel.setText("STATUS: YOUR TURN");
                statusLabel.setForeground(new Color(255, 87, 34));
            } else {
                statusLabel.setText("STATUS: OPPONENT'S TURN");
                statusLabel.setForeground(new Color(208, 212, 219));
            }
        });
    }

    /**
     * Custom paints a layered "medieval battle" backdrop behind the main menu using
     * Java 2D primitives — gradient sky/ground, a glowing sun, smoke plumes, opposing
     * armies of foot soldiers and horsemen, a clashing pair at centre, fallen soldiers,
     * embedded arrows, a vignette, and a decorative gold border. Rendered entirely
     * procedurally so no extra image assets are required at runtime.
     */
    private void drawBattleBackground(Graphics2D g2, int W, int H) {
        if (W == 0 || H == 0){  //RadialGradientPaint throws if radius <= 0
            return;
        } 
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int ground = (int)(H * 0.72f);

        GradientPaint sky = new GradientPaint(
            0, 0,      new Color(20, 10, 5),
            0, ground, new Color(185, 65, 10));
        g2.setPaint(sky);
        g2.fillRect(0, 0, W, H);

        int sunX = (int)(W * 0.72f), sunY = ground - 18;
        g2.setColor(new Color(220, 60, 20, 180));
        g2.fillOval(sunX - 30, sunY - 30, 60, 60);
        for (int r = 1; r <= 4; r++) {
            g2.setColor(new Color(220, 80, 10, 45 - r * 10));
            g2.setStroke(new BasicStroke(r * 3f));
            g2.drawOval(sunX - 30 - r*10, sunY - 30 - r*10, 60 + r*20, 60 + r*20);
        }

        drawSmoke(g2, (int)(W*0.12f), ground - 10);
        drawSmoke(g2, (int)(W*0.85f), ground - 8);
        drawSmoke(g2, (int)(W*0.35f), ground - 5);

        GradientPaint gnd = new GradientPaint(
            0, ground, new Color(45, 32, 12),
            0, H,      new Color(22, 15, 5));
        g2.setPaint(gnd);
        g2.fillRect(0, ground, W, H - ground);

        g2.setColor(new Color(30, 20, 8, 100));
        g2.setStroke(new BasicStroke(1f));
        int[][] tufts = {{40,8},{110,14},{190,6},{280,11},{360,7},{430,13},{80,18},{230,16},{400,9}};
        for (int[] t : tufts) {
            int tx = t[0] * W / 500, ty = ground + t[1];
            g2.drawLine(tx, ty, tx + 8, ty - 4);
            g2.drawLine(tx + 4, ty, tx + 12, ty - 5);
        }

        g2.setColor(new Color(35, 20, 8, 160));
        int[] hx = {0, W/6, W/4, W*2/5, W/2, W*3/5, W*3/4, W*5/6, W, W, 0};
        int[] hy = {ground, ground-28, ground-40, ground-22, ground-50,
                    ground-30, ground-44, ground-18, ground-35, H, H};
        g2.fillPolygon(hx, hy, hx.length);

        Color whiteShield = new Color(230, 230, 230);
        Color whiteArmour = new Color(180, 180, 190);
        int[] leftXs = {20, 55, 90, 130, 168, 210};
        for (int i = 0; i < leftXs.length; i++) {
            int x = leftXs[i] * W / 500;
            boolean hasAxe   = (i % 3 == 0);
            boolean hasSword = (i % 3 == 1);
            drawFootSoldier(g2, x, ground, true, hasAxe, hasSword, !hasAxe && !hasSword,
                            whiteShield, whiteArmour);
        }
        drawHorseman(g2, (int)(W * 0.09f), ground,     true, whiteArmour, true);
        drawHorseman(g2, (int)(W * 0.20f), ground - 4, true, whiteArmour, false);

        Color blackShield = new Color(30, 30, 30);
        Color blackArmour = new Color(55, 55, 60);
        int[] rightXs = {480, 445, 410, 370, 330, 295};
        for (int i = 0; i < rightXs.length; i++) {
            int x = rightXs[i] * W / 500;
            boolean hasAxe   = (i % 3 == 2);
            boolean hasSword = (i % 3 == 0);
            drawFootSoldier(g2, x, ground, false, hasAxe, hasSword, !hasAxe && !hasSword,
                            blackShield, blackArmour);
        }
        drawHorseman(g2, (int)(W * 0.91f), ground,     false, blackArmour, false);
        drawHorseman(g2, (int)(W * 0.79f), ground - 4, false, blackArmour, true);

        int mid = W / 2;
        drawFootSoldier(g2, mid - 26, ground + 2, true,  false, true, false, whiteShield, whiteArmour);
        drawFootSoldier(g2, mid + 14, ground + 2, false, false, true, false, blackShield, blackArmour);
        drawClashSparks(g2, mid - 4, ground - 28);

        drawFallenSoldier(g2, (int)(W * 0.38f), ground + 2);
        drawFallenSoldier(g2, (int)(W * 0.62f), ground + 2);

        drawArrowInGround(g2, (int)(W*0.28f), ground + 4, -65);
        drawArrowInGround(g2, (int)(W*0.55f), ground + 2, -80);
        drawArrowInGround(g2, (int)(W*0.67f), ground + 5, -70);

        RadialGradientPaint vignette = new RadialGradientPaint(
            new java.awt.geom.Point2D.Float(W / 2f, H / 2f),
            Math.max(W, H) * 0.72f,
            new float[]{0.4f, 1.0f},
            new Color[]{new Color(0,0,0,0), new Color(0,0,0,180)});
        g2.setPaint(vignette);
        g2.fillRect(0, 0, W, H);

        g2.setPaint(new Color(0, 0, 0, 70));
        g2.fillRect(0, 0, W, H);

        g2.setColor(new Color(190, 140, 45, 180));
        g2.setStroke(new BasicStroke(3f));
        g2.drawRect(4, 4, W - 9, H - 9);
        g2.setColor(new Color(190, 140, 45, 80));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(9, 9, W - 19, H - 19);
        for (int[] c : new int[][]{{4,4},{W-20,4},{4,H-20},{W-20,H-20}}) {
            g2.setColor(new Color(200, 160, 60, 200));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(c[0], c[1], 16, 16);
        }
    }

    private void drawSmoke(Graphics2D g2, int x, int y) {
        for (int i = 0; i < 6; i++) {
            int alpha = 80 - i * 12;
            int r = 10 + i * 8;
            int ox = (i % 2 == 0) ? -3 : 4;
            g2.setColor(new Color(30, 25, 20, Math.max(alpha, 5)));
            g2.fillOval(x - r/2 + ox, y - i * 18 - r/2, r, r);
        }
    }

    private void drawClashSparks(Graphics2D g2, int x, int y) {
        Color[] sparkColors = {
            new Color(255, 230, 50, 220),
            new Color(255, 160, 20, 180),
            new Color(255, 255, 200, 200)
        };
        int[][] sparks = {{-12,-8},{-7,-14},{0,-16},{8,-13},{13,-7},{-5,-4},{6,-5},{-9,-3},{10,-2}};
        g2.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < sparks.length; i++) {
            g2.setColor(sparkColors[i % sparkColors.length]);
            int sx = x + sparks[i][0], sy = y + sparks[i][1];
            g2.drawLine(x, y, sx, sy);
            g2.fillOval(sx - 2, sy - 2, 4, 4);
        }
    }

    private void drawArrowInGround(Graphics2D g2, int x, int y, int angleDeg) {
        Graphics2D g3 = (Graphics2D) g2.create();
        g3.translate(x, y);
        g3.rotate(Math.toRadians(angleDeg));
        g3.setColor(new Color(100, 70, 30));
        g3.setStroke(new BasicStroke(2f));
        g3.drawLine(0, 0, 0, -22);
        g3.setColor(new Color(160, 160, 170));
        int[] ax = {-3, 0, 3}, ay = {-22, -28, -22};
        g3.fillPolygon(ax, ay, 3);
        g3.setColor(new Color(180, 50, 30));
        g3.drawLine(-3, -4, 0, 0);
        g3.drawLine( 3, -4, 0, 0);
        g3.dispose();
    }

    private void drawFallenSoldier(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(25, 18, 10, 200));
        g2.fillOval(x, y - 5, 28, 10);
        g2.fillOval(x + 22, y - 8, 10, 10);
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(x + 6, y - 2, x - 4, y + 4);
        g2.setColor(new Color(130, 130, 140, 160));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(x - 6, y + 3, x + 14, y - 1);
    }

    private void drawFootSoldier(Graphics2D g2, int x, int groundY,
                                  boolean facingRight,
                                  boolean hasAxe, boolean hasSword, boolean hasSpear,
                                  Color shieldColor, Color armourColor) {
        int dir = facingRight ? 1 : -1;

        g2.setColor(armourColor.darker());
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(x,     groundY - 6, x - dir*3, groundY);
        g2.drawLine(x,     groundY - 6, x + dir*3, groundY);

        g2.setColor(armourColor);
        g2.fillRoundRect(x - 5, groundY - 22, 10, 16, 3, 3);

        int shieldX = x - dir * 9;
        g2.setColor(shieldColor);
        g2.fillRoundRect(shieldX - 4, groundY - 26, 9, 18, 3, 3);
        g2.setColor(shieldColor.darker());
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(shieldX - 4, groundY - 26, 9, 18, 3, 3);
        g2.setColor(new Color(200, 170, 60));
        g2.fillOval(shieldX - 2, groundY - 18, 5, 5);

        g2.setColor(new Color(200, 180, 130));
        g2.fillOval(x - 5, groundY - 34, 10, 10);
        g2.setColor(armourColor.brighter());
        g2.fillRect(x - 5, groundY - 36, 10, 7);
        g2.fillRect(x - 6, groundY - 36, 12, 3);
        g2.setColor(armourColor);
        g2.fillRect(x - 1, groundY - 33, 2, 5);

        int armStartX = x + dir * 5;
        int armStartY = groundY - 18;

        if (hasSpear) {
            int spearTipX = armStartX + dir * 22;
            int spearTipY = armStartY - 30;
            g2.setColor(new Color(110, 80, 40));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(armStartX, armStartY, spearTipX, spearTipY);
            g2.setColor(new Color(170, 170, 180));
            int[] sx = {spearTipX - 3, spearTipX,      spearTipX + 3};
            int[] sy = {spearTipY + 4, spearTipY - 10, spearTipY + 4};
            g2.fillPolygon(sx, sy, 3);
        } else if (hasSword) {
            g2.setColor(new Color(110, 80, 40));
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(armStartX, armStartY, armStartX + dir*4, armStartY - 8);
            g2.setColor(new Color(190, 190, 200));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(armStartX + dir*4, armStartY - 8, armStartX + dir*10, armStartY - 28);
            g2.setColor(new Color(160, 130, 50));
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(armStartX + dir*2, armStartY - 6, armStartX + dir*6, armStartY - 10);
        } else if (hasAxe) {
            int axeEndX = armStartX + dir * 16;
            int axeEndY = armStartY - 20;
            g2.setColor(new Color(100, 70, 30));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawLine(armStartX, armStartY, axeEndX, axeEndY);
            g2.setColor(new Color(160, 160, 170));
            int[] ax2 = {axeEndX - 2, axeEndX + dir*8, axeEndX + dir*8, axeEndX - 2};
            int[] ay2 = {axeEndY - 10, axeEndY - 14, axeEndY + 2, axeEndY - 2};
            g2.fillPolygon(ax2, ay2, 4);
        }
    }

    private void drawHorseman(Graphics2D g2, int x, int groundY,
                               boolean facingRight, Color armourColor, boolean withSpear) {
        int dir = facingRight ? 1 : -1;
        Color horseColor = new Color(80, 50, 25);
        Color hairColor  = new Color(40, 25, 10);

        g2.setColor(horseColor);
        g2.fillOval(x - 20, groundY - 32, 44, 22);
        int[] neckX = {x + dir*10, x + dir*18, x + dir*22, x + dir*14};
        int[] neckY = {groundY - 38, groundY - 40, groundY - 28, groundY - 26};
        g2.fillPolygon(neckX, neckY, 4);
        g2.fillOval(x + dir*16, groundY - 44, 14, 12);
        g2.setColor(new Color(50, 30, 15));
        g2.fillOval(x + dir*22, groundY - 37, 3, 3);
        g2.setColor(new Color(20, 10, 5));
        g2.fillOval(x + dir*21, groundY - 43, 3, 3);
        g2.setColor(hairColor);
        g2.setStroke(new BasicStroke(2f));
        for (int m = 0; m < 4; m++) {
            g2.drawLine(x + dir*(12+m*2), groundY - 40 - m,
                        x + dir*(10+m*2), groundY - 48 - m*2);
        }
        g2.drawLine(x - dir*19, groundY - 30, x - dir*26, groundY - 38);
        g2.drawLine(x - dir*19, groundY - 30, x - dir*28, groundY - 30);
        g2.drawLine(x - dir*19, groundY - 30, x - dir*25, groundY - 22);
        g2.setColor(horseColor.darker());
        g2.setStroke(new BasicStroke(4f));
        g2.drawLine(x - 10, groundY - 16, x - 14, groundY);
        g2.drawLine(x - 5,  groundY - 16, x + 2,  groundY);
        g2.drawLine(x + 8,  groundY - 16, x + 4,  groundY);
        g2.drawLine(x + 14, groundY - 16, x + 20, groundY);
        g2.setColor(new Color(30, 20, 10));
        for (int hx2 : new int[]{x-14, x+2, x+4, x+20}) g2.fillOval(hx2-3, groundY-3, 6, 5);
        g2.setColor(armourColor.darker());
        g2.fillOval(x - 4, groundY - 40, 20, 12);

        int rX = x + dir * 2, rY = groundY - 50;
        g2.setColor(armourColor);
        g2.fillRoundRect(rX - 6, rY - 14, 12, 16, 3, 3);
        g2.setColor(armourColor.darker());
        g2.setStroke(new BasicStroke(4f));
        g2.drawLine(rX - 3, rY + 2, rX - 12, rY + 14);
        g2.drawLine(rX + 3, rY + 2, rX + 12, rY + 14);
        g2.setColor(new Color(200, 175, 130));
        g2.fillOval(rX - 5, rY - 26, 10, 10);
        g2.setColor(new Color(130, 130, 145));
        g2.fillRect(rX - 5, rY - 28, 10, 8);
        g2.fillRect(rX - 7, rY - 28, 14, 3);
        g2.fillRect(rX - 1, rY - 25, 2, 5);

        if (withSpear) {
            int spearEndX = rX + dir * 40, spearEndY = rY - 5;
            int spearButX = rX - dir * 15, spearButY = rY;
            g2.setColor(new Color(110, 80, 35));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawLine(spearButX, spearButY, spearEndX, spearEndY);
            g2.setColor(new Color(180, 180, 190));
            int[] spx = {spearEndX - dir*3, spearEndX + dir*10, spearEndX - dir*3};
            int[] spy = {spearEndY - 4, spearEndY, spearEndY + 4};
            g2.fillPolygon(spx, spy, 3);
        } else {
            g2.setColor(new Color(100, 75, 30));
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(rX + dir*6, rY - 14, rX + dir*14, rY - 32);
            g2.setColor(new Color(185, 185, 200));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(rX + dir*14, rY - 32, rX + dir*18, rY - 46);
            g2.setColor(new Color(160, 130, 50));
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(rX + dir*10, rY - 26, rX + dir*18, rY - 30);
        }

        int sX = rX - dir * 9;
        g2.setColor(armourColor);
        g2.fillRoundRect(sX - 5, rY - 20, 10, 18, 4, 4);
        g2.setColor(armourColor.darker());
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(sX - 5, rY - 20, 10, 18, 4, 4);
        g2.setColor(new Color(200, 165, 55));
        g2.fillOval(sX - 2, rY - 12, 5, 5);
    }

    /**
     * Drives the lifecycle of a single human move. Validates the move against the Game's
     * rule engine, prompts the player to choose a promotion piece if a pawn reaches the
     * back rank, commits the move locally, flips the turn flag, repaints the board and
     * captured panels, checks for game-over (mate/stalemate), and — depending on mode —
     * either triggers the bot reply or transmits a MOVE packet to the peer over the
     * socket. The transmitted packet also carries a hash of the resulting board state
     * so the receiver can detect desynchronisation.
     */
    private void attemptMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (!vsBot && network == null) {
            logArea.append("System: Not connected yet.\n");
            return;
        }

        Piece p = game.board[fromRow][fromCol];
        if (p == null) return;
        boolean isWhite = p.isWhite;

        // Delegate to the rule engine so the GUI never bypasses any of Game's safety checks.
        if (!game.canMove(fromRow, fromCol, toRow, toCol, isWhite)) {
            logArea.append("Invalid move\n");
            return;
        }

        // Detect promotion: a pawn arriving on the opposite back rank must be replaced.
        String pawnPromotion = "None";
        if (game.board[fromRow][fromCol] instanceof Pawn && ((toRow == 0 && isWhite) || (toRow == 7 && !isWhite))) {

            // Graphical Promotion Menu — show the four legal promotion targets as scaled
            // sprite icons so the player picks visually rather than typing a letter.
            String colorStr = isWhite ? "White" : "Black";
            Object[] options = {
                new ImageIcon(pieceImages.get(colorStr + "Queen").getScaledInstance(60, 60, Image.SCALE_SMOOTH)),
                new ImageIcon(pieceImages.get(colorStr + "Rook").getScaledInstance(60, 60, Image.SCALE_SMOOTH)),
                new ImageIcon(pieceImages.get(colorStr + "Bishop").getScaledInstance(60, 60, Image.SCALE_SMOOTH)),
                new ImageIcon(pieceImages.get(colorStr + "Knight").getScaledInstance(60, 60, Image.SCALE_SMOOTH))
            };
            
            int choice = JOptionPane.showOptionDialog(this,
                "Choose a piece to promote to:", "Pawn Promotion",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

            // Map the dialog's index to the single-character promotion code expected by Game.promotion().
            if (choice == 1) pawnPromotion = "R";
            else if (choice == 2) pawnPromotion = "B";
            else if (choice == 3) pawnPromotion = "N";
            else pawnPromotion = "Q"; // Defaults to Queen if user closes dialog without choosing — almost always the right pick.
        }

        String san = game.toAlgebraic(fromRow, fromCol, toRow, toCol, isWhite, pawnPromotion);
        String formattedMove = game.formatMove(san);
        game.sanMoves.add(san);

        game.Move(fromRow, fromCol, toRow, toCol, isWhite); // Commit the move to the shared Game model.
        if (!pawnPromotion.equals("None")) {
            game.promotion(toRow, toCol, isWhite, pawnPromotion); // Replace the pawn on the back rank with the chosen piece.
        }

        boolean givesCheck = game.isInCheck(!isWhite); //Test whether the OPPONENT's king is now in check.
        if (givesCheck) logArea.append("Check!\n", true);

        game.whiteTurn = !game.whiteTurn; //Hand the turn over so canMove() will reject moves from the moved side.
        game.recordState();
        activeBoard.repaint(); //Trigger paintComponent to redraw the new board state.
        whiteCapturedPanel.repaint(); //Refresh capture displays in case this move took a piece.
        blackCapturedPanel.repaint();
        announceEndIfOver(); //Detect mate or stalemate before the next click is accepted.

        if (vsBot) {
            logArea.append(formattedMove + "\n");
            if (!gameOver) triggerBotMove();
        } else {
            String message = "MOVE:" + fromRow + "," + fromCol + "," + toRow + "," + toCol + "," + isWhite + "," + pawnPromotion + "," + givesCheck + "," + game.stateSignature().hashCode();
            network.sendMessage(message);
            
            if (network.isSendFailed()) logArea.append("System: Send failed, connection lost.\n");
            else logArea.append(formattedMove + "\n");
        }
    }

    /**
     * Resets state for a fresh game against the local bot. Sets vsBot, instantiates a new
     * Game in the standard starting position, renames the black player to "Bot", swaps
     * to the WORK card, repaints, and starts the chess clock.
     */
    private void startBotGame() {
        vsBot = true;
        playingWhite = true;
        game = new Game();
        game.blackPlayer.name = "Bot";
        gameOver = false;
        logArea.setText("");
        ((CardLayout)cards.getLayout()).show(cards, "WORK");
        logArea.append("Game started! You are White. Bot is thinking as Black...\n");
        activeBoard.repaint();
        whiteCapturedPanel.repaint();
        blackCapturedPanel.repaint();
        startGameClock();
    }

    /**
     * Runs the bot's minimax search on a background worker thread so the search does
     * not freeze the Event Dispatch Thread (which would lock the entire UI). When a
     * move is found, the actual state mutations and repaints are marshalled back onto
     * the EDT via invokeLater — Swing components must only be touched on that thread.
     * The bot's clock is then debited a simulated "thinking time" so its remaining
     * time feels realistic even though the search itself returns nearly instantly.
     */
    private void triggerBotMove() {
        new Thread(() -> { // Worker thread keeps the EDT free so the UI never freezes during search.
            SwingUtilities.invokeLater(() -> logArea.append("Bot is thinking...\n"));

            int[] move = ChessBot.getMove(game); // Heavy compute happens here, OFF the EDT.

            if (move == null) {
                SwingUtilities.invokeLater(() -> logArea.append("Bot has no legal moves.\n"));
                return;
            }

            //Copy primitives into final locals so the lambda below can capture them safely.
            final int fR = move[0], fC = move[1], tR = move[2], tC = move[3];
            final int promoChar = move[4];
            SwingUtilities.invokeLater(() -> { //Bounce mutations back onto the EDT
                String promo = promoChar != -1 ? String.valueOf((char) promoChar) : "None";
                String san = game.toAlgebraic(fR, fC, tR, tC, false, promo);
                String formattedMove = game.formatMove(san);
                game.sanMoves.add(san);

                game.Move(fR, fC, tR, tC, false); //The bot always plays Black in this app.
                if (!promo.equals("None")) game.promotion(tR, tC, false, promo);
                if (game.isInCheck(true)) logArea.append("Check!\n", true); 
                game.whiteTurn = !game.whiteTurn;
                game.recordState();

                //Bot plays instantly, but we charge its clock a realistic chunk of time.
                long used = botThinkMillis();
                blackTimeMs -= used; 
                if (blackTimeMs <= 0) { 
                    blackTimeMs = 0;
                    updateClockLabels();
                    stopGameClock();
                    showGameOverDialog("Black ran out of time. White wins.");
                    return;
                }
                updateClockLabels();

                activeBoard.repaint();
                whiteCapturedPanel.repaint();
                blackCapturedPanel.repaint();
                logArea.append("Bot: " + formattedMove + " (used " + String.format("%.1f", used / 1000.0) + "s)\n");
                announceEndIfOver();
            });
        }).start();
    }

    /**
     * Final-screen dispatcher when the game ends (mate, stalemate, resignation, timeout,
     * or draw agreed). Flags gameOver to freeze further input, stops the clock, and
     * shows a modal dialog offering Rematch / Export History / Main Menu. The dialog
     * loops on Export History so the player can export and then still pick another
     * action; rematch sends a network REMATCH:REQUEST when applicable.
     */
    private void showGameOverDialog(String endMessage) {
        logArea.append(endMessage + "\n");
        gameOver = true;
        stopGameClock();

        SwingUtilities.invokeLater(() -> {
            String[] options = {"Rematch", "Export PGN", "Export Log", "Main Menu"};
            while (true) {
                int choice = JOptionPane.showOptionDialog(this,
                    endMessage + "\nWhat would you like to do?",
                    "Game Over",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    options,
                    "Rematch");

                if (choice == 0) {
                    if (vsBot) startBotGame();
                    else if (network != null) {
                        network.sendMessage("REMATCH:REQUEST");
                        logArea.append("System: Rematch requested. Waiting for opponent...\n");
                    }
                    break;
                } else if (choice == 1) {
                    exportPGN();
                } else if (choice == 2) {
                    exportHistory();
                } else {
                    if (!vsBot && network != null) network.sendMessage("REMATCH:DECLINE");
                    returnToMenu();
                    break;
                }
            }
        });
    }

    private void exportPGN() {
        File dir = new File("history");
        dir.mkdirs();
        String ts = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File file = new File(dir, "game_" + ts + ".pgn");
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.print(game.exportToPGN());
            JOptionPane.showMessageDialog(this,
                "Saved PGN to " + file.getPath(), "Exported PGN", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "PGN Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Writes the contents of the chat/move log to a uniquely named text file under a
     * "history/" directory. File I/O implementation: the directory is created lazily
     * via mkdirs() so the first export works on a fresh install, the filename is
     * stamped with the current date and time so consecutive exports never overwrite
     * each other, and the PrintWriter/FileWriter pair is opened in a try-with-resources
     * block so the file handle is always released — even if the write throws. Success
     * and failure are both reported back to the user through a modal dialog.
     */
    private void exportHistory() {
        File dir = new File("history");
        dir.mkdirs();
        String ts = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File file = new File(dir, "game_" + ts + ".txt");
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.print(logArea.getText());
            JOptionPane.showMessageDialog(this,
                "Saved to " + file.getPath(), "Exported", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Constructs one of the two side-by-side "player cards" on the dashboard, each
     * showing a name on the left and a clock on the right inside a bordered panel.
     */
    private JPanel buildPlayerCard(JLabel nameLbl, JLabel clockLbl) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(26, 32, 44));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(74, 85, 104), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        nameLbl.setForeground(new Color(247, 250, 252));
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        clockLbl.setForeground(new Color(208, 212, 219));
        clockLbl.setFont(new Font("Consolas", Font.BOLD, 18));

        card.add(nameLbl, BorderLayout.WEST);
        card.add(clockLbl, BorderLayout.EAST);
        return card;
    }

    /**
     * Initialises both players' clocks to the standard 10-minute budget and starts a
     * recurring Swing Timer that ticks every 200 ms. The Timer fires tickClock() on the
     * Event Dispatch Thread so all clock-related UI updates are automatically thread-safe.
     */
    private void startGameClock() {
        whiteTimeMs = INITIAL_TIME_MS;
        blackTimeMs = INITIAL_TIME_MS;
        lastTickMs = System.currentTimeMillis();
        refreshPlayerNames();
        updateClockLabels();
        highlightActivePlayer();
        if (chessClock == null) {
            chessClock = new Timer(200, e -> tickClock());
            chessClock.setRepeats(true);
        }
        chessClock.start();
    }

    private void stopGameClock() {
        if (chessClock != null) chessClock.stop();
    }

    /**
     * Per-tick chess-clock update. Computes the elapsed wall-clock time since the last
     * tick (capped at 5 s so a modal dialog or window freeze doesn't accidentally burn
     * the whole budget), then subtracts that delta from whichever side is on move. If
     * the active side's time hits zero the game ends with a timeout result. In bot mode
     * the bot's clock is debited inside triggerBotMove() instead, so only the human's side counts down here.
     */
    private void tickClock() {
        if (game == null) return;
        long now = System.currentTimeMillis();
        long delta = Math.min(now - lastTickMs, 5000); // cap freeze/dialog pauses
        lastTickMs = now;
        if (gameOver) return;

        if (game.whiteTurn) {
            whiteTimeMs -= delta;
            if (whiteTimeMs <= 0) {
                whiteTimeMs = 0;
                updateClockLabels();
                stopGameClock();
                showGameOverDialog("White ran out of time. Black wins.");
                return;
            }
        } else if (!vsBot) {
            blackTimeMs -= delta;
            if (blackTimeMs <= 0) {
                blackTimeMs = 0;
                updateClockLabels();
                stopGameClock();
                showGameOverDialog("Black ran out of time. White wins.");
                return;
            }
        }
        updateClockLabels();
        highlightActivePlayer();
    }

    //Simulated time the bot "spends" per move: 1–10s, weighted toward 3–5s.
    private long botThinkMillis() {
        double seconds;
        if (botClockRng.nextDouble() < 0.65) {
            seconds = 3.0 + botClockRng.nextDouble() * 2.0; //65% of the time: 3.0–5.0s
        } else {
            seconds = 1.0 + botClockRng.nextDouble() * 9.0; //35% of the time: 1.0–10.0s
        }
        return Math.round(seconds * 1000.0);
    }

    private void refreshPlayerNames() {
        if (game == null) return;
        String wn = game.whitePlayer != null ? game.whitePlayer.name : "White";
        String bn = game.blackPlayer != null ? game.blackPlayer.name : "Black";
        whiteNameLabel.setText("♔ " + wn);
        blackNameLabel.setText("♚ " + bn);
    }

    // Assign localPlayerName to whichever colour I am playing as.
    private void applyMyName() {
        if (localPlayerName == null || game == null) return;
        if (playingWhite) game.whitePlayer.name = localPlayerName;
        else              game.blackPlayer.name = localPlayerName;
    }

    // Assign opponentPlayerName to the colour opposite to me.
    private void applyOpponentName() {
        if (opponentPlayerName == null || game == null) return;
        if (playingWhite) game.blackPlayer.name = opponentPlayerName;
        else              game.whitePlayer.name = opponentPlayerName;
    }

    private void updateClockLabels() {
        whiteClockLabel.setText(fmtClock(whiteTimeMs));
        blackClockLabel.setText(fmtClock(blackTimeMs));
    }

    private static String fmtClock(long ms) {
        long total = Math.max(0, ms) / 1000;
        long m = total / 60;
        long s = total % 60;
        return String.format("%d:%02d", m, s);
    }

    private void highlightActivePlayer() {
        if (game == null || whitePlayerCard == null) return;
        JPanel active   = game.whiteTurn ? whitePlayerCard : blackPlayerCard;
        JPanel inactive = game.whiteTurn ? blackPlayerCard : whitePlayerCard;
        active.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 87, 34), 2),
            BorderFactory.createEmptyBorder(7, 11, 7, 11)));
        inactive.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(74, 85, 104), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    }

    /**
     * Checks for game-over after every move and routes to the correct end-of-game dialog.
     * Combines Game.isInCheck() and Game.hasLegalMove() to distinguish checkmate (no
     * moves AND in check) from stalemate (no moves AND not in check).
     */
    private void announceEndIfOver() {
        boolean sideToMove = game.whiteTurn;
        boolean inCheck = game.isInCheck(sideToMove);
        boolean hasMove = game.hasLegalMove(sideToMove);
        
        if (!hasMove && inCheck) {
            String winner = sideToMove ? "Black" : "White";
            showGameOverDialog("Checkmate! " + winner + " wins.");
            return;
        } else if (!hasMove) {
            showGameOverDialog("Stalemate. Draw.");
            return;
        }

        String drawReason = game.getDrawReason();
        if (drawReason != null) {
            if (drawReason.equals("FiftyMoves")) {
                showGameOverDialog("Draw by 50-move rule.");
            } else if (drawReason.equals("Repetition")) {
                showGameOverDialog("Draw by threefold repetition.");
            } else if (drawReason.equals("InsufficientMaterial")) {
                showGameOverDialog("Draw by insufficient material.");
            }
        }
    }

    /**
     * Bootstraps a networked two-player session and runs the long-lived receive loop on
     * a background worker thread. Responsibilities:
     *  - establish the TCP connection (open a ServerSocket and accept() if hosting,
     *    or open a Socket to the supplied IP/port if joining);
     *  - assign colours — the server flips a coin, broadcasts COLOR:, and exchanges
     *    NAME: messages so both sides know who they're playing;
     *  - read newline-delimited text packets from BufferedReader and route each one by.
     */
    private void startNetwork(boolean isServer, String ip, int port) {
        game = new Game();
        gameOver = false;
        logArea.setText("");

        localPlayerName = isServer ? game.whitePlayer.name : game.blackPlayer.name;
        opponentPlayerName = null;

        ((CardLayout)cards.getLayout()).show(cards, "WORK");
        
        network = new ChessNetwork(new ChessNetworkListener() {
            @Override
            public void onConnected() {
                logArea.append("System: Connected!\n");
                if (isServer) {
                    playingWhite = Math.random() < 0.5;
                    network.sendMessage("COLOR:" + (playingWhite ? "black" : "white"));
                    SwingUtilities.invokeLater(() -> {
                        applyMyName();
                        refreshPlayerNames();
                    });
                    network.sendMessage("NAME:" + localPlayerName);
                }
                SwingUtilities.invokeLater(() -> {
                    activeBoard.repaint();
                    whiteCapturedPanel.repaint();
                    blackCapturedPanel.repaint();
                    startGameClock();
                });
            }

            @Override
            public void onColorAssigned(boolean clientIsWhite) {
                SwingUtilities.invokeLater(() -> {
                    playingWhite = clientIsWhite;
                    applyMyName();
                    refreshPlayerNames();
                    activeBoard.repaint();
                    whiteCapturedPanel.repaint();
                    blackCapturedPanel.repaint();
                });
                network.sendMessage("NAME:" + localPlayerName);
            }

            @Override
            public void onNameReceived(String name) {
                SwingUtilities.invokeLater(() -> {
                    opponentPlayerName = name;
                    applyOpponentName();
                    refreshPlayerNames();
                });
            }

            @Override
            public void onChatReceived(String msg) {
                SwingUtilities.invokeLater(() -> {
                    if (msg.startsWith("System:")) {
                        logArea.append(msg + "\n");
                    } else {
                        logArea.append("Opponent: " + msg + "\n");
                    }
                });
            }

            @Override
            public void onMoveReceived(int fromRow, int fromCol, int toRow, int toCol, boolean isWhite, String pawnPromotion, boolean isCheck, int peerSig) {
                SwingUtilities.invokeLater(() -> {
                    if (!game.canMove(fromRow, fromCol, toRow, toCol, isWhite)) {
                        logArea.append("Invalid move received from peer\n");
                        return;
                    }
                    String san = game.toAlgebraic(fromRow, fromCol, toRow, toCol, isWhite, pawnPromotion);
                    String formattedMove = game.formatMove(san);
                    game.sanMoves.add(san);

                    game.Move(fromRow, fromCol, toRow, toCol, isWhite);
                    if (!pawnPromotion.equals("None")) {
                        game.promotion(toRow, toCol, isWhite, pawnPromotion);
                    }
                
                    logArea.append("Opponent: " + formattedMove + "\n");
                    if (isCheck) {
                        logArea.append("In Check!\n", true);
                    }
                    game.whiteTurn = !game.whiteTurn;
                    game.recordState();
                    activeBoard.repaint();
                    whiteCapturedPanel.repaint();
                    blackCapturedPanel.repaint();

                    if (peerSig != 0) {
                        int localSig = game.stateSignature().hashCode();
                        if (localSig != peerSig) {
                            logArea.append("⚠ DESYNC: boards diverged (local=" + localSig + " peer=" + peerSig + "). Move not trusted.\n");
                            gameOver = true;
                            stopGameClock();
                        }
                    }
                    announceEndIfOver();
                });
            }

            @Override
            public void onResigned() {
                SwingUtilities.invokeLater(() -> showGameOverDialog("Opponent resigned. You win!"));
            }

            @Override
            public void onDrawOffer() {
                SwingUtilities.invokeLater(() -> {
                    int response = JOptionPane.showConfirmDialog(GUI.this,
                        "Opponent offered a draw. Accept?",
                        "Draw Offer",
                        JOptionPane.YES_NO_OPTION);
                    if (response == JOptionPane.YES_OPTION) {
                        network.sendMessage("GAME:DRAW_ACCEPT");
                        showGameOverDialog("Draw agreed.");
                    } else {
                        network.sendMessage("GAME:DRAW_DECLINE");
                    }
                });
            }

            @Override
            public void onDrawAccept() {
                SwingUtilities.invokeLater(() -> showGameOverDialog("Opponent accepted the draw. Game Over."));
            }

            @Override
            public void onDrawDecline() {
                SwingUtilities.invokeLater(() -> logArea.append("System: Opponent declined the draw.\n"));
            }

            @Override
            public void onRematchRequest() {
                SwingUtilities.invokeLater(() -> {
                    int response = JOptionPane.showConfirmDialog(GUI.this,
                        "Opponent requested a rematch. Accept?",
                        "Rematch Request",
                        JOptionPane.YES_NO_OPTION);
                    if (response == JOptionPane.YES_OPTION) {
                        network.sendMessage("REMATCH:ACCEPT");
                        game = new Game();
                        gameOver = false;
                        applyMyName();
                        applyOpponentName();
                        logArea.append("System: Starting a new game...\n");
                        activeBoard.repaint();
                        whiteCapturedPanel.repaint();
                        blackCapturedPanel.repaint();
                        startGameClock();
                    } else {
                        network.sendMessage("REMATCH:DECLINE");
                        returnToMenu();
                    }
                });
            }

            @Override
            public void onRematchAccept() {
                SwingUtilities.invokeLater(() -> {
                    game = new Game();
                    gameOver = false;
                    applyMyName();
                    applyOpponentName();
                    logArea.append("System: Opponent accepted rematch. Starting a new game...\n");
                    activeBoard.repaint();
                    whiteCapturedPanel.repaint();
                    blackCapturedPanel.repaint();
                    startGameClock();
                });
            }

            @Override
            public void onRematchDecline() {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(GUI.this, "Opponent declined the rematch.");
                    returnToMenu();
                });
            }

            @Override
            public void onDisconnected(String reason) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append("System: Connection lost (" + reason + ").\n");
                    JOptionPane.showMessageDialog(GUI.this, "Connection lost: " + reason);
                    returnToMenu();
                });
            }
        });

        if (isServer) {
            network.startServer(port);
        } else {
            network.startClient(ip, port);
        }
    }

    /**
     * Eagerly loads all twelve piece sprites (six piece types x two colours) from disk
     * into a HashMap keyed by "<Color><PieceType>" so paintComponent() can look them up
     * in O(1). Doing this once at construction keeps the render loop disk-free.
     */
    private void loadImages() {
        String[] pieces = {"Pawn", "Rook", "Knight", "Bishop", "Queen", "King"};
        String[] colors = {"White", "Black"};
        try {
            for (String pieceColor : colors) {
                for (String pieceType : pieces) {
                    String fileName = "ChessPieces/" + pieceColor + "Pieces/" + pieceColor + pieceType + ".png";
                    Image imagePiece = ImageIO.read(new File(fileName));
                    pieceImages.put(pieceColor + pieceType, imagePiece);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading images: " + e.getMessage());
        }
    }

    /**
     * Inner JPanel subclass that renders the 8x8 board and processes the player's mouse
     * input. Two-click selection model: the first click selects a friendly piece, the
     * second click either moves it (if legal), deselects it (if the same square is
     * clicked again), or transfers selection to another friendly piece. The board is
     * flipped for the Black player so the player's own pieces are always at the bottom.
     */
    private class ActiveBoardPanel extends JPanel {
        private static final Color light = new Color(208, 212, 219);
        private static final Color dark  = new Color(74, 85, 104);
        private int selectedRow = -1;
        private int selectedCol = -1;

        // True when the local player is Black — the board is drawn rotated 180 degrees
        // so the player's own pieces sit at the bottom regardless of colour.
        private boolean flipped(){
            return !playingWhite;
        }

        public ActiveBoardPanel() {
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    if (game == null || (!vsBot && network == null) || gameOver){
                        return;
                    } 
                    // Convert pixel coordinates back to board indices. The display
                    // coordinates may be flipped (Black's perspective), so we un-flip
                    // before reading/writing the actual game.board 2D array.
                    int w = getWidth(), h = getHeight();
                    int sq = Math.min(w, h) / 8;
                    int displayCol = e.getX() / sq;
                    int displayRow = e.getY() / sq;
                    if (displayCol >= 8 || displayRow >= 8){
                        return;
                    }
                    int col = flipped() ? 7 - displayCol : displayCol;
                    int row = flipped() ? 7 - displayRow : displayRow;
                    if (selectedRow == -1) {
                        // No piece selected yet: first click must land on a friendly piece whose colour matches the turn.
                        Piece p = game.board[row][col];
                        if (p != null && p.isWhite == game.whiteTurn && p.isWhite == playingWhite) {
                            selectedRow = row;
                            selectedCol = col;
                            repaint();
                        }
                    } else {
                        if (selectedRow == row && selectedCol == col) {
                            // Same-square click toggles the selection off — gives the player an escape hatch.
                            selectedRow = -1;
                            selectedCol = -1;
                            repaint();
                        } else {
                            Piece targetP = game.board[row][col];
                            // Transfer selection when clicking another friendly piece — feels natural and skips an extra deselect click.
                            if (targetP != null && targetP.isWhite == game.whiteTurn && targetP.isWhite == playingWhite) {
                                selectedRow = row;
                                selectedCol = col;
                                repaint();
                            } else {
                                // Otherwise treat the second click as the move destination.
                                attemptMove(selectedRow, selectedCol, row, col);
                                selectedRow = -1; // Clear selection so the next move starts fresh.
                                selectedCol = -1;
                                repaint();
                            }
                        }
                    }
                }
            });
        }

        /**
         * Renders the board in four ordered layers (drawn back-to-front so each layer
         * can paint over the previous one): (1) the alternating light/dark squares,
         * (2) the orange highlight on the currently selected square, (3) move-hint
         * markers — solid dots on empty legal targets and hollow rings on capture
         * targets, computed by polling Game.canMove() for every destination square —
         * and (4) the piece sprites looked up from the pre-loaded image cache.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int sq = Math.min(w, h) / 8;

            // Draw board squares — alternating light/dark colour pattern over the 8x8 grid.
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    int dr = flipped() ? 7 - row : row; // Flip the display row when the local player is Black.
                    int dc = flipped() ? 7 - col : col; // Same flip applied to columns so the whole board rotates 180.
                    g2.setColor((row + col) % 2 == 0 ? light : dark); // Classic chequerboard: even-parity squares are light.
                    g2.fillRect(dc * sq, dr * sq, sq, sq);
                }
            }

            // Draw last move highlight
            if (game != null && game.lastMoveFromRow != -1) {
                g2.setColor(new Color(255, 235, 59, 100)); // Soft semi-transparent yellow
                int drFrom = flipped() ? 7 - game.lastMoveFromRow : game.lastMoveFromRow;
                int dcFrom = flipped() ? 7 - game.lastMoveFromCol : game.lastMoveFromCol;
                g2.fillRect(dcFrom * sq, drFrom * sq, sq, sq);
                
                int drTo = flipped() ? 7 - game.lastMoveToRow : game.lastMoveToRow;
                int dcTo = flipped() ? 7 - game.lastMoveToCol : game.lastMoveToCol;
                g2.fillRect(dcTo * sq, drTo * sq, sq, sq);
            }

            // Draw king in check highlight
            if (game != null) {
                if (game.isInCheck(true)) {
                    Piece whiteKing = game.getKing(true);
                    if (whiteKing != null) {
                        int dr = flipped() ? 7 - whiteKing.row : whiteKing.row;
                        int dc = flipped() ? 7 - whiteKing.col : whiteKing.col;
                        g2.setColor(new Color(244, 67, 54, 120)); // Soft semi-transparent red
                        g2.fillRect(dc * sq, dr * sq, sq, sq);
                    }
                }
                if (game.isInCheck(false)) {
                    Piece blackKing = game.getKing(false);
                    if (blackKing != null) {
                        int dr = flipped() ? 7 - blackKing.row : blackKing.row;
                        int dc = flipped() ? 7 - blackKing.col : blackKing.col;
                        g2.setColor(new Color(244, 67, 54, 120)); // Soft semi-transparent red
                        g2.fillRect(dc * sq, dr * sq, sq, sq);
                    }
                }
            }

            // Draw selection highlight — orange tint on the square the player has clicked.
            if (selectedRow != -1 && selectedCol != -1) {
                int dr = flipped() ? 7 - selectedRow : selectedRow;
                int dc = flipped() ? 7 - selectedCol : selectedCol;
                g2.setColor(new Color(255, 87, 34, 180)); // Semi-transparent orange so the piece below remains visible.
                g2.fillRect(dc * sq, dr * sq, sq, sq);
            }

            // Draw valid move highlights — dots/rings that show every legal destination for the selected piece.
            if (selectedRow != -1 && selectedCol != -1 && game != null) {
                Piece selectedPiece = game.board[selectedRow][selectedCol];
                if (selectedPiece != null) {
                    g2.setColor(new Color(0, 0, 0, 60)); // Faint black so the markers do not dominate the board art.

                    for (int r = 0; r < 8; r++) {
                        for (int c = 0; c < 8; c++) {
                            // Poll the rule engine for every square; cheap because canMove() short-circuits quickly on invalid targets.
                            if (game.canMove(selectedRow, selectedCol, r, c, selectedPiece.isWhite)) {
                                int dr = flipped() ? 7 - r : r;
                                int dc = flipped() ? 7 - c : c;

                                int radius = sq / 6; // Marker size scales with the board so it looks right at any window size.
                                int centerX = dc * sq + sq / 2;
                                int centerY = dr * sq + sq / 2;

                                // Hollow ring on capture targets, solid dot on empty squares — standard chess-app convention.
                                if (game.board[r][c] != null) {
                                    g2.setStroke(new BasicStroke(4f));
                                    g2.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                                } else {
                                    g2.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                                }
                            }
                        }
                    }
                }
            }

            // Draw pieces — top layer so sprites cover the squares and highlights below.
            if (game != null && game.board != null) {
                for (int row = 0; row < 8; row++) {
                    for (int col = 0; col < 8; col++) {
                        Piece p = game.board[row][col];
                        if (p != null) {
                            String colorStr = p.isWhite ? "White" : "Black";
                            String key = colorStr + p.getType(); // HashMap key matches loadImages()'s naming scheme.
                            Image img = pieceImages.get(key);
                            if (img != null) {
                                int dr = flipped() ? 7 - row : row;
                                int dc = flipped() ? 7 - col : col;
                                g2.drawImage(img, dc * sq, dr * sq, sq, sq, this); // Scale the sprite to fit one board square exactly.
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Tears down the active game/connection and switches the CardLayout back to the
     * main menu. Closes the socket and PrintWriter so the receive thread's readLine()
     * returns null and the worker exits cleanly, then clears player-name fields so the
     * next session starts from a known state.
     */
    private void returnToMenu() {
        stopGameClock();
        if (network != null) {
            network.close();
        }
        network = null;
        localPlayerName = null;
        opponentPlayerName = null;
        ((CardLayout)cards.getLayout()).show(cards, "MENU");
    }

    /**
     * Inner JPanel subclass that renders one side's captured pieces as a small horizontal
     * row of sprite icons. Constructed once per colour and added to the dashboard; calling
     * repaint() is enough to refresh after every capture because paintComponent() always
     * reads the latest ArrayList from the Game.
     */
    private class CapturedPanel extends JPanel {
        private boolean isWhite;

        public CapturedPanel(boolean isWhite) {
            this.isWhite = isWhite;
            setPreferredSize(new Dimension(200, 35));
            setOpaque(false);
        }

        /**
         * Iterates the appropriate captured-piece ArrayList and draws each piece's sprite
         * at a fixed 30x30 size, shifting x by 15 px per piece so the icons overlap
         * slightly — a compact display that scales as more pieces are captured.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (game == null) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            //Get the list of captured pieces from the Game model
            ArrayList<Piece> captured = isWhite ? game.capturedWhite : game.capturedBlack;

            int x = 0;
            for (Piece p : captured) {
                String key = (isWhite ? "White" : "Black") + p.getType();
                Image img = pieceImages.get(key);
                if (img != null) {
                    g2.drawImage(img, x, 2, 30, 30, this);
                    x += 15;
                }
            }
        }
    }

    private class LogPane extends JTextPane {
        public void append(String s) {
            append(s, false);
        }
        public void append(String s, boolean bold) {
            try {
                javax.swing.text.StyledDocument doc = getStyledDocument();
                javax.swing.text.SimpleAttributeSet style = new javax.swing.text.SimpleAttributeSet();
                javax.swing.text.StyleConstants.setForeground(style, getForeground());
                javax.swing.text.StyleConstants.setFontFamily(style, getFont().getFamily());
                javax.swing.text.StyleConstants.setFontSize(style, getFont().getSize());
                if (bold) {
                    javax.swing.text.StyleConstants.setBold(style, true);
                    javax.swing.text.StyleConstants.setForeground(style, new Color(255, 87, 34)); // Make it orange too!
                }
                doc.insertString(doc.getLength(), s, style);
            } catch (Exception e) {}
        }
    }
}
