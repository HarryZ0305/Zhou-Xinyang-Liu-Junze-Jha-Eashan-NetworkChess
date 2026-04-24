import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;


public class MoveLog extends JPanel {

    
    private static final Color BG         = new Color(20, 20, 20);
    private static final Color HEADER_BG  = new Color(28, 28, 28);
    private static final Color ROW_EVEN   = new Color(26, 26, 26);
    private static final Color ROW_ODD    = new Color(33, 33, 33);
    private static final Color ROW_HOVER  = new Color(46, 44, 38);
    private static final Color ACCENT     = new Color(200, 170, 90);
    private static final Color LATEST_BG  = new Color(55, 50, 28);
    private static final Color WHITE_FG   = new Color(230, 225, 210);
    private static final Color BLACK_FG   = new Color(155, 150, 135);
    private static final Color NUM_FG     = new Color(90,  88,  78);
    private static final Color DIVIDER    = new Color(44,  44,  44);
    private static final Color CASTLE_FG  = new Color(120, 165, 220);
    private static final Color CAPTURE_FG = new Color(205, 95,  85);
    private static final Color CHECK_FG   = new Color(225, 185, 55);
    private static final Color PROMO_FG   = new Color(120, 200, 130);

    private static final char[] FILES = {'a','b','c','d','e','f','g','h'};

  
    private final ArrayList<String[]> movePairs = new ArrayList<>();
    private int     lastPair     = -1;
    private boolean lastWasWhite = false;


    private JPanel      moveContainer;
    private JScrollPane scrollPane;
    private JLabel      statusLabel;
    private JLabel      openingLabel;
    private JLabel      moveCountLabel;

    public MoveLog() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setPreferredSize(new Dimension(230, 0));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, DIVIDER));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildScroll(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }
