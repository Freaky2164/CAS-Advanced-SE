package compucrash;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class CDisplayFieldLinkBean extends CDisplayFieldBean {

	private JLabel label = new JLabel();
	private JTextField text = new JTextField();
	private CButton bSource;
	private CButton bLink;
	
	public CDisplayFieldLinkBean(CProperties p, CInfoFrame frame) {
		super(p, frame);
		setLayout(new FlowLayout(FlowLayout.LEFT));
		add(label);
		bSource = CButtonFactory.getButton("dropdown");
		add(bSource);
		bSource.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bSource(e);
			}			
		});
		bLink = CButtonFactory.getButton("dropdown");
		bLink.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bLink(e);
			}			
		});
		add(text);
		add(bLink);
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
		
	public void setEditedColor() {
	    text.setForeground((Color)CPropertyManager.getInstance().getGlobal("EDITED_COLOR"));
	}
	
	public void resetEditedColor() {
	    text.setForeground(Color.BLACK);
	}
	
	protected void bSource(ActionEvent e) {
		JFileChooser chooser = new JFileChooser();
//		chooser.addChoosableFileFilter(new CPdfFileFilter());
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
	
	protected void bLink(ActionEvent e) {
		Runtime r = Runtime.getRuntime();
		Process p = null;
		try {
			p = r.exec("explorer " + text.getText());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}
	
	public Component getTextField() {
	    return text;
	}

	public void setValue(Object obj) {
		if (obj == null) {
			text.setText(null);
		} else {
			text.setText(obj.toString());	
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
}
