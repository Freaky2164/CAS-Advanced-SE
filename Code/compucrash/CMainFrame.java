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

    protected void reportAction() {
        CProperties p = new CProperties();
        CProperties pA = new CProperties();
        p.put(Integer.toString(1), pA);
        pA.put("label", "Personalnummer");
        pA.put("equals", "1");
        pA.put("between", "1");
        pA.put("like", "1");
        new CReportFrame(p);
    }

    @Override
    public void dispose() {
        CPropertyManager.getInstance();
        CPropertyManager.dispose();
        System.exit(0);
    }

}
