package compucrash;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public abstract class CManagingDatabase {

    private static final Logger LOGGER = Logger.getLogger(CManagingDatabase.class.getName());
    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    protected Connection conn;
    protected String driverManager = "";
    protected String connection = "";
    protected String user = "";
    protected String pwd = "";

    private static CachedRowSet toCachedResultSet(ResultSet resultSet) throws SQLException {
        CachedRowSet cachedRowSet = RowSetProvider.newFactory().createCachedRowSet();
        try {
            cachedRowSet.populate(resultSet);
            return cachedRowSet;
        } catch (SQLException e) {
            try {
                cachedRowSet.close();
            } catch (SQLException closeException) {
                e.addSuppressed(closeException);
            }
            throw e;
        }
    }

    private static String requireIdentifier(Object value) {
        String identifier = value == null ? null : value.toString();
        if (identifier == null || !SQL_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + identifier);
        }
        return identifier;
    }

    protected static String validateSqlExpression(String expression) {
        if (expression == null
                || !expression.matches("[A-Za-z0-9_().,\\s'\"+\\-*/=<>%]+")) {
            throw new IllegalArgumentException("Invalid SQL expression");
        }
        return expression;
    }

    abstract void connect(Properties p);

    protected String getMainFrame() {
        String str = null;
        String sqlString = "SELECT u.main_frame FROM compucrash.user_def u "
                + "WHERE LOWER(u.user_name) = LOWER(?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlString, ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY)) {
            pstmt.setString(1, CPropertyManager.USER);
            try (ResultSet rset = pstmt.executeQuery()) {
                rset.first();
                str = rset.getString(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load main frame", e);
        }
        return str;
    }

    protected ResultSet getObjects() {
        String sqlString = "SELECT o.object_name, o.object_label, uor.panel FROM compucrash.object_def o, compucrash.user_object_relation uor "
                + "WHERE LOWER(uor.user_name) = LOWER(?) AND uor.object_name = o.object_name "
                + "AND uor.main_pos > 0 ";
        return getPreparedResultSet(sqlString, CPropertyManager.USER);
    }

    protected ResultSet getButtons() {
        String sqlString = "SELECT buttonid, bez, label, image, position, tooltip, command, mnemonic FROM compucrash.button_def ORDER BY buttonid";
        return getResultSet(sqlString);
    }

    protected ResultSet getTables(String object_name) {
        String sqlString = "SELECT owner, table_name, isleading FROM compucrash.object_tab_def ORDER BY owner, table_name, isleading DESC";
        return getResultSet(sqlString);
    }

    protected ResultSet prepareCInfoDataObjects() {
        String sqlString = "SELECT o.object_name, o.object_label, "
                + "ot.owner, ot.table_name, ot.isleading, "
                + "otc.column_name, otc.pos_info, otc.label, otc.iskey, "
                + "otc.tooltip, otc.data_type, otc.data_length, otc.data_precision, otc.data_scale, "
                + "otc.view_panel, otc.panel, otc.label_length, otc.editable, otc.formula, otc.data_height, otc.gridwidth, "
                + "otc.source, otc.init, o.color, otc.springen, otc.action, uor.bapply "
                + "FROM compucrash.object_def o, compucrash.object_tab_def ot, compucrash.object_tab_col_def otc, compucrash.user_object_relation uor "
                + "WHERE ot.object_name = o.object_name AND otc.object_name = ot.object_name AND otc.owner = ot.owner "
                + "AND otc.table_name = ot.table_name AND otc.pos_info > 0 "
                + "AND LOWER(uor.user_name) = LOWER(?) AND uor.object_name = o.object_name "
                + "ORDER BY o.object_name, otc.pos_info";
        return getPreparedResultSet(sqlString, CPropertyManager.USER);
    }

    protected ResultSet getCInfoDataCustButtons(String object_name) {
        String sqlString = "SELECT cbd.bez, cbr.panel, cbr.pos "
                + "FROM compucrash.button_def cbd, compucrash.cust_button_rel cbr "
                + "WHERE cbd.buttonid = cbr.buttonid "
                + "AND cbr.user_name = ? "
                + "AND cbr.dialog_type = 'info' "
                + "AND cbr.object_name = ? "
                + "ORDER BY cbr.panel, cbr.pos, cbd.bez ";
        return getPreparedResultSet(sqlString, CPropertyManager.USER, object_name);
    }

    protected ResultSet getCListDataCustButtons(String object_name) {
        String sqlString = "SELECT cbd.bez, cbr.pos "
                + "FROM compucrash.button_def cbd, compucrash.cust_button_rel cbr "
                + "WHERE cbd.buttonid = cbr.buttonid "
                + "AND cbr.user_name = ? "
                + "AND cbr.dialog_type = 'list' "
                + "AND cbr.object_name = ? "
                + "ORDER BY cbr.panel, cbr.pos, cbd.bez ";
        return getPreparedResultSet(sqlString, CPropertyManager.USER, object_name);
    }

    protected ResultSet getGlobals() {
        String sqlString = "SELECT name, value, type FROM compucrash.globals ORDER BY name";
        return getResultSet(sqlString);
    }

    protected ResultSet prepareCListDataObjects() {
        String sqlString = "SELECT o.object_name, o.object_label, "
                + "ot.owner, ot.table_name, ot.isleading, "
                + "otc.column_name, otc.pos_list, otc.label, otc.iskey, otc.formula, "
                + "uor.bnew, uor.bedit, uor.bdelete, uor.bcopy, uor.bdisplay, otc.list_data_scale, uor.default_button, "
                + "otc.orderby, uor.limitation, o.color "
                + "FROM compucrash.object_def o, compucrash.object_tab_def ot, compucrash.object_tab_col_def otc, compucrash.user_object_relation uor "
                + "WHERE ot.object_name = o.object_name AND otc.object_name = ot.object_name AND otc.owner = ot.owner "
                + "AND otc.table_name = ot.table_name AND otc.pos_list > 0 "
                + "AND LOWER(uor.user_name) = LOWER(?) AND uor.object_name = o.object_name "
                + "ORDER BY o.object_name, otc.pos_list";
        return getPreparedResultSet(sqlString, CPropertyManager.USER);
    }

    abstract CListDataManagingDatabase createCListDataManagingDatabase(CListDataObject parent);

    abstract CInfoDataManagingDatabase createCInfoDataManagingDatabase(CInfoDataObject parent);

    public Statement getStatement() throws SQLException {
        return conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
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
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Database driver not found", e);
            System.exit(0);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "Failed to connect to database: " + connection, se);
            System.exit(0);
        }
    }

    protected ResultSet getResultSet(String sqlString) {
        try (Statement stmt = getStatement();
             ResultSet rset = stmt.executeQuery(sqlString)) {
            CMessage.print("CManagingDatabase.getResultSet:\n" + sqlString);
            return toCachedResultSet(rset);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to execute query", e);
            System.exit(0);
        }
        return null;
    }

    protected ResultSet getPreparedResultSet(String sqlString, Object... parameters) {
        try (PreparedStatement pstmt = conn.prepareStatement(sqlString, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            for (int i = 0; i < parameters.length; i++) {
                pstmt.setObject(i + 1, parameters[i]);
            }
            try (ResultSet rset = pstmt.executeQuery()) {
                return toCachedResultSet(rset);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to execute prepared query", e);
            System.exit(0);
        }
        return null;
    }

    public ResultSet getSelect(CProperties p) {
        CMessage.print(p);
        try {
            String columnName = requireIdentifier(p.get("column_name"));
            String owner = requireIdentifier(p.get("owner"));
            String tableName = requireIdentifier(p.get("table_name"));
            String sqlString = "SELECT DISTINCT " + columnName
                    + " FROM " + owner + "." + tableName
                    + " ORDER BY 1";
            CMessage.print("CManagingDatabase:getSelect(p):");
            CMessage.print(sqlString);
            try (Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                 ResultSet rset = stmt.executeQuery(sqlString)) {
                return toCachedResultSet(rset);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to execute select query", e);
        }
        return null;
    }

    public abstract Object getInit(String init);

    public ResultSet getMainPanels() {
        String sqlString = "SELECT panel, main_pos FROM compucrash.user_object_relation "
                + "WHERE main_pos > 0 AND LOWER(user_name) = LOWER(?) UNION "
                + "SELECT panel, main_pos FROM compucrash.cust_button_main_rel "
                + "WHERE main_pos > 0 AND LOWER(user_name) = LOWER(?) "
                + "ORDER BY main_pos ";
        return getPreparedResultSet(sqlString, CPropertyManager.USER, CPropertyManager.USER);
    }

    public ResultSet getCustMainButtons() {
        String sqlString = "SELECT cbmr.panel, cbmr.main_pos, "
                + "cb.label, cb.position, cb.tooltip, cb.image, cb.command "
                + "FROM compucrash.cust_button_main_rel cbmr, compucrash.cust_button_def cb "
                + "WHERE cbmr.buttonid = cb.buttonid "
                + "AND LOWER(user_name) = LOWER(?) "
                + "ORDER BY main_pos ";
        return getPreparedResultSet(sqlString, CPropertyManager.USER);
    }
}
