package compucrash;

import javax.swing.*;
import java.awt.*;

public class CAboutFrame extends CFrame {

    public CAboutFrame(CFrame parent) throws HeadlessException {
        super(null);
        JLabel logo = new JLabel(new ImageIcon("../images/compucrash.gif"));
        getMainPaneTop().add(logo);
        JTextArea lizenzText = new JTextArea();
        lizenzText.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(lizenzText);
        getMainPane().add(sp);
        String textLizenz = """
                Lizenzvereinbarung
                Dieses Softwareprodukt ist ausschlie�lich f�r die bioPharm GmbH lizenziert.
                
                Alle Rechte verbleiben bei Compucrash IT-Services Tanja Reisle.
                
                Danksagung
                
                Die Autorin möchte sich bei den Programmierern der JODA Bibliotheken für 
                die Datumskonvertierung
                bedanken. Ebenso fur die Arbeit des Apache POI Projektes, mit dem EXCEL-Tabellen ein- und ausgegeben 
                werden. 
                """;
        lizenzText.setText(textLizenz);
        CButton bCancel = CButtonFactory.getButton("cancel");
        bCancel.addActionListener(e -> bCancel());
        getButtonPaneRight().add(bCancel);

        pack();
        setVisible(true);
    }

    protected void bCancel() {
        dispose();
    }
}
