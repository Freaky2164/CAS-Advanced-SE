package compucrash;

import javax.swing.table.AbstractTableModel;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CTableModel extends AbstractTableModel {

    private static final String COLUMNNAME = "column_name";
    private static final Logger LOGGER = Logger.getLogger(CTableModel.class.getName());
    private final ArrayList<Object> columns = new ArrayList<>();
    private transient ResultSet rset;
    private transient ResultSetMetaData rsmd;
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
                pKey.put(COLUMNNAME, pA.get(COLUMNNAME));
                try {
                    for (int k = 1; k <= rsmd.getColumnCount(); k++) {
                        // TODO wegen Oracle nur Abfrage auf Attributname
                        if (rsmd.getColumnName(k).equalsIgnoreCase(pA.get(COLUMNNAME).toString())) {
                            pKey.put("value", getValueAt(selectedRow, k - 1));
                            break;
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to read key from result set", e);
                    return (CProperties) Collections.emptyMap();
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
            if (o instanceof Date date) {
                LocalDate dt = date.toLocalDate();
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
