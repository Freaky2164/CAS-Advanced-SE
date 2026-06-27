package compucrash;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;

public class CButton extends JButton {

	public static final int RIGHT = 0;
	public static final int LEFT = 1;
	public static final int TOP = 2;
	public static final int BOTTOM = 3;
	
	private String bez;
	
	private CCommand command = null;
	
	public CButton(CProperties p) {
		super((String)p.get("label"), (Icon)p.get("icon"));
		switch (((Integer)p.get("position")).intValue()) {
			case LEFT :
				this.setHorizontalTextPosition(JButton.LEFT);
				break;
			case TOP :
				this.setHorizontalTextPosition(JButton.CENTER);
				this.setVerticalTextPosition(JButton.TOP);
				break;
			case BOTTOM :
				this.setHorizontalTextPosition(JButton.CENTER);
				this.setVerticalTextPosition(JButton.BOTTOM);
				break;
			default :
			case RIGHT :
				this.setHorizontalTextPosition(JButton.RIGHT);
		}
		this.setToolTipText((String)p.get("tooltip"));
		this.bez = (String)p.get("bez");
		if (p.get("mnemonic") != null) {
		    setMnemonic(Integer.parseInt(p.get("mnemonic").toString()));
		}
		if (p.get("command") != null) {
			try {
				Class c = Class.forName(p.get("command").toString());
				command = (CCommand)c.newInstance();
			} catch (ClassNotFoundException e) {				
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (command != null) command.execute(null);
			}			
		});
	}
	
	public void setCCommand( CCommand command) {
		this.command = command;
	}
	
	public boolean hasCommand() {
		if (command == null) return false;
		return true;
	}
	
	public CCommand getCommand() {
		return command;
	}
		
	public String toString() {
		return bez;
	}
	
	
}
