package compucrash;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class CMessageFrame extends CFrame {
	
	public CMessageFrame(String message) throws HeadlessException {
		super(null);
		CButton bCancel = CButtonFactory.getButton("cancel");
		bCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
		getButtonPaneRight().add(bCancel);
		JTextArea ta = new JTextArea(message);
		JScrollPane sp = new JScrollPane(ta);
		getMainPane().add(sp);
		setSize(800,500);
		setVisible(true);
	}

	protected void cancel() {
		dispose();
	}

}
