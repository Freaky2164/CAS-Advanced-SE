
package compucrash;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

public abstract class CInfoDataManagingDatabase {

	protected CInfoDataObject parent;
	protected String SQLString;
	protected CProperties p;

	public CInfoDataManagingDatabase(CInfoDataObject parent) {
		this.parent = parent;
	}
	
	protected void forUpdate() {}

	protected void prepareSQLString(CProperties p) {
		String SQLFrom = new String();
		String SQLSelect = new String();
		String SQLWhere = new String();

		this.p = p;
		CProperties pTables = (CProperties) p.get("tables");
		for (int i = 0; i < pTables.size(); i++) {
			SQLFrom += (String) pTables.get(Integer.toString(i)) + ",";
		}
		SQLFrom = SQLFrom.substring(0, SQLFrom.length() - 1);
		CProperties pAttributes = (CProperties) p.get("attributes");
		for (int i = 1; i <= pAttributes.size(); i++) {
			CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
			if (pA.get("formula") != null) {
				SQLSelect += (String) pA.get("formula") + " "
						+ (String) pA.get("column_name") + ",";
			} else if (pA.get("data_type").toString().equalsIgnoreCase("TABLE") 
			        || pA.get("data_type").toString().equalsIgnoreCase("LIST") 
			        || pA.get("data_type").toString().equalsIgnoreCase("HISTORY")) {
				SQLSelect += " NULL "
				+ (String) pA.get("column_name") + ",";
			} else {
//			    if (pA.get("data_type").toString().equalsIgnoreCase("KRYPT")) {
//			        SQLSelect += "UNKRYPT(" + (String) pA.get("owner") + CDataManager.schemaSeparator
//	        		+ (String) pA.get("table_name") + "."
//	        		+ (String) pA.get("column_name") + "),";
//			    } else {
			        SQLSelect += (String) pA.get("owner") + CDataManager.schemaSeparator
			        		+ (String) pA.get("table_name") + "."
			        		+ (String) pA.get("column_name") + ",";
//			    }
			}
		}
		SQLSelect = SQLSelect.substring(0, SQLSelect.length() - 1);

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
		if (SQLWhere.length() >= 5) {
			SQLWhere = SQLWhere.substring(0, SQLWhere.length() - 5);
		} else {
			SQLWhere = "1 = 1";
		}
		this.SQLString = "SELECT " + SQLSelect + " FROM " + SQLFrom + " WHERE "
				+ SQLWhere;
	}

