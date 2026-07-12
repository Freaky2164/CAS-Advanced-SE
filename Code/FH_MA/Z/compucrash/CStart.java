package compucrash;
import java.util.Locale;

// Adobe Acrobat Reader Preferences - optional, commented out due to missing JAR
// import com.adobe.acrobat.gui.ReaderPrefs;

public class CStart {

	public static void main(String[] args) {
		CSplashScreen splash = new CSplashScreen();
		Locale.setDefault(Locale.GERMANY);
		if (args.length > 0) {
			CPropertyManager.getInstance(args[0]);
		} else {
			CPropertyManager.getInstance();
		}
	    // Adobe Reader initialization - skipped if JAR not available
	    // try {
        //     ReaderPrefs.initialize();
    	//     ReaderPrefs.restoreDefaults();
    	//     ReaderPrefs.readerPrefs.setProperty("com.adobe.acrobat.AcceptedLicAgreement","true");
        // } catch (Exception e1) {
        //     e1.printStackTrace();
        // }

		new CLoginFrame(null);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		splash.dispose();
	}
}
