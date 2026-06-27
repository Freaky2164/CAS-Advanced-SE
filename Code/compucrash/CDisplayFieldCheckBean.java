package compucrash;

import javax.swing.*;
import java.awt.*;

public class CDisplayFieldCheckBean extends CDisplayFieldBean /*implements CSelectParent*/ {

    private final JLabel label = new JLabel();
    private final JCheckBox check = new JCheckBox();

    public CDisplayFieldCheckBean(CProperties p, CInfoFrame frame) {
        super(p, frame);
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(label);
        add(check);
        String labelString = (String) p.get("label");
        label.setText(labelString);
        label.setPreferredSize(
                new Dimension(
                        Integer.parseInt((String) p.get("label_length")) * 7,
                        label.getPreferredSize().height));
        if (p.get("tooltip") != null) {
            setToolTipText((String) p.get("tooltip"));
        }
        check.addActionListener(e -> lostFocus());
    }

    public Component getTextField() {
        return check;
    }

    public void setEditedColor() {
        check.setForeground((Color) CPropertyManager.getInstance().getGlobal("EDITED_COLOR"));
    }

    public void resetEditedColor() {
        check.setForeground(Color.BLACK);
    }

    public void setEditable(int i) {
        if (i == 0) {
            check.setEnabled(false);
        } else check.setEnabled(i == 1);
    }

    public Object getValue() {
        if (check.isSelected()) {
            return "1";
        } else {
            return null;
        }
    }

    public void setValue(Object o) {
        check.setSelected(o != null && o.toString().equalsIgnoreCase("1"));
        lostFocus();
    }

    @Override
    public void setColor(Color c) {
        super.setColor(c);
        check.setBackground(c);
    }

}
