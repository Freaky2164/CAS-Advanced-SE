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
    private final CProperties p;
    protected CListDataObject parent;

    public CListDataManagingDatabase(CListDataObject parent) {
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
            case "=":
            case "<>":
            case "<":
            case "<=":
            case ">":
            case ">=":
            case "LIKE":
            case "NOT LIKE":
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
            if (pA.get("formula") != null) {
                sqlSelect.append(CManagingDatabase.validateSqlExpression(pA.get("formula").toString())).append(' ')
                        .append(sqlIdentifier(pA.get("column_name"))).append(',');
            } else {
                sqlSelect.append(sqlIdentifier(pA.get("owner"))).append(CDataManager.getSchemaSeparator())
                        .append(sqlIdentifier(pA.get("table_name"))).append('.')
                        .append(sqlIdentifier(pA.get("column_name"))).append(',');
            }
        }
        sqlSelect.setLength(sqlSelect.length() - 1);
        for (int i = 1; i <= pAttributes.size(); i++) {
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
            if (!pA.get("iskey").toString().equalsIgnoreCase("0")) {
                for (int j = 1; j < pTables.size(); j++) {
                    sqlWhere.append(sqlIdentifier(pTables.get("0"))).append('.').append(sqlIdentifier(pA.get("column_name")))
                            .append(" = ").append(sqlIdentifier(pTables.get(Integer.toString(j)))).append('.')
                            .append(sqlIdentifier(pA.get("column_name"))).append(" AND ");
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
            rset = CDataManager.getInstance().getStatement().executeQuery(sqlString + " ORDER BY " + validateOrderColumn(orderColumn));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to execute select", e);
        }
        return rset;
    }

    public ResultSet getSelect(String text) {
        ResultSet rset = null;
        CProperties pTables = (CProperties) p.get("tables");
        CProperties pAttributes = (CProperties) p.get("attributes");
        StringBuilder sqlSearchString = new StringBuilder(" AND (");
        List<String> searchColumns = new ArrayList<String>();
        for (int i = 1; i <= pAttributes.size(); i++) {
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
            if (pA.get("formula") != null) {
// nicht in formeln suchen
//			    SQLSearchString += (String) pA.get("column_name") + " LIKE '%" +
//				text + "%' OR ";
            } else {
                searchColumns.add(sqlIdentifier(pA.get("owner")) + CDataManager.getSchemaSeparator() +
                        sqlIdentifier(pA.get("table_name")) + "." +
                        sqlIdentifier(pA.get("column_name")));
            }
        }
        for (int i = 0; i < searchColumns.size(); i++) {
            sqlSearchString.append(searchColumns.get(i)).append(" LIKE ?");
            if (i < searchColumns.size() - 1) {
                sqlSearchString.append(" OR ");
            }
        }
        sqlSearchString.append(")");
        String SQLOrderString;
        if (p.get("order") != null) {
            CProperties po = (CProperties) p.get("order");
            StringBuilder sqlOrderSb = new StringBuilder(" ORDER BY ");
            for (int i = 1; i <= po.size(); i++) {
                sqlOrderSb.append(sqlOrderExpression(po.get(Integer.toString(i)))).append(", ");
            }
            sqlOrderSb.setLength(sqlOrderSb.length() - 2);
            SQLOrderString = sqlOrderSb.toString();
        } else {
            SQLOrderString = "";
        }

        try {
            String sqlString = buildBaseSql();
            CMessage.print("CListDataManagingDatabase.getSelect(text):\n" + sqlString + sqlSearchString);
            Connection conn = CDataManager.getInstance().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sqlString + sqlSearchString + SQLOrderString);
            for (int i = 0; i < searchColumns.size(); i++) {
                pstmt.setString(i + 1, "%" + text + "%");
            }
            rset = pstmt.executeQuery();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to execute search select", e);
        }
        return rset;
    }

    public ResultSet select(CProperties pTab) {
        //TODO getSelect durch die universelle select Methode ersetzen
        ResultSet rset = null;
        String sqlString = buildBaseSql();
        String SQLOrderString;
        if (pTab.get("order") != null) {
            CProperties po = (CProperties) pTab.get("order");
            StringBuilder sqlOrderSb2 = new StringBuilder(" ORDER BY ");
            for (int i = 1; i <= po.size(); i++) {
                sqlOrderSb2.append(sqlOrderExpression(po.get(Integer.toString(i)))).append(", ");
            }
            sqlOrderSb2.setLength(sqlOrderSb2.length() - 2);
            SQLOrderString = sqlOrderSb2.toString();
        } else {
            SQLOrderString = "";
        }
        String SQLFilterString;
        List<Object> parameters = new ArrayList<Object>();
        if (pTab.get("filter_and") != null) {
            CProperties pf = (CProperties) pTab.get("filter_and");
            SQLFilterString = " ";
            for (int i = 1; i <= pf.size(); i++) {
                CProperties pfa = (CProperties) pf.get(Integer.toString(i));
                SQLFilterString += "AND "
                        + sqlIdentifier(pfa.get("owner")) + CDataManager.getSchemaSeparator()
                        + sqlIdentifier(pfa.get("table_name")) + "."
                        + sqlIdentifier(pfa.get("column_name")) + " "
                        + sqlOperator(pfa.get("operator")) + " ? ";
                parameters.add(pfa.get("value"));
            }
        } else {
            SQLFilterString = "";
        }
        if (pTab.get("filter_or") != null) {
            CProperties pf = (CProperties) pTab.get("filter_or");
            SQLFilterString += " AND ( 1 = 0 ";
            for (int i = 1; i <= pf.size(); i++) {
                CProperties pfa = (CProperties) pf.get(Integer.toString(i));
                SQLFilterString += "OR "
                        + sqlIdentifier(pfa.get("owner")) + CDataManager.getSchemaSeparator()
                        + sqlIdentifier(pfa.get("table_name")) + "."
                        + sqlIdentifier(pfa.get("column_name")) + " "
                        + sqlOperator(pfa.get("operator")) + " ? ";
                parameters.add(pfa.get("value"));
            }
            SQLFilterString += ")";
        }

        try {
            CMessage.print("CListDataManagingDatabase.getSelect:\n" + sqlString + SQLFilterString + SQLOrderString);
            PreparedStatement pstmt = CDataManager.getInstance().getConnection().prepareStatement(sqlString + SQLFilterString + SQLOrderString);
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }
            rset = pstmt.executeQuery();
        } catch (SQLException e) {
//			e.printStackTrace();
        }
        return rset;
    }
}
