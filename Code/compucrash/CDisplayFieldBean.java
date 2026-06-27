package compucrash;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JPanel;

public abstract class CDisplayFieldBean extends JPanel implements CInfoParent {
    
    public CProperties p;
    public CInfoFrame frame;
    public String name;

	public abstract void setValue(Object o);
	public abstract void setEditable(int i);
	public abstract Object getValue();
	public void lostFocus() {
	    if (frame.setAttributeValue(name, getValue())) setEditedColor();
	}
	// getTextField wird das noch gebraucht?
    public abstract Component getTextField();
    public void setColor(Color c) {
        this.setBackground(c);
    }
    
    public CDisplayFieldBean() {}
    public CDisplayFieldBean(CProperties p, CInfoFrame frame) {
        this();
        this.p = p;
        this.frame = frame;
        
        this.name = p.get("owner").toString() + "." +
        		p.get("table_name").toString() + "." +
        		p.get("column_name").toString();
    }
    public abstract void setEditedColor();
    public abstract void resetEditedColor();
    public void refresh() {
    }
}