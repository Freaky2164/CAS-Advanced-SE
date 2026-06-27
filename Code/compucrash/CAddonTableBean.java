package compucrash;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JTextField;

public abstract class CAddonTableBean extends JPanel {

    public CTable tab;
    public CListParent parent;
    
    public CAddonTableBean(CTable tab, CListParent parent) {
        this.tab = tab;
        this.parent = parent;
    }

    public abstract void setColor(Color c);
    public abstract JTextField getTextField();
}
