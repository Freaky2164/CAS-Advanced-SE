package compucrash;

import java.util.Hashtable;

public class CProperties extends Hashtable<Object, Object> {

    public CProperties() {
        super();
    }

    public CProperties(CProperties source) {
        super();
        if (source == null) return;
        source.forEach((key, value) -> {
            Object copy = value instanceof CProperties ? new CProperties((CProperties) value) : value;
            put(key, copy);
        });
    }

    public static CProperties copyOf(CProperties source) {
        return new CProperties(source);
    }

    @Override
    public Object put(Object k, Object v) {
        if ((k == null) || (v == null)) return null;
        return super.put(k, v);
    }

}
