
package compucrash;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;

public class CDataObjectFactory {

	private static CDataObjectFactory uniqueInstance = null;

	private Hashtable cListDataObjects = new Hashtable();
	private Hashtable cInfoDataObjects = new Hashtable();
	private DatabaseMetaData dbmd = null;

	private CDataObjectFactory() {
		uniqueInstance = this;
		try {
			dbmd = CDataManager.getInstance().getStatement().getConnection().getMetaData();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		prepareCInfoDataObjects();
		prepareCListDataObjects();
		String objectName;
	}

	private void prepareCInfoDataObjects() {
		try {
			ResultSet rset = CDataManager.getInstance().prepareCInfoDataObjects();
			String objectDesc = "";
			String objectTable = "";
			CProperties po = new CProperties();
			CProperties pas = new CProperties();
			CProperties pa = new CProperties();
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
				String col7 = Integer.toString(rset.getInt(7));
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
					po = new CProperties();
					objectDesc = col1;					
					po.put("object_name", col1);
					po.put("object_label", col2);
					po.put("apply", col27);
					po.put("color", col24);
					pas = new CProperties();
					po.put("attributes", pas);
					objectTable = "";
					pt = new CProperties();
					po.put("tables", pt);
					i = 0;
					j = 0;
					CProperties pcb = new CProperties();
					po.put("cust_buttons", pcb);
					try {
					    ResultSet rset3 = CDataManager.getInstance().getCInfoDataCustButtons(col1);
					    int k = 0;
					    while (rset3.next()) {
					        k++;
					        CProperties pcbi = new CProperties();
					        pcb.put(Integer.toString(k),pcbi);
					        pcbi.put("bez", rset3.getString(1));
					        pcbi.put("panel",rset.getString(2));
					    }
					    rset3.close();
					} catch (SQLException e) {
					    e.printStackTrace();
					}
				}
				i++;
				pa = new compucrash.CProperties();
				if (!(col3 + compucrash.CDataManager.schemaSeparator + col4).equalsIgnoreCase(objectTable)) {
					objectTable = col3 + compucrash.CDataManager.schemaSeparator + col4;
					if (col5.equals("1")) {
						pt.put("0", objectTable);
					} else {
					    boolean ins = false;
					    for (Enumeration en = pt.elements(); en.hasMoreElements();) {
					        if(en.nextElement().toString().equalsIgnoreCase(objectTable)) ins = true;
					    }
					    if (!ins) {
						j++;
						pt.put(Integer.toString(j), objectTable);
						ins = false;
					    }
					}
				}
				pa.put("owner", col3);
				pa.put("table_name", col4);
				//				pa.put("isleading",rset.getString(5));
				pa.put("column_name", col6);
				//				pa.put("pos_info",rset.getString(7));
				pa.put("label", col8);
				pas.put(Integer.toString(i), pa);
				// TODO key ersetzen durch iskey in Attributen.
				if (col9.equals("1")) {
					po.put("key", col6);
				}
				pa.put("iskey", col9);
				if (col10 != null) pa.put("tooltip", col10);
				pa.put("data_type", col11);
				if (col12 != null) pa.put("data_length", col12);
				if (col13 != null) pa.put("data_precision", col13);
				pa.put("data_scale", col14);
				pa.put("view_panel", col15);
				pa.put("panel", col16);
				pa.put("label_length", col17);
				pa.put("editable", col18);
				if (col19 != null) pa.put("formula", col19);
				if (col20 != null) pa.put("data_height", col20);
				if (col21 != null) pa.put("gridwidth", col21);
				if (col22 != null) pa.put("source", col22);
				if (col23 != null) pa.put("init", col23);
				if (col25 != null) pa.put("springen", col25);
				if (col26 != null) pa.put("action", col26);
				try {
//					ResultSet rset2 = dbmd.getColumns(null, col3, col4.toLowerCase(), col6.toLowerCase());
					ResultSet rset2 = dbmd.getColumns(null, col3.toUpperCase(), col4.toUpperCase(), col6.toUpperCase());
					CMessage.print("CDataObjectFactory.prepareCInfoDataObjects():");
					System.out.println(rset2.next());
					int sqltype = rset2.getInt(5);
					pa.put("sqltype", new Integer(sqltype));
					System.out.println(pa);
					rset2.close();
				} catch (SQLException e) {}
				// CInfoDataObject erzeugen und in Hashtable eintragen.
				cInfoDataObjects.put(objectDesc, new CInfoDataObject(po));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private void prepareCListDataObjects() {
		try {
			ResultSet rset = CDataManager.getInstance().prepareCListDataObjects();
			String objectDesc = "";
			String objectTable = "";
			CProperties po = new CProperties();
			CProperties pas = new CProperties();
			CProperties pa = new CProperties();
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
				String col7 = Integer.toString(rset.getInt(7));
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
					po = new CProperties();
					objectDesc = col1;					
					po.put("object_name", col1);
					po.put("object_label", col2);
					po.put("color", col20);
					if (col11 != null) po.put("bnew", col11);
					if (col12 != null) po.put("bedit", col12);
					if (col13 != null) po.put("bdelete", col13);
					if (col14 != null) po.put("bcopy", col14);
					if (col15 != null) po.put("bdisplay", col15);
					pas = new CProperties();
					po.put("attributes", pas);
					objectTable = "";
					pt = new CProperties();
					po.put("tables", pt);
					i = 0;
					j = 0;
					CProperties pcb = new CProperties();
					po.put("cust_buttons", pcb);
					try {
					    ResultSet rset3 = CDataManager.getInstance().getCListDataCustButtons(col1);
					    int k = 0;
					    while (rset3.next()) {
					        k++;
					        CProperties pcbi = new CProperties();
					        pcb.put(Integer.toString(k),pcbi);
					        pcbi.put("bez", rset3.getString(1));
//					        pcbi.put("panel",rset.getString(2));
					    }
					    rset3.close();
					} catch (SQLException e) {
					    e.printStackTrace();
					}
				}
				i++;
				pa = new CProperties();
				if (!(col3 + CDataManager.schemaSeparator + col4).equalsIgnoreCase(objectTable)) {
					objectTable = col3 + CDataManager.schemaSeparator + col4;
					if (col5.equals("1")) {
						pt.put("0", objectTable);
					} else {
					    boolean ins = false;
					    for (Enumeration en = pt.elements(); en.hasMoreElements();) {
					        if(en.nextElement().toString().equalsIgnoreCase(objectTable)) ins = true;
					    }
					    if (!ins) {
						j++;
						pt.put(Integer.toString(j), objectTable);
						ins = false;
					    }
					}
				}
				pa.put("owner", col3);
				pa.put("table_name", col4);
				pa.put("column_name", col6);
				pa.put("label", col8);
				if (col10 != null) pa.put("formula", col10);
				pas.put(Integer.toString(i), pa);
				if (col9.equals("1")) {
					po.put("key", col6);
				}
				pa.put("iskey",col9);
				if (col16 != null) pa.put("list_data_scale", col16);
				if (col17 != null) po.put("default_button", col17);
				if (col18 != null) {
					//  Sortierung
					if(po.get("order") == null) {
						CProperties pord = new CProperties();
						po.put("order", pord);
					}
					CProperties pordno = new CProperties();
					int temp = Integer.parseInt(col18);
					String order = Integer.toString(i);
					if (temp < 0) {
						temp = -temp;
						col18 = Integer.toString(temp);
						order = order + " DESC ";
					}
					((CProperties)po.get("order")).put(col18, order);
				}
				if (col19 != null) po.put("limitation", col19);
/*				try {
				    ResultSet rset4 = CDataManager.getInstance().getTables(col1);
					CMessage.print("CDataObjectFactory.prepareCListDataObjects().col1");
					CMessage.print(col1);
				    int k = 0;
				    while (rset4.next()) {
				        String t_owner = rset4.getString(1);
				        String t_table_name = rset4.getString(2);
				        String t_is_leading = rset4.getString(3);
				        if (t_is_leading.equalsIgnoreCase("1")) {
				            pt.put("0",t_owner + CDataManager.schemaSeparator + t_table_name);
				        } else {
				            k++;
				            pt.put(Integer.toString(k),t_owner + "." + t_table_name);
				        }
				    }
				} catch (SQLException e) {
				    e.printStackTrace();
				}*/
				// CListDataObject erzeugen und in Hashtable eintragen.
				cListDataObjects.put(objectDesc, new CListDataObject(po));
				CMessage.print("CDataObjectFactory.prepareCListDataObjects().po");
				CMessage.print(po);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void dispose() {
		uniqueInstance = null;
	}

	public static CDataObjectFactory getInstance() {
		if (uniqueInstance == null)
			return new CDataObjectFactory();
		return uniqueInstance;
	}

	public static CInfoDataObject getCInfoDataObject(String object_name) {
		return (CInfoDataObject) (CDataObjectFactory.getInstance().cInfoDataObjects.get(object_name));
	}

	public static CListDataObject getCListDataObject(String object_name) {
		return (CListDataObject) (CDataObjectFactory.getInstance().cListDataObjects.get(object_name));
	}
}