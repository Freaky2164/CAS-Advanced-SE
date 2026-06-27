package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CMainFrame extends CFrame {

    private static final Logger LOGGER = Logger.getLogger(CMainFrame.class.getName());
    JMenuBar mBar = new JMenuBar();
    JMenu mHelp = new JMenu("Hilfe");
    JMenuItem miAbout = new JMenuItem("�ber...");

    public CMainFrame() {
        this(null);
    }

    public CMainFrame(CFrame parent) {
        super(parent);
        setTitle("Hauptdialog");
        mHelp.add(miAbout);
        mBar.add(mHelp);
        setJMenuBar(mBar);
        miAbout.addActionListener(e -> new CAboutFrame(null));
        JPanel p1 = new JPanel();
        p1.setBackground(Color.WHITE);
        p1.setLayout(new BorderLayout());
        getContentPane().add(p1, BorderLayout.NORTH);
        JLabel titleLabel = new JLabel(CPropertyManager.getInstance().getProperty("title"), new ImageIcon(CPropertyManager.getInstance().getProperty("icon")), 0);
        p1.add(titleLabel, BorderLayout.NORTH);
        CTabbedPane tabbedPane = new CTabbedPane();
        getMainPane().setLayout(new BorderLayout());
        getMainPane().add(tabbedPane, BorderLayout.CENTER);
        // Tabs erzeugen
        try {
            ResultSet rset = CDataManager.getInstance().getMainPanels();
            while (rset.next()) {
                String label = rset.getString(1);
                if (tabbedPane.getTab(label) == null) {
                    tabbedPane.addTab(label);
                    tabbedPane.getTab(label).setLayout(new BorderLayout());
                    JPanel p = new JPanel();
                    p.setLayout(new FlowLayout(FlowLayout.LEFT));
                    tabbedPane.getTab(label).add(p);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load main panels", e);
            System.exit(0);
        }
        // Objekte erzeugen
        try {
            ResultSet rset = CDataManager.getInstance().getObjects();
            while (rset.next()) {
                String actionCommand = rset.getString(1);
                String label = rset.getString(2);
                String panel = rset.getString(3);
                JButton button = new JButton(label);
                button.setActionCommand(actionCommand);
                button.addActionListener(this::buttonAction);
                button.setPreferredSize(new Dimension(300, 120));
                ((JPanel) (tabbedPane.getTab(panel).getComponent(3))).add(button);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load objects", e);
            System.exit(0);
        }
        // Buttons erzeugen
        try {
            ResultSet rset = CDataManager.getInstance().getCustMainButtons();
            while (rset.next()) {
                String panel = rset.getString(1);
                String label = rset.getString(3);
                String actionCommand = rset.getString(7);
                CProperties prop = new CProperties();
                prop.put("label", label);
                prop.put("icon", null);
                prop.put("position", 0);
                prop.put("command", actionCommand);
                CButton button = new CButton(prop);
                button.setPreferredSize(new Dimension(300, 120));
                ((JPanel) (tabbedPane.getTab(panel).getComponent(3))).add(button);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load custom buttons", e);
            System.exit(0);
        }
//		Datenimport
		/*
		CProperties wumbea = new CProperties();
		wumbea.put("label","W+M Be- und Abz�ge");
		wumbea.put("icon", null);
		wumbea.put("position",0);
		wumbea.put("command","CCommandImportWuMBeA");
		CButton buttonBea = new CButton(wumbea);
		pWuM.add(buttonBea);
		buttonBea.setPreferredSize(new Dimension(300,120));
		
		CProperties wumlohn = new CProperties();
		wumlohn.put("label","W+M Lohnliste");
		wumlohn.put("icon", null);
		wumlohn.put("position",0);
		wumlohn.put("command","CCommandImportWuMLohn");
		CButton buttonLohn = new CButton(wumlohn);
		pWuM.add(buttonLohn);
		buttonLohn.setPreferredSize(new Dimension(300,120));		

		CProperties report1 = new CProperties();
		report1.put("label","Adressliste privat");
		report1.put("icon", null);
		report1.put("position",0);
		report1.put("command","CReportAdresslistePrivat");
		CButton buttonreport1 = new CButton(report1);
		pReports.add(buttonreport1);
		buttonreport1.setPreferredSize(new Dimension(300,120));

		CProperties report2 = new CProperties();
		report2.put("label","Telefonliste gesch�ftlich");
		report2.put("icon", null);
		report2.put("position",0);
		report2.put("command","CReportTelefonlisteGesch�ftlich");
		CButton buttonreport2 = new CButton(report2);
		pReports.add(buttonreport2);
		buttonreport2.setPreferredSize(new Dimension(300,120));

		CProperties report3 = new CProperties();
		report3.put("label","Geburtstags- und BZG-Liste");
		report3.put("icon", null);
		report3.put("position",0);
		report3.put("command","CReportGeburtstagsBZGListe");
		CButton buttonreport3 = new CButton(report3);
		pReports.add(buttonreport3);
		buttonreport3.setPreferredSize(new Dimension(300,120));

		CProperties report4 = new CProperties();
		report4.put("label","Personalbogen");
		report4.put("icon", null);
		report4.put("position",0);
		report4.put("command","CReportPersonalbogen");
		CButton buttonreport4 = new CButton(report4);
		pReports.add(buttonreport4);
		buttonreport4.setPreferredSize(new Dimension(300,120));

		CProperties report5 = new CProperties();
		report5.put("label","Gehalt nach Monaten/Jahren");
		report5.put("icon", null);
		report5.put("position",0);
		report5.put("command","CReportGehalt");
		CButton buttonreport5 = new CButton(report5);
		pReports.add(buttonreport5);
		buttonreport5.setPreferredSize(new Dimension(300,120));
//		buttonreport5.setEnabled(false);

		CProperties report6 = new CProperties();
		report6.put("label","Leistungspr�mie nach Grund und Extrapr�mie");
		report6.put("icon", null);
		report6.put("position",0);
		report6.put("command","CReportLeistungspraemie");
		CButton buttonreport6 = new CButton(report6);
		pReports.add(buttonreport6);
		buttonreport6.setPreferredSize(new Dimension(300,120));

		CProperties report7 = new CProperties();
		report7.put("label","Krankmeldungen");
		report7.put("icon", null);
		report7.put("position",0);
		report7.put("command","CReportKrankmeldungen");
		CButton buttonreport7 = new CButton(report7);
		pReports.add(buttonreport7);
		buttonreport7.setPreferredSize(new Dimension(300,120));

		CProperties report8 = new CProperties();
		report8.put("label","BMBF Report");
		report8.put("icon", null);
		report8.put("position",0);
		report8.put("command","CReportBMBF");
		CButton buttonreport8 = new CButton(report8);
		pReports.add(buttonreport8);
		buttonreport8.setPreferredSize(new Dimension(300,120));

		CProperties report9 = new CProperties();
		report9.put("label","Gehaltsreport (Gruppen)");
		report9.put("icon", null);
		report9.put("position",0);
		report9.put("command","CReportGehaltsgruppierung");
		CButton buttonreport9 = new CButton(report9);
		pReports.add(buttonreport9);
		buttonreport9.setPreferredSize(new Dimension(300,120));

		CProperties report10 = new CProperties();
		report10.put("label","AT-Erh�hungen");
		report10.put("icon", null);
		report10.put("position",0);
		report10.put("command","CReportATErhoehungen");
		CButton buttonreport10 = new CButton(report10);
		pReports.add(buttonreport10);
		buttonreport10.setPreferredSize(new Dimension(300,120));
*/
        setSize(1024, 768 - 32);
        setVisible(true);
    }

    protected void buttonAction(ActionEvent e) {
        Object o = CPropertyManager.getInstance().getDialog(e.getActionCommand() + ".list");
        if (o == null) {
            new CListFrame(e.getActionCommand(), this);
        } else {
            ((CListFrame) o).toFront();
            ((CListFrame) o).setVisible(true);
        }
    }

    protected void reportAction(ActionEvent e) {
        CProperties p = new CProperties();
        CProperties pA = new CProperties();
        p.put(Integer.toString(1), pA);
        pA.put("label", "Personalnummer");
        pA.put("equals", "1");
        pA.put("between", "1");
        pA.put("like", "1");
        new CReportFrame(p);
    }

    public void dispose() {
        CPropertyManager.getInstance().dispose();
        System.exit(0);
    }

}
