package compucrash;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class CInfoDataManagingDatabase {

    private static final Logger LOGGER = Logger.getLogger(CInfoDataManagingDatabase.class.getName());

    private static final String TABLES_KEY = "tables";
    private static final String ATTRIBUTES_KEY = "attributes";
    private static final String COLUMN_NAME_KEY = "column_name";
    private static final String OWNER_KEY = "owner";
    private static final String TABLE_NAME_KEY = "table_name";
    private static final String IS_KEY_KEY = "iskey";
    private static final String DATA_TYPE_KEY = "data_type";
    private static final String FORMULA_KEY = "formula";
    private static final String EDITABLE_KEY = "editable";
    private static final String SQL_TYPE_KEY = "sqltype";
    private static final String TABLE_DATA_TYPE = "TABLE";
    private static final String LIST_DATA_TYPE = "LIST";
    private static final String HISTORY_DATA_TYPE = "HISTORY";
    private static final String COMMIT = "committed";

    protected CInfoDataObject parent;

    public CInfoDataManagingDatabase(CInfoDataObject parent) {
        this.parent = parent;
    }

    private static boolean isRelevantValue(Object value) {
        return value == null || !value.getClass().equals(CNull.class);
    }

    private static boolean isSelectableType(CProperties attribute) {
        return attribute.get(DATA_TYPE_KEY).toString().equalsIgnoreCase(TABLE_DATA_TYPE)
                || attribute.get(DATA_TYPE_KEY).toString().equalsIgnoreCase(LIST_DATA_TYPE)
                || attribute.get(DATA_TYPE_KEY).toString().equalsIgnoreCase(HISTORY_DATA_TYPE);
    }

    private static String joinQualifiedName(Object owner, Object tableName, Object columnName) {
        return owner + CDataManager.getSchemaSeparator() + tableName + "." + columnName;
    }

    private static StringBuilder buildSelectClause(CProperties pAttributes) {
        StringBuilder sqlSelect = new StringBuilder();
        sqlSelect.append("SELECT ");
        for (int i = 1; i <= pAttributes.size(); i++) {
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
            if (pA.get(FORMULA_KEY) != null) {
                sqlSelect.append(pA.get(FORMULA_KEY)).append(' ').append(pA.get(COLUMN_NAME_KEY)).append(',');
            } else if (isSelectableType(pA)) {
                sqlSelect.append(" NULL ").append(pA.get(COLUMN_NAME_KEY)).append(',');
            } else {
                sqlSelect.append(pA.get(OWNER_KEY)).append(CDataManager.getSchemaSeparator())
                        .append(pA.get(TABLE_NAME_KEY)).append('.').append(pA.get(COLUMN_NAME_KEY)).append(',');
            }
        }
        if (!sqlSelect.isEmpty()) sqlSelect.setLength(sqlSelect.length() - 1);
        return sqlSelect;
    }

    private static StringBuilder buildWhereClause(CProperties pAttributes, CProperties pTables) {
        StringBuilder sqlWhere = new StringBuilder();
        sqlWhere.append(" WHERE ");
        for (int i = 1; i <= pAttributes.size(); i++) {
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
            if (!pA.get(IS_KEY_KEY).toString().equalsIgnoreCase("0")) {
                for (int j = 1; j < pTables.size(); j++) {
                    sqlWhere.append(pTables.get("0")).append('.').append(pA.get(COLUMN_NAME_KEY))
                            .append(" = ").append(pTables.get(Integer.toString(j))).append('.')
                            .append(pA.get(COLUMN_NAME_KEY)).append(" AND ");
                }
            }
        }
        if (!sqlWhere.isEmpty()) {
            sqlWhere.setLength(sqlWhere.length() - 5);
        } else {
            sqlWhere.append("1 = 1");
        }
        return sqlWhere;
    }

    private static void setParameterValue(PreparedStatement pstmt, int k, Object value, int sqlType) throws SQLException {
        if (value == null) {
            pstmt.setNull(k, sqlType);
        } else if (value instanceof LocalDate date) {
            pstmt.setObject(k, Date.valueOf(date), sqlType);
        } else if (value instanceof LocalDateTime timestamp) {
            pstmt.setObject(k, Timestamp.valueOf(timestamp), sqlType);
        } else {
            pstmt.setObject(k, value, sqlType);
        }
    }

    private static boolean isAttributeForTable(CProperties pAttr, String tableQualified) {
        return (pAttr.get(OWNER_KEY) + CDataManager.getSchemaSeparator() + pAttr.get(TABLE_NAME_KEY))
                .equalsIgnoreCase(tableQualified)
                || !pAttr.get(IS_KEY_KEY).toString().equalsIgnoreCase("0");
    }

    private static boolean isAttributeInTable(CProperties pAttr, String tableQualified) {
        return (pAttr.get(OWNER_KEY) + CDataManager.getSchemaSeparator() + pAttr.get(TABLE_NAME_KEY))
                .equalsIgnoreCase(tableQualified);
    }

    private static boolean isEditableNonSelectable(CProperties pAttr) {
        return !pAttr.get(EDITABLE_KEY).equals("0") && !isSelectableType(pAttr);
    }

    private static List<Object> collectKeyValues(CProperties pAttributes, CProperties keys) {
        List<Object> parameterValues = new ArrayList<>();
        for (int m = 1; m <= pAttributes.size(); m++) {
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(m));
            if (!pA.get(IS_KEY_KEY).toString().equalsIgnoreCase("0")) {
                for (int k = 1; k <= keys.size(); k++) {
                    CProperties key = (CProperties) keys.get(Integer.toString(k));
                    if (key.get(COLUMN_NAME_KEY).toString().equalsIgnoreCase(pA.get(COLUMN_NAME_KEY).toString())) {
                        parameterValues.add(key.get("value"));
                    }
                }
            }
        }
        return parameterValues;
    }

    private static String buildInsertSql(String tableQualified, CProperties pAttributes, CDataObject actual) {
        StringBuilder cols = new StringBuilder("INSERT INTO ").append(tableQualified).append('(');
        StringBuilder vals = new StringBuilder();
        for (int j = 1; j <= pAttributes.size(); j++) {
            CProperties pAttr = (CProperties) pAttributes.get(Integer.toString(j));
            if (isRelevantValue(actual.get(j)) && isAttributeForTable(pAttr, tableQualified) && isEditableNonSelectable(pAttr)) {
                cols.append(pAttr.get(COLUMN_NAME_KEY)).append(',');
                vals.append("?,");
            }
        }
        if (vals.isEmpty()) return null;
        cols.setLength(cols.length() - 1);
        vals.setLength(vals.length() - 1);
        return cols.append(") VALUES (").append(vals).append(')').toString();
    }

    private static void bindInsertParameters(PreparedStatement pstmt, String tableQualified,
                                             CProperties pAttributes, CDataObject actual) throws SQLException {
        int k = 0;
        for (int j = 1; j <= pAttributes.size(); j++) {
            CProperties pAttr = (CProperties) pAttributes.get(Integer.toString(j));
            if (isRelevantValue(actual.get(j)) && isAttributeForTable(pAttr, tableQualified) && isEditableNonSelectable(pAttr)) {
                k++;
                CMessage.print(Integer.valueOf(k));
                setParameterValue(pstmt, k, actual.get(j), ((Integer) pAttr.get(SQL_TYPE_KEY)).intValue());
            }
        }
    }

    private static StringBuilder buildDeleteSql(String tableQualified, CProperties pAttributes) {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(tableQualified).append(" WHERE ");
        for (int i = 1; i <= pAttributes.size(); i++) {
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
            if (!pA.get(IS_KEY_KEY).toString().equalsIgnoreCase("0")) {
                sql.append(pA.get(COLUMN_NAME_KEY)).append(" = ? AND ");
            }
        }
        sql.setLength(sql.length() - 5);
        return sql;
    }

    private static String buildUpdateSql(String tableQualified, CProperties pAttributes,
                                         CDataObject actual, CProperties keys) {
        StringBuilder setClauses = new StringBuilder();
        for (int j = 1; j <= pAttributes.size(); j++) {
            CProperties pAttr = (CProperties) pAttributes.get(Integer.toString(j));
            if (isRelevantValue(actual.get(j)) && isAttributeInTable(pAttr, tableQualified) && isEditableNonSelectable(pAttr)) {
                setClauses.append(pAttr.get(COLUMN_NAME_KEY)).append(" = ?,");
            }
        }
        if (setClauses.isEmpty()) return null;
        setClauses.setLength(setClauses.length() - 1);
        StringBuilder whereClauses = new StringBuilder();
        for (int m = 1; m <= pAttributes.size(); m++) {
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(m));
            if (!pA.get(IS_KEY_KEY).toString().equalsIgnoreCase("0")) {
                whereClauses.append(pA.get(COLUMN_NAME_KEY)).append(" = ? AND ");
            }
        }
        if (whereClauses.length() >= 5) whereClauses.setLength(whereClauses.length() - 5);
        return "UPDATE " + tableQualified + " SET " + setClauses + " WHERE " + whereClauses;
    }

    private static int bindUpdateParameters(PreparedStatement pstmt, String tableQualified,
                                            CProperties pAttributes, CDataObject actual) throws SQLException {
        int k = 0;
        for (int j = 1; j <= pAttributes.size(); j++) {
            CProperties pAttr = (CProperties) pAttributes.get(Integer.toString(j));
            if (isRelevantValue(actual.get(j))) {
                if (isAttributeInTable(pAttr, tableQualified) && isEditableNonSelectable(pAttr)) {
                    k++;
                    setParameterValue(pstmt, k, actual.get(j), ((Integer) pAttr.get(SQL_TYPE_KEY)).intValue());
                }
                actual.remove(Integer.toString(j));
            }
        }
        return k;
    }

    protected void forUpdate() {
    }

    protected void prepareSQLString(CProperties p) {
    }

    private CProperties getProperties() {
        return parent.getCProperties();
    }

    private String buildBaseSql(CProperties p) {
        CProperties pTables = (CProperties) p.get(TABLES_KEY);
        StringBuilder sqlFrom = new StringBuilder();
        sqlFrom.append(" FROM ");
        for (int i = 0; i < pTables.size(); i++) {
            sqlFrom.append(pTables.get(Integer.toString(i))).append(',');
        }
        if (!sqlFrom.isEmpty()) sqlFrom.setLength(sqlFrom.length() - 1);

        CProperties pAttributes = (CProperties) p.get(ATTRIBUTES_KEY);
        StringBuilder sqlSelect = buildSelectClause(pAttributes);
        StringBuilder sqlWhere = buildWhereClause(pAttributes, pTables);
        String fullString = String.valueOf(sqlSelect) +
                sqlFrom +
                sqlWhere;
        return fullString;
    }

    CDataObject getCDataObject(CProperties keys, boolean b) {
        if (keys == null) {
            return new CDataObject(null);
        }
        CProperties p = getProperties();
        String sqlObjectString = buildBaseSql(p);
        List<Object> parameterValues = new ArrayList<Object>();
        for (int i = 1; i <= keys.size(); i++) {
            CProperties pKey = (CProperties) keys.get(Integer.toString(i));
            sqlObjectString += " AND " + joinQualifiedName(pKey.get(OWNER_KEY), pKey.get(TABLE_NAME_KEY),
                    pKey.get(COLUMN_NAME_KEY)) + " = ? ";
            parameterValues.add(pKey.get("value"));
        }

        Object[] rowData = null;
        try {
            if (b) {
                forUpdate();
                begin();
            }
            try (PreparedStatement pstmt = CDataManager.getInstance().getConnection().prepareStatement(sqlObjectString)) {
                for (int i = 0; i < parameterValues.size(); i++) {
                    pstmt.setObject(i + 1, parameterValues.get(i));
                }
                try (ResultSet rset = pstmt.executeQuery()) {
                    CMessage.print("CInfoDataManagingDatabase.getCDataObject():");
                    CMessage.print(sqlObjectString);
                    ResultSetMetaData rsmd = rset.getMetaData();
                    rowData = new Object[rsmd.getColumnCount()];
                    rset.next();
                    for (int i = 0; i < rsmd.getColumnCount(); i++) {
                        Object value = rset.getObject(i + 1);
                        rowData[i] = rset.wasNull() ? new CNull() : value;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load data object: " + sqlObjectString, e);
        } catch (NumberFormatException nfe) {
            CMessage.print(nfe);
        }
        return new CDataObject(rowData);
    }

    public void begin() {
    }

    void insert(CDataObject actual) throws SQLException {
        begin();
        CProperties p = getProperties();
        CProperties pTables = (CProperties) p.get(TABLES_KEY);
        CProperties pAttributes = (CProperties) p.get(ATTRIBUTES_KEY);
        for (int i = 0; i < pTables.size(); i++) {
            String tableQualified = (String) pTables.get(Integer.toString(i));
            String sqlInsert = buildInsertSql(tableQualified, pAttributes, actual);
            if (sqlInsert == null) continue;
            CMessage.print(sqlInsert);
            try (PreparedStatement pstmt = CDataManager.getInstance().getConnection().prepareStatement(sqlInsert)) {
                bindInsertParameters(pstmt, tableQualified, pAttributes, actual);
                pstmt.executeUpdate();
            }
        }
        CDataManager.getInstance().getConnection().commit();
        CMessage.print(COMMIT);
    }

    void delete(CProperties keys) throws SQLException {
        CProperties p = getProperties();
        CProperties pTables = (CProperties) p.get(TABLES_KEY);
        CProperties pAttributes = (CProperties) p.get(ATTRIBUTES_KEY);
        for (int j = pTables.size() - 1; j >= 0; j--) {
            String tableQualified = pTables.get(Integer.toString(j)).toString();
            List<Object> parameterValues = collectKeyValues(pAttributes, keys);
            if (parameterValues.isEmpty()) continue;
            StringBuilder sqlDelete = buildDeleteSql(tableQualified, pAttributes);
            try (PreparedStatement pstmt = CDataManager.getInstance().getConnection().prepareStatement(sqlDelete.toString())) {
                for (int i = 0; i < parameterValues.size(); i++) {
                    pstmt.setObject(i + 1, parameterValues.get(i));
                }
                pstmt.executeUpdate();
            }
        }
        CDataManager.getInstance().getConnection().commit();
        CMessage.print(COMMIT);
    }

    void update(CProperties keys, CDataObject actual) throws SQLException {
        CProperties p = getProperties();
        CProperties pTables = (CProperties) p.get(TABLES_KEY);
        CProperties pAttributes = (CProperties) p.get(ATTRIBUTES_KEY);

        for (int i = pTables.size() - 1; i >= 0; i--) {
            String tableQualified = (String) pTables.get(Integer.toString(i));
            String sqlUpdate = buildUpdateSql(tableQualified, pAttributes, actual, keys);
            if (sqlUpdate == null) continue;
            CMessage.print(sqlUpdate);
            List<Object> keyValues = collectKeyValues(pAttributes, keys);
            try (PreparedStatement pstmt = CDataManager.getInstance().getConnection().prepareStatement(sqlUpdate)) {
                int k = bindUpdateParameters(pstmt, tableQualified, pAttributes, actual);
                for (int iParam = 0; iParam < keyValues.size(); iParam++) {
                    pstmt.setObject(k + iParam + 1, keyValues.get(iParam));
                }
                pstmt.executeUpdate();
            }
        }
        CDataManager.getInstance().getConnection().commit();
        CMessage.print(COMMIT);
    }
}
