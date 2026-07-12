package compucrash;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class CDisplayFieldDateBean extends CDisplayFieldBean {

	private JLabel label = new JLabel();
	private JTextField text = new JTextField();
	
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
		DateTime dt = null;
		if (o == null) {
			text.setText(null);
			return;
		}
		if (o instanceof String) {
			DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy");
			dt = fmt.withLocale(Locale.GERMAN).parseDateTime(o.toString());
		} else {
			dt = new DateTime(o);
		}
		DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy");
		text.setText(fmt.print(dt));
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
		DateTimeFormatter fmt = DateTimeFormat.forPattern("dd.MM.yyyy");
		DateTime dt = fmt.withLocale(Locale.GERMAN).parseDateTime(text.getText());
		return dt;		
	}
 /*   public void setColor(Color c) {
        this.setBackground(c);        
    }*/

}
