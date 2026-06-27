package compucrash;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class CLoginFrame extends CFrame {
	
	JTextField tfUser = new JTextField(20);
	JPasswordField tfPassword = new JPasswordField(20);
	
	public CLoginFrame(CFrame parent) {
		super(parent);
		setTitle("Anmeldung");
		GridBagConstraints c = new GridBagConstraints();
		JLabel lUser = new JLabel(" Benutzer   ");
		JLabel lPassword = new JLabel(" Passwort   ");
		c.gridwidth = GridBagConstraints.RELATIVE;
		getMainPaneTopLeft().add(lUser, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		getMainPaneTopLeft().add(tfUser,c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		getMainPaneTopLeft().add(lPassword);
		c.gridwidth = GridBagConstraints.REMAINDER;
		getMainPaneTopLeft().add(tfPassword);
		JButton bOk = new JButton("Ok");
		JButton bCancel = new JButton("Abbrechen");
		getButtonPaneLeft().add(bOk);
		getButtonPaneRight().add(bCancel);
		tfUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tfUserActionPerformed();
			}		
		});
		tfPassword.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tfPasswordActionPerformed();
			}		
		});
		tfUser.setText(CPropertyManager.getInstance().getProperty("dbuser"));
		bOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bOkActionPerformed();
			}			
		});
		bCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bCancelActionPerformed();
			}			
		});
		
		pack();
		setResizable(false);
		setVisible(true);
	}

	protected void tfPasswordActionPerformed() {
		if (tfUser.getText().length() > 0) bOkActionPerformed();
	}

	protected void tfUserActionPerformed() {
	}

	protected void bCancelActionPerformed() {
		System.exit(0);
	}

	protected void bOkActionPerformed() {
		CPropertyManager.USER = tfUser.getText();
		CPropertyManager.PWD = new String(tfPassword.getPassword());
		String mainFrame = CDataManager.getInstance().getMainFrame();
		if (mainFrame == null) {
			new CMainFrame();
		} else {
			try {
				Class.forName(mainFrame).newInstance();
			} catch (InstantiationException e) {
				new CMainFrame();
			} catch (IllegalAccessException e) {
				new CMainFrame();
			} catch (ClassNotFoundException e) {
				new CMainFrame();
			}
		}
		dispose();
	}
}