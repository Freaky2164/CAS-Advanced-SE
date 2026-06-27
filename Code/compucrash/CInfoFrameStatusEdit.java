package compucrash;

import javax.swing.*;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.logging.Level;
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
        owner.setTitle("Edit");
        // get data
        // set keys uneditable
        // all buttons active
        owner.bOk.setEnabled(true);
        owner.bApply.setEnabled(true);
        owner.bCancel.setEnabled(true);
        for (int i = 0; i < owner.cFields.size(); i++) {
            owner.cFields.get(i).setEditable(owner.cFields.get(i).getProperties().get("iskey").toString().equalsIgnoreCase("0"));
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
        String errorString = "";
        compare = owner.dataObj.getCDataObjectForUpdate((CProperties) owner.p.get("keys"));
        Object[] o = new Object[original.size()];
        for (int i = 0; i < original.size(); i++) {
            o[i] = owner.cFields.get(i).getValue();
        }

        actual = new CDataObject(o);
        for (int i = 1; i <= original.size(); i++) {
            String originalString = toStringOrEmpty(original.get(i));
            String actualString = toStringOrEmpty(actual.get(i));
            String compareString = toStringOrEmpty(compare.get(i));
            if (!originalString.equals(actualString) && !originalString.equals(compareString)) {
                // Data changed while dialog opened - lost update problem
                if (!(actual.get(i) instanceof Blob)) {
                    errorString += owner.cFields.get(i - 1).getLabel() +
                            ": Original > " + originalString +
                            ", Ge\u00e4ndert > " + actualString +
                            ", Gespeichert > " + compareString + "\n";
                }
            }
        }
        int returnValue;
        Object[] options = {"Weiter", "Abbrechen"};
        if (!errorString.isEmpty()) {
            returnValue = JOptionPane.showOptionDialog(null, "Achtung, die Daten wurden ver�ndert.\n" + errorString + "Wollen Sie die Daten wirklich l�schen?", "Warnung", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        } else {
            returnValue = JOptionPane.OK_OPTION;
        }
        if (returnValue != JOptionPane.OK_OPTION) {
            try {
                CDataManager.getInstance().getConnection().rollback();
                CMessage.print("rollback");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to rollback transaction", e);
            }
            return;
        }
        owner.dataObj.update((CProperties) owner.p.get("keys"), actual);
        //Statuswechsel
        owner.status = new CInfoFrameStatusEdit(owner);
    }


    public void ok() throws SQLException {
        // save data
        // exit dialog
        apply();
        owner.dispose();
    }
}
