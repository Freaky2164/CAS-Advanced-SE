package compucrash;

import javax.swing.*;
import java.awt.*;

public class CSplashScreen extends JWindow {

    public CSplashScreen() {
        super();
        getContentPane().add(new JButton(new ImageIcon("../images/flashScreenImage.jpg")));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        setBounds(screen.width / 2 - getPreferredSize().width / 2,
                screen.height / 2 - getPreferredSize().height / 2,
                getPreferredSize().width,
                getPreferredSize().height
        );
        setVisible(true);
    }
}
