package compucrash;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

public class CPanel extends JPanel {
	
	public static final int LEFT = 0;
	public static final int CENTER = 1;
	public static final int RIGHT = 2;
	
	private JPanel left = new JPanel();
	private JPanel center = new JPanel();
	private JPanel right = new JPanel();
	private JPanel p1 = new JPanel();
	private JPanel p2 = new JPanel();
	private JPanel p3 = new JPanel();
	
	public CPanel() {
		setLayout(new BorderLayout());
		add(p1,BorderLayout.WEST);
		add(p2,BorderLayout.CENTER);
		add(p3,BorderLayout.NORTH);
		p1.setLayout(new BorderLayout());
		p2.setLayout(new BorderLayout());
		p3.setLayout(new BorderLayout());
		p1.add(left,BorderLayout.NORTH);
		p2.add(center,BorderLayout.NORTH);
		p3.add(right,BorderLayout.NORTH);
		left.setLayout(new GridBagLayout());
		center.setLayout(new GridBagLayout());
		right.setLayout(new GridBagLayout());
	}
	
	public void add(Component comp, int pos, GridBagConstraints c) {
		switch (pos) {
			case CPanel.LEFT:
			left.add(comp,c);
			return;
			case CPanel.CENTER:
			center.add(comp,c);
			return;
			case CPanel.RIGHT:
			right.add(comp,c);
			return;
			default:
			left.add(comp,c);
			return;
		}
	}
	
	public JPanel getPanel(int pos) {
		switch (pos) {
		case CPanel.LEFT:
		return left;
		case CPanel.CENTER:
		return center;
		case CPanel.RIGHT:
		return right;
		default:
		return left;
	}
	}

    public void setColor(Color c) {
        setBackground(c);
        left.setBackground(c);
        center.setBackground(c);
        right.setBackground(c);
        p1.setBackground(c);
        p2.setBackground(c);
        p3.setBackground(c);
 /*       for (int i = 0; i < left.getComponentCount();i++) {
            left.getComponent(i).setBackground(c);
        }
        for (int i = 0; i < center.getComponentCount();i++) {
            center.getComponent(i).setBackground(c);
        }
        for (int i = 0; i < right.getComponentCount();i++) {
            right.getComponent(i).setBackground(c);
        }*/
    }

}
