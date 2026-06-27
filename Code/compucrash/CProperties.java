package compucrash;
import java.util.Enumeration;
import java.util.Hashtable;

public class CProperties extends Hashtable implements Cloneable {

	public CProperties() {
		super();
	}
	
	public Object put(Object k, Object v) {
		if ((k == null) | (v == null)) return null;
		return super.put(k, v);
	}
	
	public Object clone() {
		CProperties clone = new CProperties();
//		enumeration durchgehen und dann clone an alle cproperties schicken.
		Enumeration e = this.keys();
		while (e.hasMoreElements()) {
			Object key = e.nextElement();
			Object value = this.get(key);
			if (value instanceof CProperties) {
				value = ((CProperties)value).clone();
			}
			clone.put(key, value);
		}
		return clone;
	}

}
