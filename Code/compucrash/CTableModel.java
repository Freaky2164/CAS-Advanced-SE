package compucrash;

import javax.swing.table.AbstractTableModel;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CTableModel extends AbstractTableModel {

    private static final Logger LOGGER = Logger.getLogger(CTableModel.class.getName());
    private final ArrayList<Object> columns = new ArrayList<>();
    private ResultSet rset;
    private ResultSetMetaData rsmd;
    private compucrash.CProperties p = null;

    public CTableModel(ResultSet rset) {
        super();
        this.rset = rset;
        try {
            this.rsmd = this.rset.getMetaData();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load result set metadata", e);
        }
    }

    public CTableModel() {
    }

    public Statement getStatement() throws SQLException {
        return this.rset.getStatement();
    }

    public void addColumn(Object column) {
        this.columns.add(column);
    }

    public void setProperties(compucrash.CProperties p) {
        this.p = p;
    }

    public CProperties getKeys(int selectedRow) {
        compucrash.CProperties pAttributes = (CProperties) p.get("attributes");
        int j = 0;
        CProperties keys = new CProperties();
        for (int i = 1; i <= pAttributes.size(); i++) {
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
            if (!pA.get("iskey").toString().equalsIgnoreCase("0")) {
                CProperties pKey = new compucrash.CProperties();
                j++;
                keys.put(Integer.toString(j), pKey);
                pKey.put("owner", pA.get("owner"));
                pKey.put("table_name", pA.get("table_name"));
                pKey.put("column_name", pA.get("column_name"));
                try {
                    for (int k = 1; k <= rsmd.getColumnCount(); k++) {
/*					    CMessage.print("CTableModel.getKeys.rsmd.getColumnName(k):");
					    CMessage.print(Integer.valueOf(k));
					    CMessage.print(rsmd.getColumnName(k));
					    CMessage.print(rsmd.getTableName(k));
					    CMessage.print(rsmd.getSchemaName(k));
					    String tableName = rsmd.getTableName(k);
					    CMessage.print("CTableModel.getKeys.pA.get(column_name):");
					    CMessage.print(pA.get("column_name").toString());
					    CMessage.print(pA.get("table_name").toString());
					    CMessage.print(pA.get("owner").toString());*/
	/*					if (rsmd.getColumnName(k).equalsIgnoreCase(pA.get("column_name").toString()) 
								&& rsmd.getTableName(k).equalsIgnoreCase(pA.get("table_name").toString())
								&& rsmd.getSchemaName(k).equalsIgnoreCase(pA.get("owner").toString())) {
							pKey.put("value",getValueAt(selectedRow, k - 1));
							break;
						}*/
                        // TODO wegen Oracle nur Abfrage auf Attributname
                        if (rsmd.getColumnName(k).equalsIgnoreCase(pA.get("column_name").toString())) {
                            pKey.put("value", getValueAt(selectedRow, k - 1));
                            break;
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to read key from result set", e);
                    return null;
                }
            }
        }
        return keys;
    }

    public void addRows(ResultSet rset) {
        if (rset == null) return;
        this.rset = rset;
        try {
            this.rsmd = rset.getMetaData();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load metadata for new rows", e);
        }
    }

    public ResultSetMetaData getMetaData() {
        return rsmd;
    }

    @Override
    public String getColumnName(int c) {
        if (c < 0 || c >= columns.size()) return null;
        return columns.get(c).toString();
    }

    public int getColumnCount() {
        return columns.size();
    }

    public Object getValueAt(int r, int c) {
        try {
            rset.absolute(r + 1);
            Object o = rset.getObject(c + 1);
            if (o == null) return null;
            if (o instanceof Timestamp o1) {
                LocalDate dt = o1.toLocalDateTime().toLocalDate();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");

                return fmt.format(dt);
            }
            if (o instanceof Date) {
                LocalDate dt = ((Date) o).toLocalDate();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                return fmt.format(dt);
            }
            return o;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve cell value", e);
        }
        return null;
    }

    public int getRowCount() {
        if (rset == null) return 0;
        try {
            rset.last();
            return rset.getRow();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get row count", e);
        }
        return 0;
    }

    @Override
    public Class<?> getColumnClass(int col) {
        if (rset == null) return super.getColumnClass(col);
        try {
            rset.absolute(1);
            Object o = rset.getObject(col + 1);
            if (o == null) return super.getColumnClass(col);
            if (o instanceof Timestamp) return super.getColumnClass(col);
            return o.getClass();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to determine column class", e);
        }
        return super.getColumnClass(col);
    }
}
