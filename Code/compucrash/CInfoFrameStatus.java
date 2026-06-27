package compucrash;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class CInfoFrameStatus {

    private static final Logger LOGGER = Logger.getLogger(CInfoFrameStatus.class.getName());

    private CInfoFrame owner;
    private CDataObject original;
    private CDataObject actual;
    private CDataObject compare;

    public CDataObject getActual() {
        return actual;
    }
    public CDataObject getOriginal() {
        return original;
    }
    public void setActual(CDataObject actual) {
        this.actual = actual;
    }
    public void setOriginal(CDataObject original) {
        this.original = original;
    }

    public CInfoFrameStatus(CInfoFrame owner) {
        this.owner = owner;
        original = owner.dataObj.getCDataObject((CProperties) owner.p.get("keys"));
        actual = (CDataObject) original.clone();
        // die attribute dataObj, original und actual sollten evtl in den Status
        for (int i = 0; i < owner.getcFields().size(); i++) {
            Object o = null;
            String val = "";
            if (actual.get(i + 1) == null) {
                val = null;
            } else if (actual.get(i + 1).getClass() == CNull.class) {
                val = null;
            } else {
                val = actual.get(i + 1).toString();
                o = actual.get(i + 1);
            }
            owner.getcFields().get(i).setValue(o);
        }
    }

    public abstract void entry();

    public abstract void exit();

    public abstract void action();

    public abstract void apply() throws SQLException;

    public abstract void ok() throws SQLException;

    public void rollback() {
        try {
            CDataManager.getInstance().getConnection().rollback();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to rollback transaction", e);
        }
    }

    public CInfoFrame getOwner() {
        return owner;
    }

    public void setOwner(CInfoFrame owner) {
        this.owner = owner;
    }

    public CDataObject getCompare() {
        return compare;
    }

    public void setCompare(CDataObject compare) {
        this.compare = compare;
    }
}
