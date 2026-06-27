package compucrash;

import javax.swing.*;
import java.awt.*;

public abstract class CAddonTableBean extends JPanel {

    private final CTable tab;
    private final transient CListParent addParent;

    protected CAddonTableBean(CTable tab, CListParent parent) {
        this.tab = tab;
        this.addParent = parent;
    }

    public CTable getTab() {
        return this.tab;
    }

    public CListParent getAddParent() {
        return this.addParent;
    }

    public abstract void setColor(Color c);

    public abstract JTextField getTextField();
}
