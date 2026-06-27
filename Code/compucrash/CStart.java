package compucrash;
import java.util.Locale;


public class CStart {

	public static void main(String[] args) {
		CSplashScreen splash = new CSplashScreen();
		Locale.setDefault(Locale.GERMANY);
		if (args.length > 0) {
			CPropertyManager.getInstance(args[0]);
		} else {
			CPropertyManager.getInstance();
		}

		new CLoginFrame(null);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		splash.dispose();
	}
}
