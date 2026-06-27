package compucrash;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class CDisplayFieldTextBean extends CDisplayFieldBean {

	private JLabel label = new JLabel();
	private JTextArea text = new JTextArea();
	private JScrollPane sp = new JScrollPane(text);
	
	public CDisplayFieldTextBean(CProperties p, CInfoFrame frame) {
		super(p, frame);
		setLayout(new FlowLayout(FlowLayout.LEFT));
		add(label);
		add(sp);
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
		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		if (Integer.parseInt((String)p.get("data_height")) != 0) {
			text.setRows(Integer.parseInt((String)p.get("data_height")));
		} else {
			text.setRows(3);		    
		}
		text.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
            }
            public void focusLost(FocusEvent e) {
                lostFocus();
            }		    
		});
	}

	public void setEditedColor() {
	    text.setForeground((Color)CPropertyManager.getInstance().getGlobal("EDITED_COLOR"));
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

	public void setValue(Object o) {
		if (o == null) {
			text.setText(null);
			return;
		}
		text.setText(o.toString());
		lostFocus();
	}

	public void setEditable(int i) {
		boolean bool = false;
		if (i == 1) bool = true;
		text.setEditable(bool);
		if (bool) {
			text.setBackground(Color.WHITE);
		} else {
			text.setBackground(new Color(238,238,238));
		}
	}

	public Object getValue() {
		return getText();
	}

}
