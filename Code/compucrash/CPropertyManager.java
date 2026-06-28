package compucrash;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CPropertyManager {

    public static final String VERSION = "1.1.1";
    private static final Logger LOGGER = Logger.getLogger(CPropertyManager.class.getName());
    private static final Properties properties = new Properties();
    private static boolean debug = false;
    private static String user;
    private static String pwd;
    private static String iniFile = "Compucrash.ini";
    private static NumberFormat nf = NumberFormat.getInstance();
    private static CPropertyManager uniqueInstance = null;
    private final CProperties dialogs = new CProperties();
    private final CProperties globals = new CProperties();

    public static void setDebug(boolean debug) {
        CPropertyManager.debug = debug;
    }
    private CPropertyManager() {
        super();

// Load properties from file		
        try (FileInputStream inFile = new FileInputStream(iniFile)) {
            properties.load(inFile);
            if (properties.get("debug") != null && properties.get("debug").toString().equalsIgnoreCase("true")) {
                /*hier soll debuggt werden*/
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            System.exit(0);
        }
    }

    public static CPropertyManager getInstance() {
        if (uniqueInstance == null) {
            uniqueInstance = new CPropertyManager();
        }
        return uniqueInstance;
    }

    public static void getInstance(String file) {
        if (file != null) iniFile = file;
        getInstance();
    }

    public static boolean isDebug() {
        return debug;
    }

    public static String getUser() {
        return user;
    }

    public static void setUser(String user) {
        CPropertyManager.user = user;
    }

    public static String getPwd() {
        return pwd;
    }

    public static void setPwd(String pwd) {
        CPropertyManager.pwd = pwd;
    }

    public static NumberFormat getNf() {
        return nf;
    }

    public static void setNf(NumberFormat nf) {
        CPropertyManager.nf = nf;
    }

    public static void dispose() {
        try (FileOutputStream outFile = new FileOutputStream(iniFile)) {
            properties.store(outFile, null);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        uniqueInstance = null;
    }

    public void setDialog(String dialog, Object ref) {
        if (ref == null) {
            dialogs.remove(dialog);
        } else {
            dialogs.put(dialog, ref);
        }
    }

    public Object getDialog(String dialog) {
        return dialogs.get(dialog);
    }

    public Properties getProperties() {
        return properties;
    }

    public String getProperty(String property) {
        return properties.getProperty(property);
    }

    public void setProperty(String property, String value) {
        properties.setProperty(property, value);
    }

    public Object getGlobal(String name) {
        if (globals.size() == 0) prepareGlobals();
        return globals.get(name);
    }

    public void prepareGlobals() {
        globals.put("init", "NULL");
        ResultSet rset = CDataManager.getInstance().getGlobals();
        try {
            while (rset.next()) {
                String name = rset.getString(1);
                String value = rset.getString(2);
                String type = rset.getString(3);
                if (type.equalsIgnoreCase("BOOLEAN")) {
                    if (value.equalsIgnoreCase("TRUE")) {
                        globals.put(name, Boolean.TRUE);
                    } else if (value.equalsIgnoreCase("FALSE")) {
                        globals.put(name, Boolean.FALSE);
                    }
                } else if (type.equalsIgnoreCase("COLOR")) {
                    // Farbe zuweisen
                    globals.put(name,
                            new Color(
                                    Integer.parseInt(value.substring(0, 3)),
                                    Integer.parseInt(value.substring(3, 6)),
                                    Integer.parseInt(value.substring(6))
                            )
                    );
                } else if (type.equalsIgnoreCase("INTEGER")) {
                    // Integerwert
                    globals.put(name, Integer.valueOf(value));
                } else if (type.equalsIgnoreCase("FLOAT")) {
                    // Floatwert
                    globals.put(name, Double.valueOf(value));
                } else if (type.equalsIgnoreCase("CHAR")) {
                    // String
                    globals.put(name, value);
                }
            }
        } catch (SQLException _) {
            // Nothing to do
        }
    }
}
