package compucrash;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

public class CDisplayFieldCheckBean extends CDisplayFieldBean /*implements CSelectParent*/ {

	private JLabel label = new JLabel();
	private JCheckBox check = new JCheckBox();
	
	public CDisplayFieldCheckBean(CProperties p, CInfoFrame frame) {
		super(p, frame);
		setLayout(new FlowLayout(FlowLayout.LEFT));
		add(label);
		add(check);
		String labelString = (String)p.get("label") + "";
		label.setText(labelString);
		label.setPreferredSize(
			new Dimension(
				Integer.parseInt((String) p.get("label_length")) * 7,
				label.getPreferredSize().height));
		if (p.get("tooltip") != null) {
			setToolTipText((String) p.get("tooltip"));
		}
		check.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               lostFocus();
            }		    
		});
	}
	
    public Component getTextField() {
	    return check;
	}
	public void setValue(Object o) {
		if (o == null || !o.toString().equalsIgnoreCase("1")) {
			check.setSelected(false);
		} else {
		    check.setSelected(true);
		}
		lostFocus();
	}
	
	public void setEditedColor() {
	    check.setForeground((Color)CPropertyManager.getInstance().getGlobal("EDITED_COLOR"));
	}
	
	public void resetEditedColor() {
	    check.setForeground(Color.BLACK);
	}
	
	public void setEditable(int i) {
		if (i == 0) {
			check.setEnabled(false);
		} else if (i == 1) {
			check.setEnabled(true);
		} else {
			check.setEnabled(false);
		}
	}

	public Object getValue() {
	    if (check.isSelected()) {
	        return "1";
	    } else {
	        return null;
	    }
	}

    public void setColor(Color c) {
        super.setColor(c);
        check.setBackground(c);        
    }

}
