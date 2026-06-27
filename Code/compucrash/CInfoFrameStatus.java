package compucrash;
import java.sql.SQLException;

public abstract class CInfoFrameStatus {

	public CInfoFrame owner;
	public CDataObject original;
	public CDataObject actual;
	public CDataObject compare;

	public CInfoFrameStatus(CInfoFrame owner){
		this.owner = owner;
		original = owner.dataObj.getCDataObject((CProperties)owner.p.get("keys"));
		actual = (CDataObject) original.clone();
		// die attribute dataObj, original und actual sollten evtl in den Status
		for (int i = 0; i < owner.cFields.size(); i++) {
			Object o = null;
			String val = "";
			if (actual.get(i+1) == null) {
				val = null;
			} else if (actual.get(i+1).getClass() == CNull.class){
				val = null;
			} else {
				val = actual.get(i+1).toString();
				o = actual.get(i+1);
			}
			((CDisplayField)owner.cFields.get(i)).setValue(o); 
		}
	}
	
	public abstract void entry();
	public abstract void exit();
	public abstract void action();
	public abstract void apply() throws SQLException;
	public abstract void ok() throws SQLException;

	public void rollback() {
		try {
			System.out.println("rollback");
			CDataManager.getInstance().getConnection().rollback();
		} catch (SQLException e) {
			e.printStackTrace();
		}		
	}
}
