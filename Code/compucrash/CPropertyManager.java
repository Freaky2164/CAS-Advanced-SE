package compucrash;
import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Properties;

public class CPropertyManager {

	public static boolean DEBUG = false;
    private static CPropertyManager uniqueInstance = null;
	public static String USER;
	public static String PWD;
	public static final String version = "1.1.1";
	// Erste Stelle Hauptrelease
	// Zweite Stelle: Änderungen an der DB
	// Dritte Stelle Ereiterungen und Patches
	public static String iniFile = "Compucrash.ini";
	public static NumberFormat nf = NumberFormat.getInstance();
	private Properties properties = new Properties();
	private CProperties dialogs = new CProperties();
	private CProperties globals = new CProperties();
	
	public CPropertyManager() {
		super();
		uniqueInstance = this;
		
// Load properties from file		
		try {
			properties.load(new FileInputStream(iniFile));
			if (properties.get("debug") != null && properties.get("debug").toString().equalsIgnoreCase("true")) DEBUG = true;
		} catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}
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
	    FileOutputStream outFile;
        try {
            outFile = new FileOutputStream(iniFile);
            properties.store(outFile, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
		uniqueInstance = null;
	}
	
	public static CPropertyManager getInstance() {
		if (uniqueInstance == null) {
			new CPropertyManager();
		}
		return uniqueInstance;
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

	public static void getInstance(String file) {
		if (file != null) iniFile = file;
		getInstance();
	}
	
	public Object getGlobal(String name) {
	    if (globals.size() == 0) prepareGlobals();
	    return globals.get(name);
	}
	
	public void prepareGlobals() {
	    globals.put("init","NULL");
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
                        globals.put(name,Boolean.FALSE);
                    }
                } else if (type.equalsIgnoreCase("COLOR")) {
                    // Farbe zuweisen
                    globals.put(name,
                            new Color(
                            Integer.parseInt(value.substring(0,3)),
                            Integer.parseInt(value.substring(3,6)),
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
