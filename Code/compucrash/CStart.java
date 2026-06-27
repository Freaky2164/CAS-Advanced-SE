package compucrash;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CStart {

    private static final Logger LOGGER = Logger.getLogger(CStart.class.getName());

    static void main(String[] args) {
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
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Splash screen sleep interrupted", e);
        }
        splash.dispose();
    }
}
