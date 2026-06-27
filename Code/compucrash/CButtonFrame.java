package compucrash;

import java.awt.*;

public class CButtonFrame extends CFrame {

    protected CButton bCancel;

    public CButtonFrame(CProperties p) throws HeadlessException {
        super(null);
        bCancel = CButtonFactory.getButton("cancel");
        bCancel.addActionListener(e -> cancel());
        getButtonPaneRight().add(bCancel);
        getMainPaneTop().setLayout(new GridLayout(0, 1));
        for (int i = 1; i <= p.size(); i++) {
            CProperties pB = (CProperties) p.get(Integer.toString(i));
            if (pB == null) continue;
            CButton button = new CButton(pB);
            button.getCommand().setOwner(p.get("owner"));
            getMainPaneTop().add(button);
        }
        pack();
        setVisible(true);
    }

    protected void cancel() {
        dispose();
    }

}
