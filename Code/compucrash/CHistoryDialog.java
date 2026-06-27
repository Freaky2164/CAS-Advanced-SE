package compucrash;

import java.awt.GridBagConstraints;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CHistoryDialog extends CFrame {

	protected CButton bCancel;
	protected CProperties p;
	protected CInfoFrame frame;
	protected CDisplayFieldHistoryBean history;	

    public CHistoryDialog(CProperties oldP, CInfoFrame frame, CDisplayFieldHistoryBean history) throws HeadlessException {
        super(frame);
        this.p = (CProperties)oldP.clone();
        this.frame = frame;
        this.history = history;
		bCancel = CButtonFactory.getButton("cancel");
		bCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
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
