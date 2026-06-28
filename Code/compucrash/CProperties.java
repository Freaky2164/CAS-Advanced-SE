package compucrash;

import java.util.HashMap;

public class CProperties extends HashMap<Object, Object> {

    public CProperties() {
        super();
    }

    public CProperties(CProperties source) {
        super();
        if (source == null) return;
        source.forEach((_, value) -> {
            if (value instanceof CProperties properties) {
                this.putAll(properties);
            }
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
