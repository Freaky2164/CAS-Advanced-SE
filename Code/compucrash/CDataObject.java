package compucrash;
import java.sql.Statement;
import java.util.Hashtable;

public class CDataObject extends Hashtable {
	
	public Statement stmt = null;

	public CDataObject(Object[] o) {
		super();
		if (o == null) return;
		for (int i = 0; i < o.length; i++) {
			if (o[i] == null) o[i] = new CNull();
			put(Integer.toString(i+1),o[i]);
		}
	}
	
	public CDataObject(Object[] o, Statement stmt) {
		this(o);
		this.stmt = stmt;
	}
	
	public Object get(int i) {
		if (containsKey(Integer.toString(i))) {
			if (get(Integer.toString(i)).getClass().equals(CNull.class)) return null;
			return get(Integer.toString(i));
		} else {
			return new CNull();
		}
	}
}