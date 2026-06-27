package compucrash;

import javax.swing.*;
import java.awt.*;

public abstract class CDisplayFieldBean extends JPanel implements CInfoParent {

    protected CProperties p;
    protected CInfoFrame frame;
    protected String name;

    protected CDisplayFieldBean() {
    }

    protected CDisplayFieldBean(CProperties p, CInfoFrame frame) {
        this();
        this.p = p;
        this.frame = frame;

        this.name = p.get("owner").toString() + "." +
                p.get("table_name").toString() + "." +
                p.get("column_name").toString();
    }

    public abstract void setEditable(int i);

    public abstract Object getValue();

    public abstract void setValue(Object o);

    public void lostFocus() {
        if (frame.setAttributeValue(name, getValue())) setEditedColor();
    }

    // getTextField wird das noch gebraucht?
    public abstract Component getTextField();

    public void setColor(Color c) {
        this.setBackground(c);
    }

    public abstract void setEditedColor();

    public abstract void resetEditedColor();

    public void refresh() {
    }
}
