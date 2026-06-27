package compucrash;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.JDesktopPane;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class CDisplayFieldNumberBean extends CDisplayFieldBean implements CSelectParent {

	private JLabel label = new JLabel();
	private JTextField text = new JTextField();
	private CButton bSource;
	private CSelectDialog selectDialog = null;
	private NumberFormat nf = NumberFormat.getInstance();
	
	public CDisplayFieldNumberBean(CProperties p, CInfoFrame frame) {
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
		if (p.get("data_precision") != null) {
			try {
				int dataPrecision = 0;
				dataPrecision = Integer.parseInt(p.get("data_precision").toString());
				nf.setMaximumFractionDigits(dataPrecision);
				nf.setMinimumFractionDigits(dataPrecision);
			} catch (NumberFormatException nfe) {}
		}
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
	

	public String getText() {
		Number value;
			try {
				value = nf.parse(text.getText());
			} catch (ParseException e) {
				return null;
			}
		return value.toString();
	}
	
	public void setValue(Object o) {
		if (o == null) {
			text.setText(null);
			return;
		}
		text.setText(nf.format(o));
		lostFocus();
	}

	public void setEditable(int i) {
		boolean bool = false;
		if (i == 1) bool = true;
		text.setEditable(bool);
	}
	
	public Object getValue() {
		Number value;
		try {
			value = nf.parse(text.getText());
		} catch (ParseException e) {
			return null;
		}
	return value;
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
		while (o != null && o.getClass() != CInfoFrame.class) {
			o = o.getParent();
		    offsetX += o.getX();
		    offsetY += o.getY();
		}
		if (((String)p.get("source")).equals("SELF")) {
			selectDialog = new CSelectDialog(o,this,p);
		} else {
			selectDialog = new CSelectDialog(o,this,CDataObjectFactory.getCListDataObject((String)p.get("source")));
		}
		if (o instanceof CInfoFrame) {
		    try {
		    ((CInfoFrame)o).getDesktopPane().add(selectDialog,JDesktopPane.MODAL_LAYER);
		    } catch (Exception ex) {
		    }
		    int x = offsetX;
		    if (x + selectDialog.getWidth() > o.getWidth()) {
		        x = o.getWidth() - selectDialog.getWidth();
		    }
		    if (x < 0) {
		        x = 0;
		        selectDialog.setSize(o.getWidth(),selectDialog.getHeight());
		    }
		    int y = offsetY;
		    if (y + selectDialog.getHeight() > o.getHeight() - 30) {
		        y = o.getHeight() - 30 - selectDialog.getHeight();
		    }
		    if (y < 0) {
		        y = 0;
		        selectDialog.setSize(selectDialog.getWidth(),o.getHeight() - 30);
		    }		    
		    selectDialog.setBounds(x,y,selectDialog.getWidth(),selectDialog.getHeight());
		}
	}

    public void resetSelectDialog() {
	    selectDialog = null;
    }

}
