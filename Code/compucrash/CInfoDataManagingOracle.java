package compucrash;

public class CInfoDataManagingOracle extends CInfoDataManagingDatabase {

	private String SQLObjectString;

    public CInfoDataManagingOracle(CInfoDataObject parent) {
		super(parent);
	}
	
	public void forUpdate() {
		SQLObjectString += " FOR UPDATE";
	}

}
