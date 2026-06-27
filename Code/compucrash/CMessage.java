package compucrash;

import java.util.logging.Logger;

public class CMessage {

    private static final Logger LOGGER = Logger.getLogger(CMessage.class.getName());

    public static void print(Object o) {
        if (CPropertyManager.DEBUG) {
            LOGGER.info(String.valueOf(o));
        }
    }
}
