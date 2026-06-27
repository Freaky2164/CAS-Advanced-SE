package compucrash;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class CDisplayFieldDateBean extends CDisplayFieldBean {

	private JLabel label = new JLabel();
	private JTextField text = new JTextField();
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	
	public CDisplayFieldDateBean(CProperties p, CInfoFrame frame) {
		super(p, frame);
		setLayout(new FlowLayout(FlowLayout.LEFT));
		add(label);
		add(text);
		String labelString = (String)p.get("label") + "";
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
	    text.setForeground((Color)CPropertyManager.getInstance().getGlobal("EDITED_COLOR"));
	}
	
	public void resetEditedColor() {
	    text.setForeground(Color.BLACK);
	}
	
	public void setValue(Object o) {
		LocalDate dt = null;
		if (o == null) {
			text.setText(null);
			return;
		}
		if (o instanceof LocalDate) {
			dt = (LocalDate) o;
		} else if (o instanceof java.sql.Date) {
			dt = ((java.sql.Date) o).toLocalDate();
		} else if (o instanceof java.sql.Timestamp) {
			dt = ((java.sql.Timestamp) o).toLocalDateTime().toLocalDate();
		} else if (o instanceof Date) {
			dt = ((Date) o).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		} else {
			String value = o.toString().trim();
			if (value.length() == 0) {
				text.setText("");
				return;
			}
			try {
				dt = LocalDate.parse(value, FORMATTER);
			} catch (DateTimeParseException ex) {
				dt = LocalDate.parse(value);
			}
		}
		text.setText(FORMATTER.format(dt));
	}

	public void setEditable(int i) {
		boolean bool = false;
		if (i == 1) bool = true;
		text.setEditable(bool);
	}
	
	public Object getValue() {
		if (text.getText() == null || text.getText().equalsIgnoreCase("")) {
			return null;
		}
		LocalDate dt = LocalDate.parse(text.getText(), FORMATTER);
		return dt;		
	}
 /*   public void setColor(Color c) {
        this.setBackground(c);        
    }*/

}
