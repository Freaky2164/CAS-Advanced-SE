package compucrash;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public abstract class CManagingDatabase {

	protected Connection conn;
	protected Statement stmt;
	protected String driverManager = "";
	protected String connection = "";
	protected String user = "";
	protected String pwd = "";

	abstract void connect(Properties p);
	
	protected String getMainFrame() {
		String str = null;
		String SQLString = "SELECT u.main_frame FROM compucrash.user_def u "
			+ "WHERE LOWER(u.user_name) = LOWER('" + CPropertyManager.USER + "')"; 
		try {
			ResultSet rset = getResultSet(SQLString);
			rset.first();
			str = rset.getString(1);
		} catch (SQLException e) {}
		return str;
	}

	protected ResultSet getObjects() {
		String SQLString = "SELECT o.object_name, o.object_label, uor.panel FROM compucrash.object_def o, compucrash.user_object_relation uor "
			+ "WHERE LOWER(uor.user_name) = LOWER('" + CPropertyManager.USER + "') AND uor.object_name = o.object_name "
			+ "AND uor.main_pos > 0 "; 
		return getResultSet(SQLString);
	}

	protected ResultSet getButtons() {
		String SQLString = "SELECT buttonid, bez, label, image, position, tooltip, command, mnemonic FROM compucrash.button_def ORDER BY buttonid";
		return getResultSet(SQLString);
	}
	
	protected ResultSet getTables(String object_name) {
		String SQLString = "SELECT owner, table_name, isleading FROM compucrash.object_tab_def ORDER BY owner, table_name, isleading DESC";
		return getResultSet(SQLString);
	    
	}

	protected ResultSet prepareCInfoDataObjects() {
		String SQLString = "SELECT o.object_name, o.object_label, "
			+ "ot.owner, ot.table_name, ot.isleading, "
			+ "otc.column_name, otc.pos_info, otc.label, otc.iskey, "
			+ "otc.tooltip, otc.data_type, otc.data_length, otc.data_precision, otc.data_scale, "
			+ "otc.view_panel, otc.panel, otc.label_length, otc.editable, otc.formula, otc.data_height, otc.gridwidth, "
			+ "otc.source, otc.init, o.color, otc.springen, otc.action, uor.bapply "
			+ "FROM compucrash.object_def o, compucrash.object_tab_def ot, compucrash.object_tab_col_def otc, compucrash.user_object_relation uor "
			+ "WHERE ot.object_name = o.object_name AND otc.object_name = ot.object_name AND otc.owner = ot.owner "
			+ "AND otc.table_name = ot.table_name AND otc.pos_info > 0 " 
			+ "AND LOWER(uor.user_name) = LOWER('" + CPropertyManager.USER + "') AND uor.object_name = o.object_name " 
			+ "ORDER BY o.object_name, otc.pos_info";
		return getResultSet(SQLString);
	}
	
	protected ResultSet getCInfoDataCustButtons(String object_name) {
	    String SQLString = "SELECT cbd.bez, cbr.panel, cbr.pos " +
				"FROM compucrash.button_def cbd, compucrash.cust_button_rel cbr " +
				"WHERE cbd.buttonid = cbr.buttonid " +
	    		"AND cbr.user_name = '" + CPropertyManager.USER + "' " +
	    		"AND cbr.dialog_type = 'info' " +
	    		"AND cbr.object_name = '" + object_name + "' " +
	    		"ORDER BY cbr.panel, cbr.pos, cbd.bez ";
	    return getResultSet(SQLString);
	}

	protected ResultSet getCListDataCustButtons(String object_name) {
	    String SQLString = "SELECT cbd.bez, cbr.pos " +
	    		"FROM compucrash.button_def cbd, compucrash.cust_button_rel cbr " +
	    		"WHERE cbd.buttonid = cbr.buttonid " +
	    		"AND cbr.user_name = '" + CPropertyManager.USER + "' " +
	    		"AND cbr.dialog_type = 'list' " +
	    		"AND cbr.object_name = '" + object_name + "' " +
	    		"ORDER BY cbr.panel, cbr.pos, cbd.bez ";
	    return getResultSet(SQLString);
	}

	protected ResultSet getGlobals() {
	    String SQLString = "SELECT name, value, type FROM compucrash.globals ORDER BY name";
	    return getResultSet(SQLString);
	}

	protected ResultSet prepareCListDataObjects() {
		String SQLString = "SELECT o.object_name, o.object_label, "
			+ "ot.owner, ot.table_name, ot.isleading, "
			+ "otc.column_name, otc.pos_list, otc.label, otc.iskey, otc.formula, "
			+ "uor.bnew, uor.bedit, uor.bdelete, uor.bcopy, uor.bdisplay, otc.list_data_scale, uor.default_button, "
			+ "otc.orderby, uor.limitation, o.color "
			+ "FROM compucrash.object_def o, compucrash.object_tab_def ot, compucrash.object_tab_col_def otc, compucrash.user_object_relation uor "
			+ "WHERE ot.object_name = o.object_name AND otc.object_name = ot.object_name AND otc.owner = ot.owner "
			+ "AND otc.table_name = ot.table_name AND otc.pos_list > 0 " 
			+ "AND LOWER(uor.user_name) = LOWER('" + CPropertyManager.USER + "') AND uor.object_name = o.object_name " 
			+ "ORDER BY o.object_name, otc.pos_list";
		return getResultSet(SQLString);
	}

	abstract CListDataManagingDatabase createCListDataManagingDatabase(CListDataObject parent);

	abstract CInfoDataManagingDatabase createCInfoDataManagingDatabase(CInfoDataObject parent);

/*	public Statement getStatement() {
		return stmt;
	}*/

	public Statement getStatement() throws SQLException {
		return conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);		
}

	public Connection getConnection() {
		return conn;
	}

	protected void connect() {
		try {
			Class.forName(driverManager);
			conn = DriverManager.getConnection(connection, CPropertyManager.USER, CPropertyManager.PWD);
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
		} catch (ClassNotFoundException e) {
			System.out.println(e);
			System.exit(0);
		} catch (SQLException se) {
			System.out.println(se);
			System.out.println(connection);
			System.exit(0);
		}
	}

	protected ResultSet getResultSet(String SQLString) {
		ResultSet rset = null;
		try {
			CMessage.print("CManagingDatabase.getResultSet:\n" + SQLString);
			rset = getStatement().executeQuery(SQLString);
		} catch (SQLException e) {
			System.out.println(e);
			e.printStackTrace();
			System.exit(0);
		}
		return rset;
	}

	public ResultSet getSelect(CProperties p) {
		ResultSet rset = null;
		CMessage.print(p);
		try {
			Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
			String SQLString = "SELECT DISTINCT " + p.get("column_name")
				+ " FROM " + p.get("owner") + "." + p.get("table_name")
				+ " ORDER BY 1";
			CMessage.print("CManagingDatabase:getSelect(p):");
			CMessage.print(SQLString);
			rset = stmt.executeQuery(SQLString);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rset;
	}

	public abstract Object getInit(String init);

	public ResultSet getMainPanels() {
		String SQLString = "SELECT panel, main_pos FROM compucrash.user_object_relation " +
		"WHERE main_pos > 0 AND LOWER(user_name) = LOWER('" + CPropertyManager.USER + "') UNION " +
		"SELECT panel, main_pos FROM compucrash.cust_button_main_rel " +
		"WHERE main_pos > 0 AND LOWER(user_name) = LOWER('" + CPropertyManager.USER + "') " +
		"ORDER BY main_pos ";			
		return getResultSet(SQLString);
	}

	public ResultSet getCustMainButtons() {
		String SQLString = "SELECT cbmr.panel, cbmr.main_pos, " +
		"cb.label, cb.position, cb.tooltip, cb.image, cb.command " +
		"FROM compucrash.cust_button_main_rel cbmr, compucrash.cust_button_def cb " +
		"WHERE cbmr.buttonid = cb.buttonid " +
		"AND LOWER(user_name) = LOWER('" + CPropertyManager.USER + "') " +
		"ORDER BY main_pos ";			
		return getResultSet(SQLString);
	}
}