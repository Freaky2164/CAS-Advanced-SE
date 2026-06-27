package compucrash;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

//Test
/* Test2 */
/* Test3 
asdsda
asdasd
*/

public class CManagingSQLServer extends CManagingDatabase {

	void connect(Properties p) {
	    CDataManager.schemaSeparator = ".";
		driverManager = "net.sourceforge.jtds.jdbc.Driver";
		connection = "jdbc:jtds:sqlserver://" 
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
		String SQLString = "SELECT " + init;
		try {
			Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
//			System.out.println("CManagingDatabase:getInit(p):");
//			System.out.println(SQLString);
			rset = stmt.executeQuery(SQLString);
			rset.first();
			o = rset.getObject(1);	
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println(SQLString);
		}
		return o;
	}


}
