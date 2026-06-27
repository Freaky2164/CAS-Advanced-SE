package compucrash;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CDataObjectFactory {

    private static final String SQLTYPE = "sqltype";
    private static final String PANEL = "panel";
    private static final String ORDER = "order";
    private static final String ATTRIBUTES = "attributes";
    private static final String TABLES = "tables";
    private static final String CUST_BUTTONS = "cust_buttons";
    private static final Logger LOGGER = Logger.getLogger(CDataObjectFactory.class.getName());
    private static CDataObjectFactory uniqueInstance = null;
    private final HashMap<String, CListDataObject> cListDataObjects = new HashMap<>();
    private final HashMap<String, CInfoDataObject> cInfoDataObjects = new HashMap<>();
    private DatabaseMetaData dbmd = null;

    private CDataObjectFactory() {
        try {
            dbmd = CDataManager.getInstance().getStatement().getConnection().getMetaData();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to read database metadata", ex);
        }

        prepareCInfoDataObjects();
        prepareCListDataObjects();
    }

    public static CDataObjectFactory getInstance() {
        if (uniqueInstance == null) {
            uniqueInstance = new CDataObjectFactory();
        }
        return uniqueInstance;
    }

    public static CInfoDataObject getCInfoDataObject(String objectName) {
        return CDataObjectFactory.getInstance().cInfoDataObjects.get(objectName);
    }

    public static CListDataObject getCListDataObject(String objectName) {
        return CDataObjectFactory.getInstance().cListDataObjects.get(objectName);
    }

    private static String toUpper(String s) {
        return s == null ? null : s.toUpperCase();
    }

    private static String toLower(String s) {
        return s == null ? null : s.toLowerCase();
    }

    public static void dispose() {
        uniqueInstance = null;
    }

    private void prepareCInfoDataObjects() {
        try (ResultSet rset = CDataManager.getInstance().prepareCInfoDataObjects()) {
            String objectDesc = "";
            String objectTable = "";
            CProperties po = new CProperties();
            CProperties pas = new CProperties();
            new CProperties();
            CProperties pt = new CProperties();
            int i = 0;
            int j = 0;
            while (rset.next()) {
                String col1 = rset.getString(1);
                String col2 = rset.getString(2);
                String col3 = rset.getString(3);
                String col4 = rset.getString(4);
                String col5 = Integer.toString(rset.getInt(5));
                String col6 = rset.getString(6);
                String col8 = rset.getString(8);
                String col9 = Integer.toString(rset.getInt(9));
                String col10 = rset.getString(10);
                String col11 = rset.getString(11);
                String col12 = Integer.toString(rset.getInt(12));
                String col13 = Integer.toString(rset.getInt(13));
                String col14 = Integer.toString(rset.getInt(14));
                String col15 = rset.getString(15);
                String col16 = rset.getString(16);
                String col17 = Integer.toString(rset.getInt(17));
                String col18 = Integer.toString(rset.getInt(18));
                String col19 = rset.getString(19);
                String col20 = Integer.toString(rset.getInt(20));
                String col21 = Integer.toString(rset.getInt(21));
                String col22 = rset.getString(22);
                String col23 = rset.getString(23);
                String col24 = rset.getString(24);
                String col25 = rset.getString(25);
                String col26 = rset.getString(26);
                String col27 = rset.getString(27);
                if (!col1.equalsIgnoreCase(objectDesc)) {
                    objectDesc = col1;
                    po = createInfoObject(col1, col2, col27, col24);
                    pas = (CProperties) po.get(ATTRIBUTES);
                    pt = (CProperties) po.get(TABLES);
                    objectTable = "";
                    i = 0;
                    j = 0;
                }
                i++;
                new CProperties();
                if (!(col3 + compucrash.CDataManager.getSchemaSeparator() + col4).equalsIgnoreCase(objectTable)) {
                    objectTable = col3 + compucrash.CDataManager.getSchemaSeparator() + col4;
                    j = updateTableEntry(pt, objectTable, col5, j);
                }
                addInfoAttribute(po, pas, i, col3, col4, col6, col8, col9, col10, col11, col12, col13, col14,
                        col15, col16, col17, col18, col19, col20, col21, col22, col23, col25, col26);
                // CInfoDataObject erzeugen und in Hashtable eintragen.
                cInfoDataObjects.put(objectDesc, new CInfoDataObject(po));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to prepare info data objects", ex);
        }

    }

    private int getColumnType(String schema, String table, String column) throws SQLException {
        String[] schemaCandidates = {schema, toUpper(schema), toLower(schema)};
        String[] tableCandidates = {table, toUpper(table), toLower(table)};
        String[] columnCandidates = {column, toUpper(column), toLower(column)};
        for (String s : schemaCandidates) {
            for (String t : tableCandidates) {
                for (String c : columnCandidates) {
                    int result = tryGetColumnType(s, t, c);
                    if (result != Integer.MIN_VALUE) return result;
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    private int tryGetColumnType(String schema, String table, String column) throws SQLException {
        if (schema == null || table == null || column == null) return Integer.MIN_VALUE;
        ResultSet rset2 = dbmd.getColumns(null, schema, table, column);
        try {
            if (rset2.next()) {
                return rset2.getInt(5);
            }
        } finally {
            rset2.close();
        }
        return Integer.MIN_VALUE;
    }

    private int updateTableEntry(CProperties pt, String objectTable, String col5, int j) {
        if (col5.equals("1")) {
            pt.put("0", objectTable);
        } else {
            boolean ins = false;
            for (Enumeration<?> en = pt.elements(); en.hasMoreElements(); ) {
                if (en.nextElement().toString().equalsIgnoreCase(objectTable)) ins = true;
            }
            if (!ins) {
                j++;
                pt.put(Integer.toString(j), objectTable);
            }
        }
        return j;
    }

    private void prepareCListDataObjects() {
        try (ResultSet rset = CDataManager.getInstance().prepareCListDataObjects()) {
            String objectDesc = "";
            String objectTable = "";
            CProperties po = new CProperties();
            CProperties pas = new CProperties();
            new CProperties();
            CProperties pt = new CProperties();
            int i = 0;
            int j = 0;
            while (rset.next()) {
                String col1 = rset.getString(1);
                String col2 = rset.getString(2);
                String col3 = rset.getString(3);
                String col4 = rset.getString(4);
                String col5 = Integer.toString(rset.getInt(5));
                String col6 = rset.getString(6);
                String col8 = rset.getString(8);
                String col9 = Integer.toString(rset.getInt(9));
                String col10 = rset.getString(10);
                String col11 = rset.getString(11);
                String col12 = rset.getString(12);
                String col13 = rset.getString(13);
                String col14 = rset.getString(14);
                String col15 = rset.getString(15);
                String col16 = Integer.toString(rset.getInt(16));
                String col17 = rset.getString(17);
                String col18 = rset.getString(18);
                String col19 = rset.getString(19);
                String col20 = rset.getString(20);
                if (!col1.equalsIgnoreCase(objectDesc)) {
                    objectDesc = col1;
                    po = createListObject(col1, col2, col20, col11, col12, col13, col14, col15);
                    pas = (CProperties) po.get(ATTRIBUTES);
                    pt = (CProperties) po.get(TABLES);
                    objectTable = "";
                    i = 0;
                    j = 0;
                }
                i++;
                new CProperties();
                if (!(col3 + CDataManager.getSchemaSeparator() + col4).equalsIgnoreCase(objectTable)) {
                    objectTable = col3 + CDataManager.getSchemaSeparator() + col4;
                    j = updateTableEntry(pt, objectTable, col5, j);
                }
                addListAttribute(po, pas, i, col3, col4, col6, col8, col9, col10, col16, col17, col18, col19);
                // CListDataObject erzeugen und in Hashtable eintragen.
                cListDataObjects.put(objectDesc, new CListDataObject(po));
                CMessage.print("CDataObjectFactory.prepareCListDataObjects().po");
                CMessage.print(po);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to prepare list data objects", ex);
        }
    }

    private CProperties createInfoObject(String objectName, String objectLabel, String apply, String color) {
        CProperties po = new CProperties();
        CProperties pas = new CProperties();
        CProperties pt = new CProperties();
        po.put("objectName", objectName);
        po.put("object_label", objectLabel);
        po.put("apply", apply);
        po.put("color", color);
        po.put(ATTRIBUTES, pas);
        po.put(TABLES, pt);
        CProperties pcb = new CProperties();
        po.put(CUST_BUTTONS, pcb);
        try (ResultSet rset3 = CDataManager.getInstance().getCInfoDataCustButtons(objectName)) {
            int k = 0;
            while (rset3.next()) {
                k++;
                CProperties pcbi = new CProperties();
                pcb.put(Integer.toString(k), pcbi);
                pcbi.put("bez", rset3.getString(1));
                pcbi.put(PANEL, rset3.getString(2));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load info custom buttons", ex);
        }
        return po;
    }

    private CProperties createListObject(String objectName, String objectLabel, String color, String bNew,
                                         String bEdit, String bDelete, String bCopy, String bDisplay) {
        CProperties po = new CProperties();
        CProperties pas = new CProperties();
        CProperties pt = new CProperties();
        po.put("objectName", objectName);
        po.put("object_label", objectLabel);
        po.put("color", color);
        if (bNew != null) po.put("bnew", bNew);
        if (bEdit != null) po.put("bedit", bEdit);
        if (bDelete != null) po.put("bdelete", bDelete);
        if (bCopy != null) po.put("bcopy", bCopy);
        if (bDisplay != null) po.put("bdisplay", bDisplay);
        po.put(ATTRIBUTES, pas);
        po.put(TABLES, pt);
        po.put(CUST_BUTTONS, new CProperties());
        try (ResultSet rset3 = CDataManager.getInstance().getCListDataCustButtons(objectName)) {
            int k = 0;
            CProperties pcb = (CProperties) po.get(CUST_BUTTONS);
            while (rset3.next()) {
                k++;
                CProperties pcbi = new CProperties();
                pcb.put(Integer.toString(k), pcbi);
                pcbi.put("bez", rset3.getString(1));
                pcbi.put(PANEL, rset3.getString(2));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load list custom buttons", ex);
        }
        return po;
    }

    private void addInfoAttribute(CProperties po, CProperties pas, int index, String owner, String tableName,
                                  String columnName, String label, String isKey, String tooltip, String dataType, String dataLength,
                                  String dataPrecision, String dataScale, String viewPanel, String panel, String labelLength,
                                  String editable, String formula, String dataHeight, String gridWidth, String source, String init,
                                  String springen, String action) {
        CProperties pa = new CProperties();
        pa.put("owner", owner);
        pa.put("table_name", tableName);
        pa.put("column_name", columnName);
        pa.put("label", label);
        pas.put(Integer.toString(index), pa);
        if ("1".equals(isKey)) {
            po.put("key", columnName);
        }
        pa.put("iskey", isKey);
        if (tooltip != null) pa.put("tooltip", tooltip);
        pa.put("data_type", dataType);
        if (dataLength != null) pa.put("data_length", dataLength);
        if (dataPrecision != null) pa.put("data_precision", dataPrecision);
        pa.put("data_scale", dataScale);
        pa.put("view_panel", viewPanel);
        pa.put(PANEL, panel);
        pa.put("label_length", labelLength);
        pa.put("editable", editable);
        if (formula != null) pa.put("formula", formula);
        if (dataHeight != null) pa.put("data_height", dataHeight);
        if (gridWidth != null) pa.put("gridwidth", gridWidth);
        if (source != null) pa.put("source", source);
        if (init != null) pa.put("init", init);
        if (springen != null) pa.put("springen", springen);
        if (action != null) pa.put("action", action);
        try {
            int sqlType = getColumnType(owner, tableName, columnName);
            if (sqlType != Integer.MIN_VALUE) {
                pa.put(SQLTYPE, Integer.valueOf(sqlType));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.FINE, "Could not determine SQL type", ex);
        }
    }

    private void addListAttribute(CProperties po, CProperties pas, int index, String owner, String tableName,
                                  String columnName, String label, String isKey, String formula, String listDataScale,
                                  String defaultButton, String orderBy, String limitation) {
        CProperties pa = new CProperties();
        pa.put("owner", owner);
        pa.put("table_name", tableName);
        pa.put("column_name", columnName);
        pa.put("label", label);
        if (formula != null) pa.put("formula", formula);
        pas.put(Integer.toString(index), pa);
        if ("1".equals(isKey)) {
            po.put("key", columnName);
        }
        pa.put("iskey", isKey);
        if (listDataScale != null) pa.put("list_data_scale", listDataScale);
        if (defaultButton != null) po.put("default_button", defaultButton);
        if (orderBy != null) {
            po.computeIfAbsent(ORDER, k -> new CProperties());
            int temp = Integer.parseInt(orderBy);
            String order = Integer.toString(index);
            if (temp < 0) {
                temp = -temp;
                orderBy = Integer.toString(temp);
                order = order + " DESC ";
            }
            ((CProperties) po.get(ORDER)).put(orderBy, order);
        }
        if (limitation != null) po.put("limitation", limitation);
    }
}
