package compucrash;

import java.sql.Statement;
import java.util.HashMap;
import java.util.Objects;

public class CDataObject extends HashMap<String, Object> {

    private transient Statement stmt = null;

    public CDataObject(Object[] o) {
        super();
        if (o == null) return;
        for (int i = 0; i < o.length; i++) {
            if (o[i] == null) o[i] = new CNull();
            put(Integer.toString(i + 1), o[i]);
        }
    }

    public CDataObject(Object[] o, Statement stmt) {
        this(o);
        this.stmt = stmt;
    }

    public Object get(int i) {
        String key = Integer.toString(i);
        if (containsKey(key)) {
            Object value = super.get(key);
            if (value != null && value.getClass().equals(CNull.class)) return null;
            return value;
        } else {
            return new CNull();
        }
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }

    public Statement getStmt() {
        return stmt;
    }

    public void setStmt(Statement stmt) {
        this.stmt = stmt;
    }
}
