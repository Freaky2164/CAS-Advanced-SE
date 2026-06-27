package compucrash;
import java.util.Properties;

public class CButtonProperty extends Properties {
	
	public CButtonProperty () {
		super();
	}
	
	public Object put(Object k, Object v) {
		if ((k == null) | (v == null)) return null;
		return super.put(k, v);
	}

}
