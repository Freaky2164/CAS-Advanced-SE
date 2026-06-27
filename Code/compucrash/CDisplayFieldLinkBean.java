package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;

public class CDisplayFieldLinkBean extends CDisplayFieldBean {

    private final JLabel label = new JLabel();
    private final JTextField text = new JTextField();
    private final CButton bSource;
    private final CButton bLink;

    public CDisplayFieldLinkBean(CProperties p, CInfoFrame frame) {
        super(p, frame);
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(label);
        bSource = CButtonFactory.getButton("dropdown");
        add(bSource);
        bSource.addActionListener(this::bSource);
        bLink = CButtonFactory.getButton("dropdown");
        bLink.addActionListener(this::bLink);
        add(text);
        add(bLink);
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
        text.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                CMessage.print("focusGained");
            }

            public void focusLost(FocusEvent e) {
                CMessage.print("focusLost");
                lostFocus();
            }
        });
    }

    public void setEditedColor() {
        text.setForeground((Color) CPropertyManager.getInstance().getGlobal("EDITED_COLOR"));
    }

    public void resetEditedColor() {
        text.setForeground(Color.BLACK);
    }


    protected void bSource(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        Object importdir = CPropertyManager.getInstance().getProperties().get("importdir");
        if (importdir != null) {
            chooser.setCurrentDirectory(new File(importdir.toString()));
        }
        int state = chooser.showOpenDialog(null);
        File fin = chooser.getSelectedFile();
        if (fin != null && state == JFileChooser.APPROVE_OPTION) {
            text.setText(fin.getAbsolutePath());
        }
    }

    protected void bLink() {
        bLink(null);
    }

    protected void bLink(ActionEvent e) {
        try {
            new ProcessBuilder("explorer", text.getText()).start();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

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
