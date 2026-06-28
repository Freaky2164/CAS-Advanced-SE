package compucrash;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class CListDataManagingDatabase {

    private static final Logger LOGGER = Logger.getLogger(CListDataManagingDatabase.class.getName());
    private static final String COLUMNNAME = "column_name";
    private static final String ORDERBY = " ORDER BY ";
    private static final String FORMULA = "formula";
    private static final String TABLENAME = "table_name";
    private static final String OWNER = "owner";
    private static final String ORDER = "order";
    private final CProperties p;
    protected CListDataObject parent;

    protected CListDataManagingDatabase(CListDataObject parent) {
        this.parent = parent;
        this.p = parent.getCProperties();
    }

    private static String sqlIdentifier(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing SQL identifier");
        }
        return CManagingDatabase.validateSqlExpression(value.toString());
    }

    private static String sqlOrderExpression(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing SQL order expression");
        }
        return CManagingDatabase.validateSqlExpression(value.toString());
    }

    private static String sqlOperator(Object value) {
        String operator = value == null ? "" : value.toString().trim().toUpperCase();
        switch (operator) {
            case "=", "<>", "<", "<=", ">", "LIKE", "NOT LIKE":
                return operator;
            default:
                throw new IllegalArgumentException("Unsupported SQL operator: " + operator);
        }
    }

    private static int validateOrderColumn(int orderColumn) {
        if (orderColumn < 1) {
            throw new IllegalArgumentException("Invalid order column: " + orderColumn);
        }
        return orderColumn;
    }

    private String buildBaseSql() {
        StringBuilder sqlFrom = new StringBuilder();
        StringBuilder sqlSelect = new StringBuilder();
        StringBuilder sqlWhere = new StringBuilder();
        CProperties pTables = (CProperties) p.get("tables");
        for (int i = 0; i < pTables.size(); i++) {
            sqlFrom.append(sqlIdentifier(pTables.get(Integer.toString(i)))).append(',');
        }
        sqlFrom.setLength(sqlFrom.length() - 1);
        CProperties pAttributes = (CProperties) p.get("attributes");
        for (int i = 1; i <= pAttributes.size(); i++) {
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
            if (pA.get(FORMULA) != null) {
                sqlSelect.append(CManagingDatabase.validateSqlExpression(pA.get(FORMULA).toString())).append(' ')
                        .append(sqlIdentifier(pA.get(COLUMNNAME))).append(',');
            } else {
                sqlSelect.append(sqlIdentifier(pA.get(OWNER))).append(CDataManager.getSchemaSeparator())
                        .append(sqlIdentifier(pA.get(TABLENAME))).append('.')
                        .append(sqlIdentifier(pA.get(COLUMNNAME))).append(',');
            }
        }
        sqlSelect.setLength(sqlSelect.length() - 1);
        for (int i = 1; i <= pAttributes.size(); i++) {
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
            if (!pA.get("iskey").toString().equalsIgnoreCase("0")) {
                for (int j = 1; j < pTables.size(); j++) {
                    sqlWhere.append(sqlIdentifier(pTables.get("0"))).append('.').append(sqlIdentifier(pA.get(COLUMNNAME)))
                            .append(" = ").append(sqlIdentifier(pTables.get(Integer.toString(j)))).append('.')
                            .append(sqlIdentifier(pA.get(COLUMNNAME))).append(" AND ");
                }
            }
        }
        if (sqlWhere.length() >= 5) {
            sqlWhere.setLength(sqlWhere.length() - 5);
        } else {
            sqlWhere.append("1 = 1");
        }
        if (p.get("limitation") != null) {
            sqlWhere.append(" AND ").append(CManagingDatabase.validateSqlExpression(p.get("limitation").toString()));
        }
        return "SELECT " + sqlSelect + " FROM " + sqlFrom + " WHERE " + sqlWhere;
    }

    protected ResultSet getSelect(int orderColumn) {
        ResultSet rset = null;
        try {
            String sqlString = buildBaseSql();
            CMessage.print("CListDataManagingDatabase.getSelect:\n" + sqlString);
            rset = CDataManager.getInstance().getStatement().executeQuery(sqlString + ORDERBY + validateOrderColumn(orderColumn));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to execute select", e);
        }
        return rset;
    }

    public ResultSet getSelect(String text) {
        ResultSet rset = null;
        CProperties pAttributes = (CProperties) p.get("attributes");
        StringBuilder sqlSearchString = new StringBuilder(" AND (");
        List<String> searchColumns = new ArrayList<>();
        for (int i = 1; i <= pAttributes.size(); i++) {
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
            if (pA.get(FORMULA) != null) {
                /*Hier soll nichts passieren*/
            } else {
                searchColumns.add(sqlIdentifier(pA.get(OWNER)) + CDataManager.getSchemaSeparator() +
                        sqlIdentifier(pA.get(TABLENAME)) + "." +
                        sqlIdentifier(pA.get(COLUMNNAME)));
            }
        }
        for (int i = 0; i < searchColumns.size(); i++) {
            sqlSearchString.append(searchColumns.get(i)).append(" LIKE ?");
            if (i < searchColumns.size() - 1) {
                sqlSearchString.append(" OR ");
            }
        }
        sqlSearchString.append(")");
        String sqlOrderString;
        if (p.get(ORDER) != null) {
            CProperties po = (CProperties) p.get(ORDER);
            StringBuilder sqlOrderSb = new StringBuilder(ORDERBY);
            for (int i = 1; i <= po.size(); i++) {
                sqlOrderSb.append(sqlOrderExpression(po.get(Integer.toString(i)))).append(", ");
            }
            sqlOrderSb.setLength(sqlOrderSb.length() - 2);
            sqlOrderString = sqlOrderSb.toString();
        } else {
            sqlOrderString = "";
        }

        try {
            String sqlString = buildBaseSql();
            CMessage.print("CListDataManagingDatabase.getSelect(text):\n" + sqlString + sqlSearchString);
            Connection conn = CDataManager.getInstance().getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sqlString + sqlSearchString + sqlOrderString)) {
                for (int i = 0; i < searchColumns.size(); i++) {
                    pstmt.setString(i + 1, "%" + text + "%");
                }
                rset = pstmt.executeQuery();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to execute search select", e);
        }
        return rset;
    }

    public ResultSet select(CProperties pTab) {
        //TODO getSelect durch die universelle select Methode ersetzen
        ResultSet rset = null;
        String sqlString = buildBaseSql();
        String sqlOrderString;
        if (pTab.get(ORDER) != null) {
            CProperties po = (CProperties) pTab.get(ORDER);
            StringBuilder sqlOrderSb2 = new StringBuilder(ORDERBY);
            for (int i = 1; i <= po.size(); i++) {
                sqlOrderSb2.append(sqlOrderExpression(po.get(Integer.toString(i)))).append(", ");
            }
            sqlOrderSb2.setLength(sqlOrderSb2.length() - 2);
            sqlOrderString = sqlOrderSb2.toString();
        } else {
            sqlOrderString = "";
        }
        StringBuilder sqlFilterString = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        if (pTab.get("filter_and") != null) {
            CProperties pf = (CProperties) pTab.get("filter_and");
            for (int i = 1; i <= pf.size(); i++) {
                CProperties pfa = (CProperties) pf.get(Integer.toString(i));
                sqlFilterString.append("AND ").append(sqlIdentifier(pfa.get(OWNER))).append(CDataManager.getSchemaSeparator()).append(sqlIdentifier(pfa.get(OWNER))).append(CDataManager.getSchemaSeparator()).append(sqlIdentifier(pfa.get(TABLENAME))).append(".").append(sqlIdentifier(pfa.get(COLUMNNAME))).append(" ").append(sqlOperator(pfa.get("operator"))).append(" ? ");
                parameters.add(pfa.get("value"));
            }
        } else {
            sqlFilterString = new StringBuilder();
        }
        if (pTab.get("filter_or") != null) {
            CProperties pf = (CProperties) pTab.get("filter_or");
            sqlFilterString.append(" AND ( 1 = 0 ");
            for (int i = 1; i <= pf.size(); i++) {
                CProperties pfa = (CProperties) pf.get(Integer.toString(i));
                sqlFilterString.append("OR ").append(sqlIdentifier(pfa.get(OWNER))).append(CDataManager.getSchemaSeparator()).append(sqlIdentifier(pfa.get(TABLENAME))).append(".").append(sqlIdentifier(pfa.get(COLUMNNAME))).append(" ").append(sqlOperator(pfa.get("operator"))).append(" ? ");
                parameters.add(pfa.get("value"));
            }
            sqlFilterString.append(")");
        }

        try {
            CMessage.print("CListDataManagingDatabase.getSelect:\n" + sqlString + sqlFilterString.toString() + sqlOrderString);
            try (PreparedStatement pstmt = CDataManager.getInstance().getConnection().prepareStatement(sqlString + sqlFilterString + sqlOrderString)) {
                for (int i = 0; i < parameters.size(); i++) {
                    pstmt.setObject(i + 1, parameters.get(i));
                }
                rset = pstmt.executeQuery();
            }
        } catch (SQLException _) {
            /*Hier soll nichts passieren*/
        }
        return rset;
    }
}
