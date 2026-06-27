package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.logging.Logger;

public class CDisplayFieldCharBean extends CDisplayFieldBean implements CSelectParent {

    private static final String PROP_SOURCE = "source";
    private static final String PROP_SPRINGEN = "springen";

    private final JLabel label = new JLabel();
    private final JTextField text = new JTextField();
    private CButton bSource;
    private CButton bSpringen;
    private CSelectDialog selectDialog = null;

    public CDisplayFieldCharBean(CProperties p, CInfoFrame frame) {
        super(p, frame);
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(label);
        if (p.get(PROP_SOURCE) != null) {
            bSource = CButtonFactory.getButton("dropdown");
            add(bSource);
            bSource.addActionListener(this::bSource);
        }
        add(text);
        if (p.get(PROP_SPRINGEN) != null) {
            bSpringen = CButtonFactory.getButton("dropdown");
            add(bSpringen);
            bSpringen.addActionListener(this::bSpringen);
        }
        String labelString = (String) p.get("label");
        label.setText(labelString);
        label.setPreferredSize(
                new Dimension(
                        Integer.parseInt((String) p.get("label_length")) * 7,
                        label.getPreferredSize().height));
        if (p.get("tooltip") != null) {
            setToolTipText((String) p.get("tooltip"));
        }
        text.setColumns(Integer.parseInt((String) p.get("data_scale")));
        text.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                lostFocus();
            }
        });
    }

    protected void bSpringen(ActionEvent e) {
        if (text.getText() != null && !text.getText().isEmpty()) {
            CProperties pS = new CProperties();
            pS.put("objectName", p.get(PROP_SPRINGEN));
            CProperties keys = CDataObjectFactory.getCListDataObject(p.get(PROP_SPRINGEN).toString()).getKeys();
            for (int i = 1; i <= keys.size(); i++) {
                CProperties pK = (CProperties) keys.get(Integer.toString(i));
                if (pK != null) {
                    String value = p.get("owner").toString() + "." +
                            p.get("table_name").toString() + "." +
                            p.get("column_name").toString();
                    pK.put("value", frame.getAttributeValue(value));
                    CMessage.print(value);
                    CMessage.print(frame.getAttributeValue(value));
                }
            }
            pS.put("keys", keys);
            CMessage.print(pS);
            new CInfoFrame(CInfoFrame.EDIT, pS, null);
        }
    }

    public void resetSelectDialog() {
        selectDialog = null;
    }

    public void setEditedColor() {
        text.setForeground((Color) CPropertyManager.getInstance().getGlobal("EDITED_COLOR"));
    }

    public void resetEditedColor() {
        text.setForeground(Color.BLACK);
    }

    protected void bSource(ActionEvent e) {
        Component o = this;
        int offsetX = text.getX();
        int offsetY = text.getY();
        if (selectDialog instanceof CSelectDialog) {
            selectDialog.setVisible(true);
            selectDialog.toFront();
            return;
        }
        while (o.getClass() != CInfoFrame.class) {
            o = o.getParent();
            offsetX += o.getX();
            offsetY += o.getY();
        }
        if (p.get(PROP_SOURCE).equals("SELF")) {
            selectDialog = new CSelectDialog(o, this, p);
        } else {
            selectDialog = new CSelectDialog(o, this, CDataObjectFactory.getCListDataObject((String) p.get(PROP_SOURCE)));
        }
        if (o instanceof CInfoFrame) {
            positionSelectDialog(o, offsetX, offsetY);
        }
    }

    private void positionSelectDialog(Component o, int offsetX, int offsetY) {
        try {
            ((CInfoFrame) o).getDesktopPane().add(selectDialog, JLayeredPane.MODAL_LAYER);
        } catch (Exception e) {
            Logger.getLogger(e.toString());
        }
        int x = offsetX;
        if (x + selectDialog.getWidth() > o.getWidth()) {
            x = o.getWidth() - selectDialog.getWidth();
        }
        if (x < 0) {
            x = 0;
            selectDialog.setSize(o.getWidth(), selectDialog.getHeight());
        }
        int y = offsetY;
        if (y + selectDialog.getHeight() > o.getHeight() - 30) {
            y = o.getHeight() - 30 - selectDialog.getHeight();
        }
        if (y < 0) {
            y = 0;
            selectDialog.setSize(selectDialog.getWidth(), o.getHeight() - 30);
        }
        selectDialog.setBounds(x, y, selectDialog.getWidth(), selectDialog.getHeight());
    }

    public Component getTextField() {
        return text;
    }

    public void setEditable(int i) {
        switch (i) {
            case 0 -> {
                text.setEditable(false);
                if (bSource != null) bSource.setEnabled(false);
            }
            case 1 -> {
                text.setEditable(true);
                if (bSource != null) bSource.setEnabled(true);
            }
            default -> {
                text.setEditable(false);
                if (bSource != null) bSource.setEnabled(true);
            }
        }
    }

    public Object getValue() {
        if (text.getText().equalsIgnoreCase("")) return null;
        return text.getText();
    }

    public void setValue(Object obj) {
        if (obj == null) {
            text.setText(null);
        } else {
            text.setText(obj.toString());
        }
        lostFocus();
    }

    @Override
    public void setColor(Color c) {
        super.setColor(c);
        if (bSource != null) bSource.setBackground(c);
    }

}
