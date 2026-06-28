package compucrash;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.logging.Logger;

public class CInfoFrameStatusEdit extends CInfoFrameStatus {

    private static final Logger LOGGER = Logger.getLogger(CInfoFrameStatusEdit.class.getName());

    public CInfoFrameStatusEdit(CInfoFrame owner) {
        super(owner);
        entry();
    }

    private static String toStringOrEmpty(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    public void entry() {
        CInfoFrame owner = getOwner();
        owner.setTitle("Edit");
        owner.bOk.setEnabled(true);
        owner.bApply.setEnabled(true);
        owner.bCancel.setEnabled(true);
        for (int i = 0; i < owner.getcFields().size(); i++) {
            owner.getcFields().get(i).setEditable(owner.getcFields().get(i).getProperties().get("iskey").toString().equalsIgnoreCase("0"));
        }
    }

    public void exit() {
        // no action required
    }

    public void action() {
        //no activity required
    }

    public void apply() throws SQLException {
        // save data
        // exchane status to itself
        StringBuilder errorString = new StringBuilder();
        setCompare(getOwner().dataObj.getCDataObjectForUpdate((CProperties) getOwner().p.get("keys")));
        Object[] o = new Object[getOriginal().size()];
        for (int i = 0; i < getOriginal().size(); i++) {
            o[i] = getOwner().getcFields().get(i).getValue();
        }

        setActual(new CDataObject(o));
        CDataObject actual = getActual();
        for (int i = 1; i <= getOriginal().size(); i++) {
            String originalString = toStringOrEmpty(getOriginal().get(i));
            String actualString = toStringOrEmpty(actual.get(i));
            String compareString = toStringOrEmpty(getCompare().get(i));
            if (!originalString.equals(actualString) && !originalString.equals(compareString) && !(actual.get(i) instanceof Blob)) {
                errorString.append(getOwner().getcFields().get(i - 1).getLabel()).append(": Original > ").append(originalString).append(", Geändert > ").append(actualString).append(", Gespeichert > ").append(compareString).append("\n");
            }

        }
        CInfoFrame owner = getOwner();
        if (CInfoFrameStatusEditAll.interactionReturner(errorString, owner, LOGGER, actual)) return;
        //Statuswechsel
        owner.setStatus(new CInfoFrameStatusEdit(owner));
    }


    public void ok() throws SQLException {
        // save data
        // exit dialog
        apply();
        CInfoFrame owner = getOwner();
        owner.dispose();
    }
}
