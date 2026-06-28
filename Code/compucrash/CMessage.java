package compucrash;

import java.text.MessageFormat;
import java.util.logging.Logger;

public class CMessage {
    private static final Logger LOGGER = Logger.getLogger(CMessage.class.getName());


    private CMessage() {
        /* This utility class should not be instantiated */
    }

    public static void print(Object o) {
        CPropertyManager.getInstance();
        if (CPropertyManager.isDebug()) {
            LOGGER.info(MessageFormat.format("Debug is {0}", o));
        }
    }
}
