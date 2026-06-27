package compucrash;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class CManagingOracle extends CManagingDatabase {

	void connect(Properties p) {
	    CDataManager.schemaSeparator = ".";
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
		ResultSet rset = null;
		Object o = null;
		try {
			Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
			String SQLString = "SELECT " + init + " FROM DUAL";
			System.out.println("CManagingDatabase:getInit(p):");
			System.out.println(SQLString);
			rset = stmt.executeQuery(SQLString);
			rset.first();
			o = rset.getObject(1);	
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return o;
	}

}