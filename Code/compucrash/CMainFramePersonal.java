package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CMainFramePersonal extends CFrame {
    private static final String STAMMDATEN = "Stammdaten";
    private static final String ADMINISTRATION = "Administration";
    private static final String REPORTS = "Reports";
    private static final String COMMAND = "command";
    private static final String WUNDM = "W + M";
    private static final String LABEL = "label";
    private static final Logger LOGGER = Logger.getLogger(CMainFramePersonal.class.getName());
    private static final String POSITION = "position";
    JMenuBar mBar = new JMenuBar();
    JMenu mHelp = new JMenu("Hilfe");
    JMenuItem miAbout = new JMenuItem("Über...");

    public CMainFramePersonal() {
        this(null);
    }

    public CMainFramePersonal(CFrame parent) {
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
        tabbedPane.addTab(STAMMDATEN);
        tabbedPane.getTab(STAMMDATEN).setLayout(new BorderLayout());
        tabbedPane.getTab(STAMMDATEN).add(pStamm);
        tabbedPane.addTab(REPORTS);
        tabbedPane.getTab(REPORTS).setLayout(new BorderLayout());
        tabbedPane.getTab(REPORTS).add(pReports);
        tabbedPane.addTab(WUNDM);
        tabbedPane.getTab(WUNDM).setLayout(new BorderLayout());
        tabbedPane.getTab(WUNDM).add(pWuM);
        tabbedPane.addTab(ADMINISTRATION);
        tabbedPane.getTab(ADMINISTRATION).setLayout(new BorderLayout());
        tabbedPane.getTab(ADMINISTRATION).add(pAdmin);
        try {
            ResultSet rset = CDataManager.getInstance().getObjects();
            while (rset.next()) {
                String actionCommand = rset.getString(1);
                String label = rset.getString(2);
                JButton button = new JButton(label);
                button.setActionCommand(actionCommand);
                button.addActionListener(this::buttonAction);
                if (button.getText().equalsIgnoreCase("Personalstammdaten")) {
                    pStamm.add(button);
                } else {
                    pAdmin.add(button);
                }
                button.setPreferredSize(new Dimension(300, 120));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load objects", e);
            System.exit(0);
        }
        CProperties wumbea = new CProperties();
        wumbea.put(LABEL, "W+M Be- und Abz�ge");
        wumbea.put("icon", null);
        wumbea.put(POSITION, 0);
        wumbea.put(COMMAND, "CCommandImportWuMBeA");
        CButton buttonBea = new CButton(wumbea);
        pWuM.add(buttonBea);
        buttonBea.setPreferredSize(new Dimension(300, 120));

        CProperties wumlohn = new CProperties();
        wumlohn.put(LABEL, "W+M Lohnliste");
        wumlohn.put("icon", null);
        wumlohn.put(POSITION, 0);
        wumlohn.put(COMMAND, "CCommandImportWuMLohn");
        CButton buttonLohn = new CButton(wumlohn);
        pWuM.add(buttonLohn);
        buttonLohn.setPreferredSize(new Dimension(300, 120));

        CProperties report1 = new CProperties();
        report1.put(LABEL, "Adressliste privat");
        report1.put("icon", null);
        report1.put(POSITION, 0);
        report1.put(COMMAND, "CReportAdresslistePrivat");
        CButton buttonreport1 = new CButton(report1);
        pReports.add(buttonreport1);
        buttonreport1.setPreferredSize(new Dimension(300, 120));

        CProperties report2 = new CProperties();
        report2.put(LABEL, "Telefonliste gesch�ftlich");
        report2.put("icon", null);
        report2.put(POSITION, 0);
        report2.put(COMMAND, "CReportTelefonlisteGesch�ftlich");
        CButton buttonreport2 = new CButton(report2);
        pReports.add(buttonreport2);
        buttonreport2.setPreferredSize(new Dimension(300, 120));

        CProperties report3 = new CProperties();
        report3.put(LABEL, "Geburtstags- und BZG-Liste");
        report3.put("icon", null);
        report3.put(POSITION, 0);
        report3.put(COMMAND, "CReportGeburtstagsBZGListe");
        CButton buttonreport3 = new CButton(report3);
        pReports.add(buttonreport3);
        buttonreport3.setPreferredSize(new Dimension(300, 120));

        CProperties report4 = new CProperties();
        report4.put(LABEL, "Personalbogen");
        report4.put("icon", null);
        report4.put(POSITION, 0);
        report4.put(COMMAND, "CReportPersonalbogen");
        CButton buttonreport4 = new CButton(report4);
        pReports.add(buttonreport4);
        buttonreport4.setPreferredSize(new Dimension(300, 120));

        CProperties report5 = new CProperties();
        report5.put(LABEL, "Gehalt nach Monaten/Jahren");
        report5.put("icon", null);
        report5.put(POSITION, 0);
        report5.put(COMMAND, "CReportGehalt");
        CButton buttonreport5 = new CButton(report5);
        pReports.add(buttonreport5);
        buttonreport5.setPreferredSize(new Dimension(300, 120));

        CProperties report6 = new CProperties();
        report6.put(LABEL, "Leistungspr�mie nach Grund und Extrapr�mie");
        report6.put("icon", null);
        report6.put(POSITION, 0);
        report6.put(COMMAND, "CReportLeistungspraemie");
        CButton buttonreport6 = new CButton(report6);
        pReports.add(buttonreport6);
        buttonreport6.setPreferredSize(new Dimension(300, 120));

        CProperties report7 = new CProperties();
        report7.put(LABEL, "Krankmeldungen");
        report7.put("icon", null);
        report7.put(POSITION, 0);
        report7.put(COMMAND, "CReportKrankmeldungen");
        CButton buttonreport7 = new CButton(report7);
        pReports.add(buttonreport7);
        buttonreport7.setPreferredSize(new Dimension(300, 120));

        CProperties report8 = new CProperties();
        report8.put(LABEL, "BMBF Report");
        report8.put("icon", null);
        report8.put(POSITION, 0);
        report8.put(COMMAND, "CReportBMBF");
        CButton buttonreport8 = new CButton(report8);
        pReports.add(buttonreport8);
        buttonreport8.setPreferredSize(new Dimension(300, 120));

        CProperties report9 = new CProperties();
        report9.put(LABEL, "Gehaltsreport (Gruppen)");
        report9.put("icon", null);
        report9.put(POSITION, 0);
        report9.put(COMMAND, "CReportGehaltsgruppierung");
        CButton buttonreport9 = new CButton(report9);
        pReports.add(buttonreport9);
        buttonreport9.setPreferredSize(new Dimension(300, 120));

        CProperties report10 = new CProperties();
        report10.put(LABEL, "AT-Erh�hungen");
        report10.put("icon", null);
        report10.put(POSITION, 0);
        report10.put(COMMAND, "CReportATErhoehungen");
        CButton buttonreport10 = new CButton(report10);
        pReports.add(buttonreport10);
        buttonreport10.setPreferredSize(new Dimension(300, 120));

        setSize(1024, 768 - 32);
        setVisible(true);
    }

    protected void buttonAction(ActionEvent e) {
        new CListFrame(e.getActionCommand(), this);
    }

    protected void reportAction() {
        CProperties p = new CProperties();
        CProperties pA = new CProperties();
        p.put(Integer.toString(1), pA);
        pA.put(LABEL, "Personalnummer");
        pA.put("equals", "1");
        pA.put("between", "1");
        pA.put("like", "1");
        new CReportFrame(p);
    }

    @Override
    public void dispose() {
        System.exit(0);
    }

}
