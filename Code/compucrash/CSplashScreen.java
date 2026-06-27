package compucrash;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JWindow;

public class CSplashScreen extends JWindow {

	public CSplashScreen() {
		super();
		getContentPane().add(new JButton(new ImageIcon("../images/flashScreenImage.jpg")));
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		setBounds(screen.width/2-getPreferredSize().width/2, 
		          screen.height/2-getPreferredSize().height/2,  
				  getPreferredSize().width,
				  getPreferredSize().height
		          );
		setVisible(true);
	}
}
