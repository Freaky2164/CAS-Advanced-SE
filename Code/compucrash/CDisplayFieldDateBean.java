package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class CDisplayFieldDateBean extends CDisplayFieldBean {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final JLabel label = new JLabel();
    private final JTextField text = new JTextField();

    public CDisplayFieldDateBean(CProperties p, CInfoFrame frame) {
        super(p, frame);
        setLayout(new FlowLayout(FlowLayout.LEFT));
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

    public Component getTextField() {
        return text;
    }

    public void setEditedColor() {
        text.setForeground((Color) CPropertyManager.getInstance().getGlobal("EDITED_COLOR"));
    }

    public void resetEditedColor() {
        text.setForeground(Color.BLACK);
    }

    public void setEditable(int i) {
        boolean bool = i == 1;
        text.setEditable(bool);
    }

    public Object getValue() {
        if (text.getText() == null || text.getText().equalsIgnoreCase("")) {
            return null;
        }

        return LocalDate.parse(text.getText(), FORMATTER);
    }

    public void setValue(Object o) {
        LocalDate dt = null;
        switch (o) {
            case null -> {
                text.setText(null);
                return;
            }
            case LocalDate localDate -> dt = localDate;
            case java.sql.Date date -> dt = date.toLocalDate();
            case java.sql.Timestamp timestamp -> dt = timestamp.toLocalDateTime().toLocalDate();
            case java.util.Date date -> dt = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            default -> {
                String value = o.toString().trim();
                if (value.isEmpty()) {
                    text.setText("");
                    return;
                }
                try {
                    dt = LocalDate.parse(value, FORMATTER);
                } catch (DateTimeParseException _) {
                    dt = LocalDate.parse(value);
                }
            }
        }
        text.setText(FORMATTER.format(dt));
    }
}
