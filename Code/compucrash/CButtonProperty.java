package compucrash;

import java.util.Properties;

public class CButtonProperty extends Properties {

    public CButtonProperty() {
        super();
    }

    @Override
    public synchronized Object put(Object k, Object v) {
        if ((k == null) || (v == null)) return null;
        return super.put(k, v);
    }

}
