package compucrash;

public class CInfoDataManagingOracle extends CInfoDataManagingDatabase {

    public CInfoDataManagingOracle(CInfoDataObject parent) {
        super(parent);
    }

    public void forUpdate() {
        // Oracle-specific FOR UPDATE locking is handled at the SQL query level
    }

}
