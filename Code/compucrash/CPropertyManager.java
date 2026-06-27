package compucrash;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CPropertyManager {

    public static final String version = "1.1.1";
    private static final Logger LOGGER = Logger.getLogger(CPropertyManager.class.getName());
    public static boolean DEBUG = false;
    public static String USER;
    public static String PWD;
    // Erste Stelle Hauptrelease
    // Zweite Stelle: �nderungen an der DB
    // Dritte Stelle Ereiterungen und Patches
    public static String iniFile = "Compucrash.ini";
    public static NumberFormat nf = NumberFormat.getInstance();
    private static CPropertyManager uniqueInstance = null;
    private final Properties properties = new Properties();
    private final CProperties dialogs = new CProperties();
    private final CProperties globals = new CProperties();

    private CPropertyManager() {
        super();

// Load properties from file		
        try (FileInputStream inFile = new FileInputStream(iniFile)) {
            properties.load(inFile);
            if (properties.get("debug") != null && properties.get("debug").toString().equalsIgnoreCase("true"))
                DEBUG = true;
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

    public void dispose() {
        try (FileOutputStream outFile = new FileOutputStream(iniFile)) {
            properties.store(outFile, null);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        uniqueInstance = null;
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
        } catch (SQLException e) {
            // Nothing to do
        }
    }
}
