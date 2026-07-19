package compucrash;

import javax.swing.*;
import java.awt.*;

public class Test extends JFrame {
    public Test() {
        Container contentPane = getContentPane();
        JTabbedPane tp = new JTabbedPane();
        JPanel panelOne = new JPanel();
        JPanel panelTwo = new JPanel();

        panelOne.add(new JButton("button in panel 1"));
        panelTwo.add(new JButton("button in panel 2"));

        tp.add(panelOne, "Panel One");
        tp.addTab("Panel Two",
                new ImageIcon("c:/workspace/Compucrash/hauswert/images/document.gif"),
                panelTwo,
                "tooltip text");

        contentPane.setLayout(new BorderLayout());
        contentPane.add(tp);
        setSize(400, 300);
        setVisible(true);
    }
    static void main() {
        new Test();
    }
}
