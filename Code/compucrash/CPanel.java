package compucrash;

import javax.swing.*;
import java.awt.*;

public class CPanel extends JPanel {

    public static final int LEFT = 0;
    public static final int CENTER = 1;
    public static final int RIGHT = 2;

    private final JPanel leftPanel = new JPanel();
    private final JPanel centerPanel = new JPanel();
    private final JPanel rightPanel = new JPanel();
    private final JPanel p1 = new JPanel();
    private final JPanel p2 = new JPanel();
    private final JPanel p3 = new JPanel();

    public CPanel() {
        setLayout(new BorderLayout());
        add(p1, BorderLayout.WEST);
        add(p2, BorderLayout.CENTER);
        add(p3, BorderLayout.NORTH);
        p1.setLayout(new BorderLayout());
        p2.setLayout(new BorderLayout());
        p3.setLayout(new BorderLayout());
        p1.add(leftPanel, BorderLayout.NORTH);
        p2.add(centerPanel, BorderLayout.NORTH);
        p3.add(rightPanel, BorderLayout.NORTH);
        leftPanel.setLayout(new GridBagLayout());
        centerPanel.setLayout(new GridBagLayout());
        rightPanel.setLayout(new GridBagLayout());
    }

    public void add(Component comp, int pos, GridBagConstraints c) {
        switch (pos) {
            case CPanel.LEFT:
                leftPanel.add(comp, c);
                return;
            case CPanel.CENTER:
                centerPanel.add(comp, c);
                return;
            case CPanel.RIGHT:
                rightPanel.add(comp, c);
                return;
            default:
                leftPanel.add(comp, c);
        }
    }

    public JPanel getPanel(int pos) {
        switch (pos) {
            case CPanel.LEFT:
                return leftPanel;
            case CPanel.CENTER:
                return centerPanel;
            case CPanel.RIGHT:
                return rightPanel;
            default:
                return leftPanel;
        }
    }

    public void setColor(Color c) {
        setBackground(c);
        leftPanel.setBackground(c);
        centerPanel.setBackground(c);
        rightPanel.setBackground(c);
        p1.setBackground(c);
        p2.setBackground(c);
        p3.setBackground(c);
    }

}
