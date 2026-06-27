package compucrash;

import java.awt.*;

public class CHistoryDialog extends CFrame {

    protected CButton bCancel;
    protected CProperties p;
    protected CInfoFrame frame;
    protected CDisplayFieldHistoryBean history;

    public CHistoryDialog(CProperties oldP, CInfoFrame frame, CDisplayFieldHistoryBean history) throws HeadlessException {
        super(frame);
        this.p = CProperties.copyOf(oldP);
        this.frame = frame;
        this.history = history;
        bCancel = CButtonFactory.getButton("cancel");
        bCancel.addActionListener(e -> cancel());
        getButtonPaneRight().add(bCancel);

        p.put("data_type", "TABLE");
        CDisplayField field = new CDisplayField(p, frame);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        getMainPaneTopLeft().add(field, c);

        setVisible(true);
        pack();
    }

    protected void cancel() {
//	    history.refresh(1);
        history.refresh();
        dispose();
    }

}
