package compucrash;

import javax.swing.*;
import java.awt.*;

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
                return (CPanel) getComponentAt(i);
            }
        }
        return null;
    }

    public void setColor(Color c) {
        setBackground(c);
        for (int i = 0; i < getComponentCount(); i++) {
            ((CPanel) (getComponentAt(i))).setColor(c);
        }
    }

}
