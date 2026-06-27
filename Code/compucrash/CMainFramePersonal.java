package compucrash;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

public class CMainFramePersonal extends CFrame {
	JMenuBar mBar = new JMenuBar();
	JMenu mHelp = new JMenu("Hilfe");
	JMenuItem miAbout = new JMenuItem("Über...");
	Hashtable buttons = new Hashtable();

	public CMainFramePersonal() {
		this(null);
	}
	
	public CMainFramePersonal(CFrame parent) {
		super(parent);
		setTitle("Hauptdialog");
		mHelp.add(miAbout);
		mBar.add(mHelp);
		setJMenuBar(mBar);
		miAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new CAboutFrame(null);
			}			
		});
		JPanel p1 = new JPanel();
		p1.setBackground(Color.WHITE);
		p1.setLayout(new BorderLayout());
		getContentPane().add(p1, BorderLayout.NORTH);
		JLabel titleLabel = new JLabel("Biopharm GmbH Personaldatenbank", new ImageIcon("../images/biopharm.gif"), 0);
		p1.add(titleLabel, BorderLayout.NORTH);
		CTabbedPane tabbedPane = new CTabbedPane();
		getMainPane().setLayout(new BorderLayout());
		getMainPane().add(tabbedPane, BorderLayout.CENTER);
		JPanel pStamm = new JPanel();
		pStamm.setLayout(new FlowLayout(FlowLayout.LEFT));
		JPanel pReports = new JPanel();
		pReports.setLayout(new FlowLayout(FlowLayout.LEFT));
		JPanel pWuM = new JPanel();
		pWuM.setLayout(new FlowLayout(FlowLayout.LEFT));
		JPanel pAdmin = new JPanel();
		pAdmin.setLayout(new FlowLayout(FlowLayout.LEFT));
		tabbedPane.addTab("Stammdaten");
		tabbedPane.getTab("Stammdaten").setLayout(new BorderLayout());
		tabbedPane.getTab("Stammdaten").add(pStamm);
		tabbedPane.addTab("Reports");
		tabbedPane.getTab("Reports").setLayout(new BorderLayout());
		tabbedPane.getTab("Reports").add(pReports);
		tabbedPane.addTab("W + M");
		tabbedPane.getTab("W + M").setLayout(new BorderLayout());
		tabbedPane.getTab("W + M").add(pWuM);
		tabbedPane.addTab("Administration");
		tabbedPane.getTab("Administration").setLayout(new BorderLayout());
		tabbedPane.getTab("Administration").add(pAdmin);
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
				if (button.getText().equalsIgnoreCase("Personalstammdaten")) {
//					tabbedPane.getTab("Stammdaten").add(button);
					pStamm.add(button);
				} else {
//					tabbedPane.getTab("Administration").add(button);
					pAdmin.add(button);
				}
				button.setPreferredSize(new Dimension(300,120));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
