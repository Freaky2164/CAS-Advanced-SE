package compucrash;
import java.sql.SQLException;

public class CInfoDataObject {
	
	private String SQLString;
	private String objectLabel;
	private CProperties p;
	private CInfoDataManagingDatabase manager;


	public CInfoDataObject(CProperties p){
		super();
		this.p = p;
		manager = CDataManager.getInstance().createCInfoDataManagingDatabase(this);
		objectLabel = (String) p.get("object_label");
		manager.prepareSQLString(p);
		CProperties pTables = (CProperties) p.get("tables");
	}
	
	public CDataObject getCDataObjectForUpdate(CProperties keys) {
		return getCDataObject(keys, true);
	}
	
	private CDataObject getCDataObject(CProperties keys, boolean b) {
		return manager.getCDataObject(keys, b);
	}

	public CDataObject getCDataObject(CProperties keys) {
		return getCDataObject(keys, false);
	}

	public void insert(CDataObject actual) throws SQLException {
		manager.insert(actual);
	}

	public String getObjectLabel() {
		return (String) p.get("object_label");
	}

	public String getSQLString() {
		return SQLString;
	}

	public CProperties getAttributes() {
		return (CProperties)p.get("attributes");
	}
	public CProperties getCProperties() {
		return p;
	}

	public void delete(CProperties keys) throws SQLException {
		manager.delete(keys);
	}

	public void update(CProperties keys, CDataObject actual) throws SQLException {
		manager.update(keys,actual);
	}

}
