package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.text.ParseException;

public class CDisplayFieldNumberBean extends CDisplayFieldBean implements CSelectParent {

    private final JTextField text = new JTextField();
    private final NumberFormat nf = NumberFormat.getInstance();
    private CButton bSource;
    private CSelectDialog selectDialog = null;

    public CDisplayFieldNumberBean(CProperties p, CInfoFrame frame) {
        super(p, frame);
        setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel();
        add(label);
        add(text);
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
        if (p.get("data_precision") != null) {
            try {
                int dataPrecision = 0;
                dataPrecision = Integer.parseInt(p.get("data_precision").toString());
                nf.setMaximumFractionDigits(dataPrecision);
                nf.setMinimumFractionDigits(dataPrecision);
            } catch (NumberFormatException _) {
                /*Das ist halt so zum Catchen*/
            }
        }
    }

    public Component getTextField() {
        return text;
    }

    public void setEditedColor() {
        text.setForeground((Color) CPropertyManager.getInstance().getGlobal("EDITED_COLOR"));
    }

    public void resetEditedColor() {
        text.setForeground(Color.BLACK);
    }


    public String getText() {
        Number value;
        try {
            value = nf.parse(text.getText());
        } catch (ParseException _) {
            return null;
        }
        return value.toString();
    }

    public void setEditable(int i) {
        boolean bool = i == 1;
        text.setEditable(bool);
    }

    public Object getValue() {
        Number value;
        try {
            value = nf.parse(text.getText());
        } catch (ParseException _) {
            return null;
        }
        return value;
    }

    public void setValue(Object o) {
        if (o == null) {
            text.setText(null);
            return;
        }
        text.setText(nf.format(o));
        lostFocus();
    }

    protected void bSource(ActionEvent e) {
        Component o = this;
        int offsetX = text.getX();
        int offsetY = text.getY();
        if (selectDialog != null) {
            selectDialog.setVisible(true);
            selectDialog.toFront();
            return;
        }
        while (o.getClass() != CInfoFrame.class) {
            o = o.getParent();
            offsetX += o.getX();
            offsetY += o.getY();
        }
        if (p.get("source").equals("SELF")) {
            selectDialog = new CSelectDialog(o, this, p);
        } else {
            selectDialog = new CSelectDialog(o, this, CDataObjectFactory.getCListDataObject((String) p.get("source")));
        }
        positionSelectDialog(o, offsetX, offsetY);
    }

    private void positionSelectDialog(Component o, int offsetX, int offsetY) {
        try {
            ((CInfoFrame) o).getDesktopPane().add(selectDialog, JLayeredPane.MODAL_LAYER);
        } catch (Exception _) {
            /* Auch hier das gleiche ne*/
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

    public void resetSelectDialog() {
        selectDialog = null;
    }

    public CButton getbSource() {
        return bSource;
    }

    public void setbSource(CButton bSource) {
        this.bSource = bSource;
    }
}