//		Datenimport
		CProperties wumbea = new CProperties();
		wumbea.put("label","W+M Be- und Abzüge");
		wumbea.put("icon", null);
		wumbea.put("position",new Integer(0));
		wumbea.put("command","CCommandImportWuMBeA");
		CButton buttonBea = new CButton(wumbea);
		pWuM.add(buttonBea);
		buttonBea.setPreferredSize(new Dimension(300,120));
		
		CProperties wumlohn = new CProperties();
		wumlohn.put("label","W+M Lohnliste");
		wumlohn.put("icon", null);
		wumlohn.put("position",new Integer(0));
		wumlohn.put("command","CCommandImportWuMLohn");
		CButton buttonLohn = new CButton(wumlohn);
		pWuM.add(buttonLohn);
		buttonLohn.setPreferredSize(new Dimension(300,120));		

		CProperties report1 = new CProperties();
		report1.put("label","Adressliste privat");
		report1.put("icon", null);
		report1.put("position",new Integer(0));
		report1.put("command","CReportAdresslistePrivat");
		CButton buttonreport1 = new CButton(report1);
		pReports.add(buttonreport1);
		buttonreport1.setPreferredSize(new Dimension(300,120));

		CProperties report2 = new CProperties();
		report2.put("label","Telefonliste geschäftlich");
		report2.put("icon", null);
		report2.put("position",new Integer(0));
		report2.put("command","CReportTelefonlisteGeschäftlich");
		CButton buttonreport2 = new CButton(report2);
		pReports.add(buttonreport2);
		buttonreport2.setPreferredSize(new Dimension(300,120));

		CProperties report3 = new CProperties();
		report3.put("label","Geburtstags- und BZG-Liste");
		report3.put("icon", null);
		report3.put("position",new Integer(0));
		report3.put("command","CReportGeburtstagsBZGListe");
		CButton buttonreport3 = new CButton(report3);
		pReports.add(buttonreport3);
		buttonreport3.setPreferredSize(new Dimension(300,120));

		CProperties report4 = new CProperties();
		report4.put("label","Personalbogen");
		report4.put("icon", null);
		report4.put("position",new Integer(0));
		report4.put("command","CReportPersonalbogen");
		CButton buttonreport4 = new CButton(report4);
		pReports.add(buttonreport4);
		buttonreport4.setPreferredSize(new Dimension(300,120));

		CProperties report5 = new CProperties();
		report5.put("label","Gehalt nach Monaten/Jahren");
		report5.put("icon", null);
		report5.put("position",new Integer(0));
		report5.put("command","CReportGehalt");
		CButton buttonreport5 = new CButton(report5);
		pReports.add(buttonreport5);
		buttonreport5.setPreferredSize(new Dimension(300,120));
//		buttonreport5.setEnabled(false);

		CProperties report6 = new CProperties();
		report6.put("label","Leistungsprämie nach Grund und Extraprämie");
		report6.put("icon", null);
		report6.put("position",new Integer(0));
		report6.put("command","CReportLeistungspraemie");
		CButton buttonreport6 = new CButton(report6);
		pReports.add(buttonreport6);
		buttonreport6.setPreferredSize(new Dimension(300,120));

		CProperties report7 = new CProperties();
		report7.put("label","Krankmeldungen");
		report7.put("icon", null);
		report7.put("position",new Integer(0));
		report7.put("command","CReportKrankmeldungen");
		CButton buttonreport7 = new CButton(report7);
		pReports.add(buttonreport7);
		buttonreport7.setPreferredSize(new Dimension(300,120));

		CProperties report8 = new CProperties();
		report8.put("label","BMBF Report");
		report8.put("icon", null);
		report8.put("position",new Integer(0));
		report8.put("command","CReportBMBF");
		CButton buttonreport8 = new CButton(report8);
		pReports.add(buttonreport8);
		buttonreport8.setPreferredSize(new Dimension(300,120));

		CProperties report9 = new CProperties();
		report9.put("label","Gehaltsreport (Gruppen)");
		report9.put("icon", null);
		report9.put("position",new Integer(0));
		report9.put("command","CReportGehaltsgruppierung");
		CButton buttonreport9 = new CButton(report9);
		pReports.add(buttonreport9);
		buttonreport9.setPreferredSize(new Dimension(300,120));

		CProperties report10 = new CProperties();
		report10.put("label","AT-Erhöhungen");
		report10.put("icon", null);
		report10.put("position",new Integer(0));
		report10.put("command","CReportATErhoehungen");
		CButton buttonreport10 = new CButton(report10);
		pReports.add(buttonreport10);
		buttonreport10.setPreferredSize(new Dimension(300,120));

		setSize(1024,768-32);
		setVisible(true);
	}
	protected void buttonAction(ActionEvent e) {
		new CListFrame(e.getActionCommand().toString(), this);
	}
	protected void reportAction(ActionEvent e) {
		CProperties p = new CProperties();
		CProperties pA = new CProperties();
		p.put(Integer.toString(1),pA);
		pA.put("label", "Personalnummer");
		pA.put("equals", "1");
		pA.put("between", "1");
		pA.put("like", "1");
		new CReportFrame(p);
	}

	public void dispose() {
		System.exit(0);
	}

}
