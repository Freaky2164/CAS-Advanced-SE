package compucrash;

import javax.swing.*;
import java.awt.*;

public class CTableHeaderSettingsFrame extends CFrame {
    private CTableHeaderSettingsFrame window = null;
    public CTableHeaderSettingsFrame() {
        super(null);
        JPanel cp = getMainPane();
        cp.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridwidth = GridBagConstraints.RELATIVE;
        cp.add(new JLabel("gr��er"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        cp.add(new JTextField(30), c);
        c.gridwidth = GridBagConstraints.RELATIVE;
        cp.add(new JLabel("kleiner"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        cp.add(new JTextField(30), c);
        c.gridwidth = GridBagConstraints.RELATIVE;
        cp.add(new JLabel("enth�lt     "), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        cp.add(new JTextField(30), c);

        getButtonPaneRight().add(CButtonFactory.getButton("cancel"));
        getButtonPaneLeft().add(CButtonFactory.getButton("ok"));
        getButtonPaneLeft().add(CButtonFactory.getButton("apply"));

        pack();
        setVisible(true);
    }

    void main() {
        this.window = new CTableHeaderSettingsFrame();
    }

    public CTableHeaderSettingsFrame getWindow() {
        return window;
    }

    public void setWindow(CTableHeaderSettingsFrame window) {
        this.window = window;
    }
}
