package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CMainFrameOld extends CFrame {

    private static final Logger LOGGER = Logger.getLogger(CMainFrameOld.class.getName());
    JMenuBar mBar = new JMenuBar();
    JMenu mHelp = new JMenu("Hilfe");
    JMenuItem miAbout = new JMenuItem("�ber...");

    public CMainFrameOld() {
        this(null);
    }

    public CMainFrameOld(CFrame parent) {
        super(parent);
        setTitle("Hauptdialog");
        mHelp.add(miAbout);
        mBar.add(mHelp);
        setJMenuBar(mBar);
        getMainPane().setLayout(new FlowLayout(FlowLayout.LEFT));
        miAbout.addActionListener(e -> new CAboutFrame(null));
        try {
            ResultSet rset = CDataManager.getInstance().getObjects();
            while (rset.next()) {
                String actionCommand = rset.getString(1);
                String label = rset.getString(2);
                JButton button = new JButton(label);
                button.setActionCommand(actionCommand);
                button.addActionListener(this::buttonAction);
                getMainPane().add(button);
                button.setPreferredSize(new Dimension(300, 120));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load objects", e);
            System.exit(0);
        }
        setSize(640, 480);
        setVisible(true);
    }

    protected void buttonAction(ActionEvent e) {
        new CListFrame(e.getActionCommand(), this);
    }

    public void dispose() {
        System.exit(0);
    }
}
