package compucrash;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CTableHeaderSettingsFrame extends CFrame {

	public CTableHeaderSettingsFrame() {
		super(null);
		JPanel cp = getMainPane();
		cp.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridwidth = GridBagConstraints.RELATIVE;
		cp.add(new JLabel("größer"), c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		cp.add(new JTextField(30),c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		cp.add(new JLabel("kleiner"), c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		cp.add(new JTextField(30),c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		cp.add(new JLabel("enthält     "), c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		cp.add(new JTextField(30),c);
		
		getButtonPaneRight().add(CButtonFactory.getButton("cancel"));
		getButtonPaneLeft().add(CButtonFactory.getButton("ok"));
		getButtonPaneLeft().add(CButtonFactory.getButton("apply"));
		
		pack();
		setVisible(true);
	}
	
	public static void main(String[] args) {
		CTableHeaderSettingsFrame window = new CTableHeaderSettingsFrame();
	}
}
