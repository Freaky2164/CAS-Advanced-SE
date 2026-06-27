package compucrash;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class CDisplayFieldCharFrameBean extends CDisplayFieldBean implements CSelectParent {

	private JLabel label = new JLabel();
	private JTextField text = new JTextField();
	private CButton bSource;
	private CButton bSpringen;
	private CSelectDialogFrame selectDialog = null;
	
	public CDisplayFieldCharFrameBean(CProperties p, CInfoFrame frame) {
		super(p, frame);
		setLayout(new FlowLayout(FlowLayout.LEFT));
		add(label);
		if (p.get("source") != null) {
			bSource = CButtonFactory.getButton("dropdown");
			add(bSource);
			bSource.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					bSource(e);
				}			
			});
		}
		add(text);
		if (p.get("springen") != null) {
			bSpringen = CButtonFactory.getButton("dropdown");
			add(bSpringen);
			bSpringen.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					bSpringen(e);
				}			
			});
		}
		String labelString = (String)p.get("label");
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
		
	protected void bSpringen(ActionEvent e) {
	    if (text.getText() != null && text.getText().length() > 0) {
			CProperties pS = new CProperties();
			pS.put("object_name", p.get("springen"));
			CProperties keys = CDataObjectFactory.getCListDataObject(p.get("springen").toString()).getKeys();
			for (int i = 1; i <= keys.size();i++) {
			    CProperties pK = (CProperties)keys.get(Integer.toString(i));
			    if (pK != null) {
			        String value = p.get("owner").toString() + "." + 
			        p.get("table_name").toString() + "." + 
			        p.get("column_name").toString();
			        pK.put("value",frame.getAttributeValue(value));
					CMessage.print(value);
					CMessage.print(frame.getAttributeValue(value));
			    }
			}
			pS.put("keys",keys);
			CMessage.print(pS);
			new CInfoFrame(CInfoFrame.EDIT, pS, null);
	    }
    }

	protected void bSource(ActionEvent e) {
		Component o = this;
		if (selectDialog instanceof CSelectDialogFrame) {
			selectDialog.setVisible(true);
			selectDialog.toFront();
			return;
		}
		while (o != null && o.getClass() != CInfoFrame.class) {
			o = o.getParent();
		}
		if (((String)p.get("source")).equals("SELF")) {
			selectDialog = new CSelectDialogFrame(o,this,p);
		} else {
			selectDialog = new CSelectDialogFrame(o,this,CDataObjectFactory.getCListDataObject((String)p.get("source")));
		} 
	}
	
	public Component getTextField() {
	    return text;
	}

	public void setValue(Object o) {
		if (o == null) {
			text.setText(null);
		} else {
			text.setText(o.toString());		    
		}
	    lostFocus();
		return;
	}

	public void setEditable(int i) {
		if (i == 0) {
			text.setEditable(false);
			if (bSource != null) bSource.setEnabled(false);
		} else if (i == 1) {
			text.setEditable(true);
			if (bSource != null) bSource.setEnabled(true);
		} else {
			text.setEditable(false);
			if (bSource != null) bSource.setEnabled(true);
		}
	}

	public Object getValue() {
		if (text.getText().equalsIgnoreCase("")) return null;
		return text.getText();
	}

    public void setColor(Color c) {
        super.setColor(c);
        if (bSource != null) bSource.setBackground(c);  
    }

	public void resetSelectDialog() {
	    selectDialog = null;
	}
	
	public void setEditedColor() {
	    text.setForeground((Color)CPropertyManager.getInstance().getGlobal("EDITED_COLOR"));
	}
	
	public void resetEditedColor() {
	    text.setForeground(Color.BLACK);
	}

}
