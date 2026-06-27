/*
 * Created on 04.01.2006
 */
package compucrash;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.HeadlessException;

import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;

/**
 * @author Peter
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class XFrame extends JFrame {

    public XFrame() throws HeadlessException {
        super();
        // TODO Auto-generated constructor stub
        setContentPane(new JDesktopPane());
        System.out.println(getContentPane().getClass());
        getContentPane().setLayout(new FlowLayout());
        getContentPane().add(new JButton("xxx"));
        JInternalFrame intf = new JInternalFrame("Titel",true,true,true);
        intf.setPreferredSize(new Dimension(100,200));
        intf.setVisible(true);
        getContentPane().add(intf);
        getContentPane().doLayout();
        setSize(100,300);
        setVisible(true);
    }

public static void main( String[] args ) {
    System.out.println("Test XFrame");
    new XFrame();
}
}
