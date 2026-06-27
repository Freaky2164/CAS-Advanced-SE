package compucrash;

import javax.swing.*;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CInfoFrameStatusDelete extends CInfoFrameStatus {

    private static final Logger LOGGER = Logger.getLogger(CInfoFrameStatusDelete.class.getName());

    public CInfoFrameStatusDelete(CInfoFrame owner) {
        super(owner);
        entry();
    }

    private static String toStringOrEmpty(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    public void entry() {
        owner.setTitle("Delete");
        // get data
        // all fields uneditable
        // only ok button active
        owner.bOk.setEnabled(true);
        owner.bApply.setEnabled(false);
        owner.bCancel.setEnabled(true);
        for (int i = 0; i < owner.cFields.size(); i++) {
            owner.cFields.get(i).setEditable(false);
        }
    }

    public void exit() {
        // no action required
    }

    public void action() {
        // no activity required
    }

    public void ok() {
        // not available
    }

    public void apply() throws SQLException {
        // DELETE data
        // exit dialog
        String errorString = "";
        compare = owner.dataObj.getCDataObjectForUpdate((CProperties) owner.p.get("keys"));
        Object[] o = new Object[original.size()];
        for (int i = 0; i < original.size(); i++) {
            o[i] = owner.cFields.get(i).getValue();
        }
        actual = new CDataObject(o);
        for (int i = 0; i < original.size(); i++) {
            String originalString = toStringOrEmpty(original.get(i));
            String compareString = toStringOrEmpty(compare.get(i));
            if (!originalString.equals(compareString)) {
                // Data changed while dialog opened - lost update problem
                errorString += owner.cFields.get(i).getLabel() +
                        ": Original > " + originalString +
                        ", Gespeichert > " + compareString + "\n";
            }
        }
        int returnValue;
        Object[] options = {"Weiter", "Abbrechen"};
        if (!errorString.isEmpty()) {
            returnValue = JOptionPane.showOptionDialog(null, "Achtung, die Daten wurden ver�ndert.\n" + errorString + "Wollen Sie die Daten wirklich l�schen?", "Warnung", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        } else {
            returnValue = JOptionPane.showOptionDialog(null, "Wollen Sie die Daten wirklich l�schen?", "Information", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
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
        owner.dataObj.delete((CProperties) owner.p.get("keys"));
        // hier �bersicht aktualisieren
//		owner.dispose();
    }
}
