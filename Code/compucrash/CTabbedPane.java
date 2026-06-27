package compucrash;
import java.awt.Color;

import javax.swing.ImageIcon;
import javax.swing.JTabbedPane;

public class CTabbedPane extends JTabbedPane {
	
	private CPanel p;

    public CTabbedPane() {
		super();
	}
	
	public void addTab(String name) {
		for (int i = 0; i < getComponentCount(); i++) {
			if (getComponentAt(i).getName().equals(name)) {
				return;
			}
		}
		p = new CPanel();
		p.setName(name);
		super.addTab(name, new ImageIcon("../hauswert/images/document.gif"), p, "Tooltip Text");
	}
	
	public CPanel getTab(String name) {
		for (int i = 0; i < getComponentCount(); i++) {
			if (getComponentAt(i).getName().equals(name)) {
				return (CPanel)getComponentAt(i);
			}
		}
		return null;
	}
	
	public void setColor(Color c) {
	    setBackground(c);
//	    if (p != null) p.setColor(c);
	    System.out.println(getComponentCount());
		for (int i = 0; i < getComponentCount(); i++) {
		    System.out.println(getComponentAt(i));
		    ((CPanel)(getComponentAt(i))).setColor(c);
		}

	}

}
