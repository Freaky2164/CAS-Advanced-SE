package compucrash;

import javax.swing.*;
import java.awt.*;

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
        getMainPaneTopLeft().add(tfUser, c);
        c.gridwidth = GridBagConstraints.RELATIVE;
        getMainPaneTopLeft().add(lPassword);
        c.gridwidth = GridBagConstraints.REMAINDER;
        getMainPaneTopLeft().add(tfPassword);
        JButton bOk = new JButton("Ok");
        JButton bCancel = new JButton("Abbrechen");
        getButtonPaneLeft().add(bOk);
        getButtonPaneRight().add(bCancel);
        tfUser.addActionListener(e -> tfUserActionPerformed());
        tfPassword.addActionListener(e -> tfPasswordActionPerformed());
        tfUser.setText(CPropertyManager.getInstance().getProperty("dbuser"));
        bOk.addActionListener(e -> bOkActionPerformed());
        bCancel.addActionListener(e -> bCancelActionPerformed());

        pack();
        setResizable(false);
        setVisible(true);
    }

    protected void tfPasswordActionPerformed() {
        if (!tfUser.getText().isEmpty()) bOkActionPerformed();
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
                Class.forName(mainFrame).getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                new CMainFrame();
            }
        }
        dispose();
    }
}
