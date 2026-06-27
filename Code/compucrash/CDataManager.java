package compucrash;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CDataManager {

    private static final Logger LOGGER = Logger.getLogger(CDataManager.class.getName());
    private static final String DATABASE_KEY = "database";
    private static String schemaSeparator = ".";
    private static CDataManager uniqueInstance = null;
    private final Connection conn;
    private final Properties properties;
    private CManagingDatabase manager;

    private CDataManager() {
        properties = CPropertyManager.getInstance().getProperties();
        if (properties.get(DATABASE_KEY).toString().equalsIgnoreCase("POSTGRES")
                || properties.get(DATABASE_KEY).toString().equalsIgnoreCase("SQLSERVER")) {
            manager = new CManagingPostgres();
        } else if (properties.get(DATABASE_KEY).toString().equalsIgnoreCase("ORACLE")) {
            manager = new CManagingOracle();
        } else if (properties.get(DATABASE_KEY).toString().equalsIgnoreCase("MYSQL")) {
            manager = new CManagingMySQL();
        } else {
            LOGGER.log(Level.SEVERE, "Keine unterstutzte Datenbank: {0}", properties.get(DATABASE_KEY));
            LOGGER.info("Unterstutzt werden: ORACLE, POSTGRES, SQLSERVER (alias for POSTGRES), MYSQL");
            System.exit(0);
        }

        manager.connect(properties);
        conn = manager.getConnection();
    }

    public static CDataManager getInstance() {
        if (uniqueInstance == null) {
            uniqueInstance = new CDataManager();
        }
        return uniqueInstance;
    }

    public static String getSchemaSeparator() {
        return schemaSeparator;
    }

    public static void setSchemaSeparator(String separator) {
        schemaSeparator = separator;
    }

    public static void dispose() {
        try {
            if (uniqueInstance != null) {
                uniqueInstance.conn.close();
            }
            uniqueInstance = null;
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "Failed to dispose database manager", se);
        }
    }

    public Statement getStatement() throws SQLException {
        return conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    }

    public Connection getConnection() {
        return conn;
    }

    public String getMainFrame() {
        return manager.getMainFrame();
    }

    public ResultSet getObjects() {
        return manager.getObjects();
    }

    public ResultSet getButtons() {
        return manager.getButtons();
    }

    public ResultSet getGlobals() {
        return manager.getGlobals();
    }

    public ResultSet getTables(String objectName) {
        return manager.getTables(objectName);
    }

    public ResultSet prepareCInfoDataObjects() {
        return manager.prepareCInfoDataObjects();
    }

    public ResultSet prepareCListDataObjects() {
        return manager.prepareCListDataObjects();
    }

    protected ResultSet getCInfoDataCustButtons(String objectName) {
        return manager.getCInfoDataCustButtons(objectName);
    }

    protected ResultSet getCListDataCustButtons(String objectName) {
        return manager.getCListDataCustButtons(objectName);
    }

    public ResultSet getSelect(CProperties p) {
        return manager.getSelect(p);
    }

    public Object getInit(String init) {
        return manager.getInit(init);
    }

    public CListDataManagingDatabase createCListDataManagingDatabase(CListDataObject parent) {
        return manager.createCListDataManagingDatabase(parent);
    }

    public CInfoDataManagingDatabase createCInfoDataManagingDatabase(CInfoDataObject parent) {
        return manager.createCInfoDataManagingDatabase(parent);
    }

    public ResultSet getMainPanels() {
        return manager.getMainPanels();
    }

    public ResultSet getCustMainButtons() {
        return manager.getCustMainButtons();
    }
}
