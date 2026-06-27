package compucrash;

import java.sql.ResultSet;

import javax.swing.table.TableModel;

public class CListDataObject {

	private CProperties p;
	private String objectLabel;
	private String SQLString;
	private CListDataManagingDatabase manager;
	private String searchText;

	public CListDataObject(CProperties p) {
		super();
		this.p = p;
		manager = CDataManager.getInstance().createCListDataManagingDatabase(this);
		objectLabel = (String) p.get("object_label");
	}
	
	public CTableModel select(int orderColumn) {
		searchText = null;
		CProperties pAttributes = (CProperties) p.get("attributes");
		CTableModel tabModel = new CTableModel();
		for (int i = 1; i <= pAttributes.size(); i++) {
			CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
			tabModel.addColumn((String) pA.get("label"));
		}
		ResultSet rset = manager.getSelect(orderColumn);
		tabModel.addRows(rset);
		tabModel.setProperties(p);
		return tabModel;
	}

	public String getObjectLabel() {
		return objectLabel;
	}
	
	public CProperties getCProperties() {
		return p;
	}
	
	public CProperties getKeys() {
		CProperties pAttributes = (CProperties)p.get("attributes");
		int j = 0;
		CProperties keys = new CProperties();
		for (int i = 1; i <= pAttributes.size(); i++) {
			CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
			if (!pA.get("iskey").toString().equalsIgnoreCase("0")) {
				CProperties pKey = new CProperties();
				j++;
				keys.put(Integer.toString(j), pKey);
				pKey.put("owner", pA.get("owner"));
				pKey.put("table_name", pA.get("table_name"));
				pKey.put("column_name", pA.get("column_name"));
			}
		}
		return keys;
	}

	public CTableModel search(String text, int orderColumn) {
		if (text == null) return select(orderColumn);
		searchText = text;
		CProperties pAttributes = (CProperties) p.get("attributes");
		CTableModel tabModel = new CTableModel();
		for (int i = 1; i <= pAttributes.size(); i++) {
			CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
			tabModel.addColumn((String) pA.get("label"));
		}
		ResultSet rset = manager.getSelect(text);
		tabModel.addRows(rset);
		tabModel.setProperties(p);
		return tabModel;
	}

	public TableModel refresh(int orderColumn) {
		if (searchText == null) {
			return select(orderColumn);
		} else {
			return search(searchText, orderColumn);
		}
	}

	public TableModel select(CProperties pTab) {
		CProperties pAttributes = (CProperties) p.get("attributes");
		CTableModel tabModel = new CTableModel();
		for (int i = 1; i <= pAttributes.size(); i++) {
			CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
			tabModel.addColumn(pA.get("label").toString());				
		}
		ResultSet rset = manager.select(pTab);
		tabModel.addRows(rset);
		tabModel.setProperties(p);
		
		return tabModel;
	}

	public TableModel search(String text) {
		if (text == null || text == "") return select(p);
		searchText = text;
		CProperties pAttributes = (CProperties) p.get("attributes");
		CTableModel tabModel = new CTableModel();
		for (int i = 1; i <= pAttributes.size(); i++) {
			CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
			tabModel.addColumn((String) pA.get("label"));
		}
		ResultSet rset = manager.getSelect(text);
		tabModel.addRows(rset);
		tabModel.setProperties(p);
		return tabModel;
	}
}

