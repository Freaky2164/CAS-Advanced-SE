package compucrash;

import javax.swing.*;
import java.awt.*;

public class CMessageFrame extends CFrame {

    public CMessageFrame(String message) throws HeadlessException {
        super(null);
        CButton bCancel = CButtonFactory.getButton("cancel");
        bCancel.addActionListener(e -> cancel());
        getButtonPaneRight().add(bCancel);
        JTextArea ta = new JTextArea(message);
        JScrollPane sp = new JScrollPane(ta);
        getMainPane().add(sp);
        setSize(800, 500);
        setVisible(true);
    }

    protected void cancel() {
        dispose();
    }

}
