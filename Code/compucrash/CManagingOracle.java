package compucrash;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CManagingOracle extends CManagingDatabase {

    private static final Logger LOGGER = Logger.getLogger(CManagingOracle.class.getName());

    void connect(Properties p) {
        CDataManager.setSchemaSeparator(".");
        driverManager = "oracle.jdbc.driver.OracleDriver";
        connection = "jdbc:oracle:thin:@"
                + p.getProperty("dbhost")
                + ":" + p.getProperty("dbport")
                + ":"
                + p.getProperty("dbsid");
        user = p.getProperty("dbuser");
        pwd = p.getProperty("dbpwd");
        super.connect();
    }

    CListDataManagingDatabase createCListDataManagingDatabase(CListDataObject parent) {
        return new CListDataManagingOracle(parent);
    }

    CInfoDataManagingDatabase createCInfoDataManagingDatabase(CInfoDataObject parent) {
        return new CInfoDataManagingOracle(parent);
    }

    public Object getInit(String init) {
        try {
            String sqlString = "SELECT " + validateSqlExpression(init) + " FROM DUAL";
            try (Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                 ResultSet resultSet = stmt.executeQuery(sqlString)) {
                resultSet.first();
                return resultSet.getObject(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize Oracle value", e);
        }
        return null;
    }

}
