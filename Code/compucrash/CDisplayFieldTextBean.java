package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class CDisplayFieldTextBean extends CDisplayFieldBean {

    private static final String PROP_LABEL = "label";
    private static final String PROP_LABEL_LENGTH = "label_length";
    private static final String PROP_TOOLTIP = "tooltip";
    private static final String PROP_DATA_SCALE = "data_scale";
    private static final String PROP_DATA_HEIGHT = "data_height";
    private static final String GLOBAL_EDITED_COLOR = "EDITED_COLOR";

    private static final int LABEL_WIDTH_MULTIPLIER = 7;
    private static final int DEFAULT_TEXT_ROWS = 3;
    private static final int EDITABLE_FLAG = 1;

    private static final Color READ_ONLY_BACKGROUND = new Color(238, 238, 238);

    private final JLabel label = new JLabel();
    private final JTextArea text = new JTextArea();
    private final JScrollPane sp = new JScrollPane(text);

    public CDisplayFieldTextBean(CProperties p, CInfoFrame frame) {
        super(p, frame);
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(label);
        add(sp);
        String labelString = (String) p.get(PROP_LABEL);
        label.setText(labelString);
        label.setPreferredSize(
                new Dimension(
                        Integer.parseInt((String) p.get(PROP_LABEL_LENGTH)) * LABEL_WIDTH_MULTIPLIER,
                        label.getPreferredSize().height));
        if (p.get(PROP_TOOLTIP) != null) {
            setToolTipText((String) p.get(PROP_TOOLTIP));
        }
        text.setColumns(Integer.parseInt((String) p.get(PROP_DATA_SCALE)));
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        int dataHeight = Integer.parseInt((String) p.get(PROP_DATA_HEIGHT));
        if (dataHeight != 0) {
            text.setRows(dataHeight);
        } else {
            text.setRows(DEFAULT_TEXT_ROWS);
        }
        text.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                /*This method is empty somehow*/
            }

            public void focusLost(FocusEvent e) {
                lostFocus();
            }
        });
    }

    public void setEditedColor() {
        text.setForeground((Color) CPropertyManager.getInstance().getGlobal(GLOBAL_EDITED_COLOR));
    }

    public void resetEditedColor() {
        text.setForeground(Color.BLACK);
    }


    public Component getTextField() {
        return text;
    }

    public String getText() {
        return text.getText();
    }

    public void setEditable(int i) {
        boolean bool = i == EDITABLE_FLAG;
        text.setEditable(bool);
        if (bool) {
            text.setBackground(Color.WHITE);
        } else {
            text.setBackground(READ_ONLY_BACKGROUND);
        }
    }

    public Object getValue() {
        return getText();
    }

    public void setValue(Object o) {
        if (o == null) {
            text.setText(null);
            return;
        }
        text.setText(o.toString());
        lostFocus();
    }

}
