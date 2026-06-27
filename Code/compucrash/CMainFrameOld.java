package compucrash;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class CMainFrameOld extends CFrame {
	JMenuBar mBar = new JMenuBar();
	JMenu mHelp = new JMenu("Hilfe");
	JMenuItem miAbout = new JMenuItem("Über...");
	Hashtable buttons = new Hashtable();
	
	public CMainFrameOld() {
		this(null);
	}
	
	public CMainFrameOld(CFrame parent) {
		super(parent);
		setTitle("Hauptdialog");
		mHelp.add(miAbout);
		mBar.add(mHelp);
		setJMenuBar(mBar);
		getMainPane().setLayout( new FlowLayout(FlowLayout.LEFT));
		miAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new CAboutFrame(null);
			}			
		});
		try {
			ResultSet rset = CDataManager.getInstance().getObjects();
			while (rset.next()) {
				String actionCommand = rset.getString(1);
				String label = rset.getString(2);
				JButton button = new JButton(label);
				button.setActionCommand(actionCommand);
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						buttonAction(e);			
					}
				});
				getMainPane().add(button);
				button.setPreferredSize(new Dimension(300,120));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
		setSize(640,480);
		setVisible(true);
	}
	
	protected void buttonAction(ActionEvent e) {
		new CListFrame(e.getActionCommand().toString(), this);
	}

	public void dispose() {
		System.exit(0);
	}
}
