package compucrash;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

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
		setSize(400,300);
		setVisible(true);
	}
    public static void main(String[] args) {
        new Test();
    }
}
