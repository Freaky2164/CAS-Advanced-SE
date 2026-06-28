/*
 * Created on 04.01.2006
 */
package compucrash;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * @author Peter
 * <p>
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class XFrame extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(XFrame.class.getName());

    public XFrame() throws HeadlessException {
        super();
        // TODO Auto-generated constructor stub
        setContentPane(new JDesktopPane());
        LOGGER.fine("Content pane: " + getContentPane().getClass());
        getContentPane().setLayout(new FlowLayout());
        getContentPane().add(new JButton("xxx"));
        JInternalFrame intf = new JInternalFrame("Titel", true, true, true);
        intf.setPreferredSize(new Dimension(100, 200));
        intf.setVisible(true);
        getContentPane().add(intf);
        getContentPane().doLayout();
        setSize(100, 300);
        setVisible(true);
    }

    static void main() {
        new XFrame();
    }
}
