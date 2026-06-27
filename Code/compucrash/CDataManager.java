package compucrash;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class CDataManager {

	private static CDataManager uniqueInstance = null;

	private Connection conn;
//	private Statement stmt;
	private ResultSet rset;
	private ResultSetMetaData rsmd;
	private Properties properties;
	private CManagingDatabase manager;
	public static String schemaSeparator;

	private CDataManager() {
		uniqueInstance = this;
		
// Load properties from file		
   		properties = CPropertyManager.getInstance().getProperties();
// vorerst ohne Factory Methode
   		if (properties.get("database").toString().equalsIgnoreCase("SQLSERVER")) {
   	   		manager = new CManagingSQLServer();   			
   		} else if (properties.get("database").toString().equalsIgnoreCase("ORACLE")){
   	   		manager = new CManagingOracle();   			   			
   		} else if (properties.get("database").toString().equalsIgnoreCase("MYSQL")){
   	   		manager = new CManagingMySQL();   			   			
   		} else {
   			System.out.println("Keine unterstützte Datenbank: " + properties.get("database").toString() + "\n");
   			System.out.println("Unterstützt werden:");
   			System.out.println("ORACLE");
   			System.out.println("SQLSERVER");
   			System.out.println("MYSQL");
   			System.exit(0);
   		}
		
// Connect to Database
   		manager.connect(properties);
   		conn = manager.getConnection();
//   		stmt = manager.getStatement();
	}

	public Statement getStatement() throws SQLException {
			return conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);		
	}
	
	public Connection getConnection() {
	    return conn;
	}

	public void dispose() {
		try {
			uniqueInstance = null;
//			stmt.close();
			conn.close();
		} catch (SQLException se) {
			System.out.println(se);
		}
	}

	public static CDataManager getInstance() {
		if (uniqueInstance == null) {
			new CDataManager();
		}
		return uniqueInstance;
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
	
	public ResultSet getTables(String object_name) {
	    return manager.getTables(object_name);
	}

	public ResultSet prepareCInfoDataObjects() {
		return manager.prepareCInfoDataObjects();
	}
	
	public ResultSet prepareCListDataObjects() {
		return manager.prepareCListDataObjects();
	}
	
	protected ResultSet getCInfoDataCustButtons(String object_name) {
	    return manager.getCInfoDataCustButtons(object_name);
	}

	protected ResultSet getCListDataCustButtons(String object_name) {
	    return manager.getCListDataCustButtons(object_name);
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
