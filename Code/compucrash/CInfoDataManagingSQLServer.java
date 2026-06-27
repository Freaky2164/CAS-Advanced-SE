package compucrash;

public class CInfoDataManagingSQLServer extends CInfoDataManagingDatabase {

	public CInfoDataManagingSQLServer(CInfoDataObject parent) {
		super(parent);
	}


/*	void begin() {
	    // unter autocommit.false ist das Begin implizit wie bei Oracle
	    // ansonsten muss alles ins statement
	}
		try {
			CDataManager.getInstance().getStatement().execute("begin transaction");
			System.out.println("begin transaction");
		} catch (SQLException e) {
			System.out.println("begin transaction failed");
			e.printStackTrace();
		}
	}*/
}
