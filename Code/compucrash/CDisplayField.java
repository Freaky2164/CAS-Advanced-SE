package compucrash;

import javax.swing.*;
import java.awt.*;

public class CDisplayField extends JPanel implements CInfoParent {

    private static final String DATA_TYPE_KEY = "data_type";
    private final CProperties p;
    private final CDisplayFieldBean display;
    private final CInfoFrame frame;
    protected String name = null;


    public CDisplayField(CProperties p, CInfoFrame frame) {
        super();
        this.p = p;
        this.frame = frame;
        this.name = p.get("owner") + "." +
                p.get("table_name") + "." +
                p.get("column_name");
        if (p.get(DATA_TYPE_KEY).toString().equals("TEXT")) {
            display = new CDisplayFieldTextBean(p, frame);
        } else if (p.get(DATA_TYPE_KEY).toString().equals("TABLE")) {
            display = new CDisplayFieldTableBean(p, frame);
        } else if (p.get(DATA_TYPE_KEY).toString().equals("DATE")) {
            display = new CDisplayFieldDateBean(p, frame);
        } else if (p.get(DATA_TYPE_KEY).toString().equals("FLOAT")) {
            display = new CDisplayFieldNumberBean(p, frame);
        } else if (p.get(DATA_TYPE_KEY).toString().equals("CHECK")) {
            display = new CDisplayFieldCheckBean(p, frame);
        } else if (p.get(DATA_TYPE_KEY).toString().equals("DOCUMENT")) {
            display = new CDisplayFieldDocumentBean(p, frame);
        } else if (p.get(DATA_TYPE_KEY).toString().equals("HISTORY")) {
            display = new CDisplayFieldHistoryBean(p, frame);
        } else if (p.get(DATA_TYPE_KEY).toString().equals("LIST")) {
            display = new CDisplayFieldListBean(p, frame);
        } else if (p.get(DATA_TYPE_KEY).toString().equals("LINK")) {
            display = new CDisplayFieldLinkBean(p, frame);
        } else if (p.get(DATA_TYPE_KEY).toString().equals("CHARFRAME")) {
            display = new CDisplayFieldCharFrameBean(p, frame);
        } else {
            display = new CDisplayFieldCharBean(p, frame);
        }
        setLayout(new BorderLayout());
        add(display, BorderLayout.CENTER);
    }

    public Object getValue() {
        return display.getValue();
    }

    public void setValue(Object o) {
        display.setValue(o);
        if (o == null) {
            frame.attributeValues.remove(name);
        } else {
            frame.attributeValues.put(name, o);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public void resetEditedColor() {
        CMessage.print(name);
        display.resetEditedColor();
    }

    public void setEditable(boolean bool) {
        if (p.get("editable") == null) {
            if (bool) {
                display.setEditable(1);
            } else {
                display.setEditable(0);
            }
        } else {
            if (bool) {
                display.setEditable(Integer.parseInt(p.get("editable").toString()));
            } else {
                display.setEditable(0);
            }
        }
    }

    public String getLabel() {
        return (String) p.get("label");
    }

    public String getViewPanel() {
        return (String) p.get("view_panel");
    }

    public String getPanel() {
        return (String) p.get("panel");
    }

    public CProperties getProperties() {
        return p;
    }

    public CInfoFrame getCInfoFrame() {
        return frame;
    }

    public void setColor(Color c) {
        setBackground(c);
        display.setColor(c);
    }

    public void refresh() {
        display.refresh();
    }
}
