package compucrash;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.util.Hashtable;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CFrame extends JFrame {

	private JPanel p1 = new JPanel();
	private JPanel p2 = new JPanel();
	private JPanel p3 = new JPanel();
	private JPanel p4 = new JPanel();
	private JPanel p5 = new JPanel();
	private JPanel p6 = new JPanel();
	private JPanel p7 = new JPanel();
	private JPanel p8 = new JPanel();
	private JPanel buttonPaneLeft = new JPanel();
	private JPanel buttonPaneRight = new JPanel();
	private JPanel mainPane = new JPanel();
	private JPanel mainPaneTopLeft = new JPanel();
	private JPanel mainPaneTop = new JPanel();
	private JPanel mainPaneTopRight = new JPanel();
	private JTextField statusLine = new JTextField();
	private Hashtable buttonsLeft = new Hashtable();
	private Hashtable buttonsRight = new Hashtable();
	protected CFrame parent;
	private CMediator mediator;
	private JPanel customButtonPane = new JPanel();
	public String name;
	public JDesktopPane cp = new JDesktopPane();
	
	public CFrame(CFrame parent) throws HeadlessException {
		super();
		this.parent = parent;
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().createImage("../images/logo.gif"));
		setContentPane(cp);
		cp.setLayout(new BorderLayout());
		cp.add(p1, BorderLayout.SOUTH);
		p1.setLayout(new BorderLayout());
		p1.add(statusLine, BorderLayout.SOUTH);
		statusLine.setEditable(false);
		cp.add(p3, BorderLayout.CENTER);
		p3.setLayout(new BorderLayout());
		p3.add(p4, BorderLayout.NORTH);
		p4.setLayout(new BorderLayout());
		p5.setLayout(new BorderLayout());
		p6.setLayout(new BorderLayout());
		p7.setLayout(new BorderLayout());
		mainPaneTopLeft.setLayout(new GridBagLayout());
		mainPaneTop.setLayout(new GridBagLayout());
		mainPaneTopRight.setLayout(new GridBagLayout());
		p4.add(p5, BorderLayout.WEST);
		p4.add(p6, BorderLayout.CENTER);
		p4.add(p7, BorderLayout.EAST);
		p5.add(mainPaneTopLeft,BorderLayout.NORTH);
		p6.add(mainPaneTop,BorderLayout.NORTH);
		p7.add(mainPaneTopRight,BorderLayout.NORTH);
		p3.add(mainPane, BorderLayout.CENTER);
		mainPane.setLayout(new BorderLayout());	
		p8.setLayout(new BorderLayout());
		p1.add(p8, BorderLayout.NORTH);
		p8.add(p2, BorderLayout.SOUTH);
		customButtonPane.setLayout(new FlowLayout(FlowLayout.LEFT));
		p8.add(customButtonPane, BorderLayout.NORTH);	
		p2.setLayout(new BorderLayout());
		p2.add(buttonPaneLeft, BorderLayout.WEST);
		p2.add(buttonPaneRight, BorderLayout.EAST);
		buttonPaneLeft.setLayout(new FlowLayout(FlowLayout.LEFT));
		buttonPaneRight.setLayout(new FlowLayout(FlowLayout.RIGHT));
	}		
	
	public JDesktopPane getDesktopPane() {
	    return cp;
	}
	
	public void setFrameSize() {
	    int x, y, width, height;
	    try {
		    x = Integer.parseInt(CPropertyManager.getInstance().getProperty(name + ".x"));	        
		    y = Integer.parseInt(CPropertyManager.getInstance().getProperty(name + ".y"));	        
		    width = Integer.parseInt(CPropertyManager.getInstance().getProperty(name + ".width"));	        
		    height = Integer.parseInt(CPropertyManager.getInstance().getProperty(name + ".height"));	        
		    setBounds(x, y, width, height);	    
	    } catch (NumberFormatException e) {
	        pack();
	    }
	}
	
	public void setStatusLine(String text) {
		statusLine.setText(text);
		statusLine.setToolTipText(text);
	}

	public void clearStatusLine() {
		statusLine.setText("");
		statusLine.setToolTipText("Keine Warnungen");		
	}

	public JPanel getButtonPaneLeft() {
		return buttonPaneLeft;
	}

	public JPanel getButtonPaneRight() {
		return buttonPaneRight;
	}
	
	public JPanel getCustomButtonPane() {
		return customButtonPane;
	}

	public JPanel getMainPane() {
		return mainPane;
	}

	public JPanel getMainPaneTop() {
		return mainPaneTop;
	}

	public Hashtable getButtonsLeft() {
		return buttonsLeft;
	}

	public Hashtable getButtonsRight() {
		return buttonsRight;
	}

	public void addButtonLeft(CButton b) {
		buttonsLeft.put(b.toString(), b);
	}

	public JPanel getMainPaneTopLeft() {
		return mainPaneTopLeft;
	}

	public JPanel getMainPaneTopRight() {
		return mainPaneTopRight;
	}

    public void setColor(Color c) {
        mainPane.setBackground(c);
        mainPaneTop.setBackground(c);
        mainPaneTopLeft.setBackground(c);
        mainPaneTopRight.setBackground(c);
        buttonPaneLeft.setBackground(c);
        buttonPaneRight.setBackground(c);
        customButtonPane.setBackground(c);
        p1.setBackground(c);
        p2.setBackground(c);
        p3.setBackground(c);
        p4.setBackground(c);
        p5.setBackground(c);
        p6.setBackground(c);
        p7.setBackground(c);
        p8.setBackground(c);
        getContentPane().setBackground(c);
        statusLine.setBackground(c);
    }
    
    public void dispose() {
        String value = Integer.toString(getBounds().x);
        CPropertyManager.getInstance().setProperty(name + ".x",value);
        value = Integer.toString(getBounds().y);
        CPropertyManager.getInstance().setProperty(name + ".y",value);
        value = Integer.toString(getBounds().width);
        CPropertyManager.getInstance().setProperty(name + ".width",value);
        value = Integer.toString(getBounds().height);
        CPropertyManager.getInstance().setProperty(name + ".height",value);
        super.dispose();
    }

}