	CDataObject getCDataObject(CProperties keys, boolean b) {
		if (keys == null) return new CDataObject(null);
		String SQLObjectString = SQLString;
		for (int i = 1; i <= keys.size(); i++) {
			CProperties pKey = (CProperties) keys.get(Integer.toString(i));
			SQLObjectString += " AND " + pKey.get("owner") + CDataManager.schemaSeparator + pKey.get("table_name") + 
			"." + pKey.get("column_name") + " = '" + pKey.get("value") + "' ";
		}
		Object[] rowData = null;
		try {
			if (b) {
			    forUpdate();
				begin();
			}
			ResultSet rset = CDataManager.getInstance().getStatement().executeQuery(SQLObjectString);
			CMessage.print("CInfoDataManagingDatabase.getCDataObject():");
			CMessage.print(SQLObjectString);
			ResultSetMetaData rsmd = rset.getMetaData();
			rowData = new Object[rsmd.getColumnCount()];
			rset.next();
			for (int i = 0; i < rsmd.getColumnCount(); i++) {
				Object o = rset.getObject(i+1);
				if (rset.wasNull()) {
					rowData[i] = new CNull();					
				} else {
					rowData[i] = o;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			CMessage.print(SQLObjectString);
		} catch (NumberFormatException nfe) {
			CMessage.print(nfe);
		}
		return new CDataObject(rowData);
	}

	public void begin() {};
	
	void insert(CDataObject actual) throws SQLException {
		PreparedStatement pstmt;
		String SQLInsertString = "";
		String SQLInsertValueString = "";
			begin();
			CProperties pTables = (CProperties) p.get("tables");
			CProperties pAttributes = (CProperties) p.get("attributes");
			for (int i = 0; i < pTables.size(); i++) {
				SQLInsertString = "INSERT INTO " + (String) pTables.get(Integer.toString(i)) + "(";
				SQLInsertValueString = "";
				for (int j = 1; j <= pAttributes.size(); j++) {
					CProperties pAttrProperties = (CProperties) pAttributes.get(Integer.toString(j));
					if (actual.get(j) == null || (actual.get(j) != null && !actual.get(j).getClass().equals(CNull.class))) {
						if (((String) pAttrProperties.get("owner") 
						        + CDataManager.schemaSeparator + (String) pAttrProperties.get("table_name"))
						        .equalsIgnoreCase((String) pTables.get(Integer.toString(i))) 
						        || !pAttrProperties.get("iskey").toString().equalsIgnoreCase("0")) {
							if (!((String) pAttrProperties.get("editable")).equals("0") 
							        && !(pAttrProperties.get("data_type").toString().equalsIgnoreCase("TABLE")
							        || pAttrProperties.get("data_type").toString().equalsIgnoreCase("LIST")
							        || pAttrProperties.get("data_type").toString().equalsIgnoreCase("HISTORY"))) {
								SQLInsertString += (String) pAttrProperties.get("column_name") + ",";
							    SQLInsertValueString += "?,";
							}
						}
					}
				}
				SQLInsertString = SQLInsertString.substring(0, SQLInsertString.length() - 1);
				SQLInsertValueString = SQLInsertValueString.substring(0, SQLInsertValueString.length() - 1);
				SQLInsertString += ") VALUES (" + SQLInsertValueString + ")";
				CMessage.print(SQLInsertString);
				pstmt = CDataManager.getInstance().getConnection().prepareStatement(SQLInsertString);
				int k = 0;
				for (int j = 1; j <= pAttributes.size(); j++) {
					CProperties pAttrProperties = (CProperties) pAttributes.get(Integer.toString(j));
					if (actual.get(j) == null
						|| (actual.get(j) != null && !actual.get(j).getClass().equals(CNull.class))) {
						if (((String) pAttrProperties.get("owner") + CDataManager.schemaSeparator 
							+ (String) pAttrProperties.get("table_name"))
							.equalsIgnoreCase((String) pTables.get(Integer.toString(i))) 
							|| !pAttrProperties.get("iskey").toString().equalsIgnoreCase("0")) {
							if (!((String) pAttrProperties.get("editable")).equals("0") 
							        && !(pAttrProperties.get("data_type").toString().equalsIgnoreCase("TABLE")
								    || pAttrProperties.get("data_type").toString().equalsIgnoreCase("LIST")
							        || pAttrProperties.get("data_type").toString().equalsIgnoreCase("HISTORY"))) {
								k++;
								CMessage.print(Integer.valueOf(k));
								if (actual.get(j) == null) {
									pstmt.setNull(k,((Integer)pAttrProperties.get("sqltype")).intValue());
									CMessage.print("--null--");
								} else {
									if (actual.get(j) instanceof LocalDate) {
										Date date = Date.valueOf((LocalDate) actual.get(j));
										pstmt.setObject(k,date,((Integer)pAttrProperties.get("sqltype")).intValue());
										CMessage.print(date);
									} else if (actual.get(j) instanceof LocalDateTime) {
										Timestamp timestamp = Timestamp.valueOf((LocalDateTime) actual.get(j));
										pstmt.setObject(k,timestamp,((Integer)pAttrProperties.get("sqltype")).intValue());
										CMessage.print(timestamp);
									} else {
										pstmt.setObject(k, actual.get(j),((Integer)pAttrProperties.get("sqltype")).intValue());
										CMessage.print(actual.get(j));
									}
								}
							}
						}
					}
				}
				pstmt.executeUpdate();
			}
			CDataManager.getInstance().getConnection().commit();
			CMessage.print("committed");
	}

	void delete(CProperties keys) throws SQLException {
		String SQLObjectString = "";
			compucrash.CProperties pTables = (CProperties) p.get("tables");
			CProperties pAttributes = (CProperties) p.get("attributes");
			for (int j = pTables.size() - 1; j >= 0; j--) {
				SQLObjectString = "DELETE FROM "
				+ (String) pTables.get(Integer.toString(j)) + " WHERE ";
				for (int i = 1; i <= pAttributes.size(); i++) {
					CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
					if (!pA.get("iskey").toString().equalsIgnoreCase("0")) {
						SQLObjectString += (String) pA.get("column_name") + " = '";
						for (int k = 1; k <= keys.size(); k++) {
							CProperties key = (CProperties) keys.get(Integer.toString(k));
							if (key.get("column_name").toString()
								.equalsIgnoreCase(
								pA.get("column_name").toString())) {
								SQLObjectString += key.get("value") + "' AND ";
							}
						}
					}
				}
				SQLObjectString = SQLObjectString.substring(0, SQLObjectString.length() - 5);
				CDataManager.getInstance().getStatement().execute(SQLObjectString);
			}
			CDataManager.getInstance().getConnection().commit();
			CMessage.print("committed");
	}

	void update(CProperties keys, CDataObject actual) throws SQLException {
		String SQLUpdateString = "";
		CProperties pTables = (CProperties) p.get("tables");
		CProperties pAttributes = (CProperties) p.get("attributes");
		PreparedStatement pstmt;
		
		for (int i = pTables.size() - 1; i >= 0; i--) {
			SQLUpdateString = "";
			for (int j = 1; j <= pAttributes.size(); j++) {
				CProperties pAttrProperties = (CProperties) pAttributes.get(Integer.toString(j));
				if (actual.get(j) == null
						|| (actual.get(j) != null && !actual.get(j).getClass().equals(CNull.class))) {
					if (((String) pAttrProperties.get("owner") + CDataManager.schemaSeparator + (String) pAttrProperties
							.get("table_name"))
							.equalsIgnoreCase((String) pTables.get(Integer.toString(i)))) {
						if (!((String) pAttrProperties.get("editable")).equals("0")
								&& !(pAttrProperties.get("data_type").toString().equalsIgnoreCase("TABLE")
								|| pAttrProperties.get("data_type").toString().equalsIgnoreCase("LIST")
								|| pAttrProperties.get("data_type").toString().equalsIgnoreCase("HISTORY"))) {
							SQLUpdateString += (String) pAttrProperties.get("column_name") + " = ?,";
						}
//						actual.remove(Integer.toString(j));
					}
				}
			}
			if (SQLUpdateString.length() != 0) {
				SQLUpdateString = SQLUpdateString.substring(0, SQLUpdateString.length() - 1);
				SQLUpdateString = "UPDATE "
						+ (String) pTables.get(Integer.toString(i)) + " SET "
						+ SQLUpdateString;
				SQLUpdateString += " WHERE ";
				for (int m = 1; m <= pAttributes.size(); m++) {
					CProperties pA = (CProperties) pAttributes.get(Integer.toString(m));
					if (!pA.get("iskey").toString().equalsIgnoreCase("0")) {
						SQLUpdateString += (String) pA.get("column_name")
								+ " = '";
						for (int k = 1; k <= keys.size(); k++) {
							CProperties key = (CProperties) keys.get(Integer.toString(k));
							if (key.get("column_name").toString()
									.equalsIgnoreCase(
											pA.get("column_name").toString())) {
								SQLUpdateString += key.get("value") + "' AND ";
							}
						}
					}
				}
				SQLUpdateString = SQLUpdateString.substring(0, SQLUpdateString.length() - 5);
				CMessage.print(SQLUpdateString);
				pstmt = CDataManager.getInstance().getConnection().prepareStatement(SQLUpdateString);
				int k = 0;
				for (int j = 1; j <= pAttributes.size(); j++) {
					CProperties pAttrProperties = (CProperties) pAttributes.get(Integer.toString(j));
					if (actual.get(j) == null
							|| (actual.get(j) != null && !actual.get(j).getClass().equals(CNull.class))) {
						if (((String) pAttrProperties.get("owner") + CDataManager.schemaSeparator + (String) pAttrProperties
								.get("table_name"))
								.equalsIgnoreCase((String) pTables.get(Integer.toString(i)))) {
							if (!((String) pAttrProperties.get("editable")).equals("0")
									&& !(pAttrProperties.get("data_type").toString().equalsIgnoreCase("TABLE")
									|| pAttrProperties.get("data_type").toString().equalsIgnoreCase("LIST")
									|| pAttrProperties.get("data_type").toString().equalsIgnoreCase("HISTORY"))) {
								k++;
								if (actual.get(j) == null) {
									pstmt.setNull(k,((Integer)pAttrProperties.get("sqltype")).intValue());
								} else {
									if (actual.get(j) instanceof LocalDate) {
										Date date = Date.valueOf((LocalDate) actual.get(j));
										pstmt.setObject(k,date,((Integer)pAttrProperties.get("sqltype")).intValue());
									} else if (actual.get(j) instanceof LocalDateTime) {
										Timestamp timestamp = Timestamp.valueOf((LocalDateTime) actual.get(j));
										pstmt.setObject(k,timestamp,((Integer)pAttrProperties.get("sqltype")).intValue());
									} else {
										pstmt.setObject(k,actual.get(j),((Integer)pAttrProperties.get("sqltype")).intValue());
									}
								}
							}
							actual.remove(Integer.toString(j));
						}
					}
				}
				pstmt.executeUpdate();
			}
		}
		CDataManager.getInstance().getConnection().commit();
		CMessage.print("committed");
	}
}
