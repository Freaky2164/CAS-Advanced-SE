package compucrash;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class CListDataManagingDatabase {
	
	protected CListDataObject parent;
	protected String SQLString;
	private CProperties p;
	
	public CListDataManagingDatabase(CListDataObject parent) {
		this.parent = parent;
		this.p = parent.getCProperties();
		prepareSQLString();
	}
	
	private void prepareSQLString() {
		String SQLFrom = new String();
		String SQLSelect = new String();
		String SQLWhere = new String();
		CProperties pTables = (CProperties) p.get("tables");
		for ( int i = 0; i < pTables.size(); i++) {
			SQLFrom += (String) pTables.get(Integer.toString(i)) + "," ;
		}
		SQLFrom = SQLFrom.substring(0,SQLFrom.length() - 1);
		CProperties pAttributes = (CProperties) p.get("attributes");
		for (int i = 1; i <= pAttributes.size(); i++) {
		    CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
		    if (pA.get("formula") != null) {
		        SQLSelect += (String) pA.get("formula") + " "
		        + (String) pA.get("column_name") + ",";
		    } else {
//		        if (pA.get("data_type").toString().equalsIgnoreCase("KRYPT")) {
//		            SQLSelect += "unkrypt(" + (String) pA.get("owner") + CDataManager.schemaSeparator
//		            + (String) pA.get("table_name") + "."
//		            + (String) pA.get("column_name") + "),";		            
//		        } else {
		            SQLSelect += (String) pA.get("owner") + CDataManager.schemaSeparator
		            + (String) pA.get("table_name") + "."
		            + (String) pA.get("column_name") + ",";
//		        }
		    }
		}
		SQLSelect = SQLSelect.substring(0,SQLSelect.length() - 1);
		for (int i = 1; i <= pAttributes.size(); i++) {
			CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
			if (!pA.get("iskey").toString().equalsIgnoreCase("0")) {
				for (int j = 1; j < pTables.size(); j++) {
					SQLWhere += pTables.get("0") + "." + pA.get("column_name")
							+ " = " + pTables.get(Integer.toString(j)) + "."
							+ pA.get("column_name") + " AND ";
				}
			}
		}
		if ( SQLWhere.length() >= 5) {
			SQLWhere = SQLWhere.substring(0,SQLWhere.length() - 5);	
		} else {
			SQLWhere = "1 = 1";
		}
		if (p.get("limitation") != null) {
			SQLWhere += " AND " + p.get("limitation").toString();
		}
		this.SQLString =  "SELECT " + SQLSelect + " FROM " + SQLFrom + " WHERE " + SQLWhere;
	}
	
	protected ResultSet getSelect(int orderColumn) {
		ResultSet rset = null;
		try {
			CMessage.print("CListDataManagingDatabase.getSelect:\n" + SQLString);
			rset = CDataManager.getInstance().getStatement().executeQuery(SQLString + " ORDER BY " + Integer.toString(orderColumn));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rset;
	}

	public ResultSet getSelect(String text) {
		ResultSet rset = null;
		String SQLSearchString = " AND (";
		CProperties pTables = (CProperties) p.get("tables");
		CProperties pAttributes = (CProperties) p.get("attributes");
		for (int i = 1; i <= pAttributes.size(); i++) {
			CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
			if (pA.get("formula") != null) {
// nicht in formeln suchen
//			    SQLSearchString += (String) pA.get("column_name") + " LIKE '%" +
//				text + "%' OR ";
			} else {
				SQLSearchString += (String) pA.get("owner") + CDataManager.schemaSeparator + 
				(String) pA.get("table_name") + "." + 
				(String) pA.get("column_name") + " LIKE '%" +
				text + "%' OR ";
			}
		}
		SQLSearchString = SQLSearchString.substring(0,SQLSearchString.length() - 4) + ")";
		String SQLOrderString;
		if (p.get("order") != null) {
			CProperties po = (CProperties)p.get("order");
			SQLOrderString = " ORDER BY ";
			for (int i = 1; i <= po.size(); i++) {
				SQLOrderString += po.get(Integer.toString(i)) + ", ";
			}
			SQLOrderString = SQLOrderString.substring(0,SQLOrderString.length() - 2);
		} else {
			SQLOrderString = "";
		}
		
		try {
			CMessage.print("CListDataManagingDatabase.getSelect(text):\n" + SQLString + SQLSearchString);
			rset = CDataManager.getInstance().getStatement().executeQuery(SQLString + SQLSearchString + SQLOrderString);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rset;
	}

	public ResultSet select(CProperties pTab) {
		//TODO getSelect durch die universelle select Methode ersetzen
		ResultSet rset = null;
		String SQLOrderString;
		if (pTab.get("order") != null) {
			CProperties po = (CProperties)pTab.get("order");
			SQLOrderString = " ORDER BY ";
			for (int i = 1; i <= po.size(); i++) {
				SQLOrderString += po.get(Integer.toString(i)) + ", ";
			}
			SQLOrderString = SQLOrderString.substring(0,SQLOrderString.length() - 2);
		} else {
			SQLOrderString = "";
		}
		String SQLFilterString;
		if (pTab.get("filter_and") != null) {
			CProperties pf = (CProperties)pTab.get("filter_and");
			SQLFilterString = " ";
			for (int i = 1; i <= pf.size(); i++) {
				CProperties pfa = (CProperties)pf.get(Integer.toString(i));
				SQLFilterString += "AND " 
				+ pfa.get("owner") + CDataManager.schemaSeparator 
				+ pfa.get("table_name") + "." 
				+ pfa.get("column_name") + " " 
				+ pfa.get("operator") + " '"
				+ pfa.get("value") + "' ";
			}
		} else {
			SQLFilterString = "";
		}
		if (pTab.get("filter_or") != null) {
			CProperties pf = (CProperties)pTab.get("filter_or");
			SQLFilterString += " AND ( 1 = 0 ";
			for (int i = 1; i <= pf.size(); i++) {
				CProperties pfa = (CProperties)pf.get(Integer.toString(i));
				SQLFilterString += "OR " 
				+ pfa.get("owner") + CDataManager.schemaSeparator 
				+ pfa.get("table_name") + "." 
				+ pfa.get("column_name") + " " 
				+ pfa.get("operator") + " '"
				+ pfa.get("value") + "' ";
			}
			SQLFilterString += ")";
		}
		
		try {
			CMessage.print("CListDataManagingDatabase.getSelect:\n" + SQLString + SQLFilterString + SQLOrderString);
			rset = CDataManager.getInstance().getStatement().executeQuery(SQLString + SQLFilterString + SQLOrderString);
		} catch (SQLException e) {
//			e.printStackTrace();
		}
		return rset;
	}
}
