package compucrash;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class CManagingMySQL extends CManagingDatabase {

	void connect(Properties p) {
	    CDataManager.schemaSeparator = "_";
		driverManager = "com.mysql.jdbc.Driver";
		connection = "jdbc:mysql://" 
			+ p.getProperty("dbhost")
			+ ":" + p.getProperty("dbport")
			+ "/"
			+ p.getProperty("dbsid");
		user = p.getProperty("dbuser");
		pwd = p.getProperty("dbpwd");
		super.connect();
	}

	CListDataManagingDatabase createCListDataManagingDatabase(CListDataObject parent) {
		return new CListDataManagingSQLServer(parent);
	}

	CInfoDataManagingDatabase createCInfoDataManagingDatabase(CInfoDataObject parent) {
		return new CInfoDataManagingSQLServer(parent);
	}
	
	public Object getInit(String init) {
		ResultSet rset = null;
		Object o = null;
		try {
			Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
			String SQLString = "SELECT " + init;
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
