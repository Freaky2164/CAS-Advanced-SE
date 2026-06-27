package compucrash;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class CAboutFrame extends CFrame {

	public CAboutFrame(CFrame parent) throws HeadlessException {
		super(null);
		JLabel logo = new JLabel(new ImageIcon("../images/compucrash.gif"));
		getMainPaneTop().add(logo);
		JTextArea lizenzText = new JTextArea();
		lizenzText.setWrapStyleWord(true);
		JScrollPane sp = new JScrollPane(lizenzText);
		getMainPane().add(sp);
		String textLizenz = "Lizenzvereinbarung \nDieses Softwareprodukt ist ausschließlich für die bioPharm GmbH lizenziert.\n" +
			"Alle Rechte verbleiben bei Compucrash IT-Services Tanja Reisle.\n" +
			"Danksagung\n" +
			"Die Autorin möchte sich bei den Programmierern der JODA Bibliotheken für \ndie Datumskonvertierung " +
			"bedanken. Ebenso fur die Arbeit des Apache \nPOI Projektes, mit dem EXCEL-Tabellen ein- und ausgegeben " +
			"werden. ";
		lizenzText.setText(textLizenz);
		CButton bCancel = CButtonFactory.getButton("cancel");
		bCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bCancel();
			}			
		});
		getButtonPaneRight().add(bCancel);
		
		pack();
		setVisible(true);
	}

	protected void bCancel() {
		dispose();
	}
}
