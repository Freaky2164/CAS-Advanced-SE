package compucrash;

import com.enterprisedt.net.ftp.FTPClientInterface;

import javax.swing.*;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CInfoFrameStatusEditAll extends CInfoFrameStatus {

    private static final Logger LOGGER = Logger.getLogger(CInfoFrameStatusEditAll.class.getName());

    public CInfoFrameStatusEditAll(CInfoFrame owner) {
        super(owner);
        entry();
    }

    private static String toStringOrEmpty(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    public void entry() {
        CInfoFrame owner = getOwner();
        owner.setTitle("Edit");
        // get data
        // set keys uneditable
        // all buttons active
        owner.bOk.setEnabled(true);
        owner.bApply.setEnabled(true);
        owner.bCancel.setEnabled(true);
        for (int i = 0; i < owner.getcFields().size(); i++) {
            owner.getcFields().get(i).setEditable(true);
        }
        setOwner(owner);
    }

    public void exit() {
        // no action required
    }

    public void action() {
        //no activity required
    }

    private CProperties collectUpdatedKeys(CProperties pAttributes) {
        int j = 0;
        CProperties keys = new CProperties();
        for (int i = 1; i <= pAttributes.size(); i++) {
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
            if (!pA.get("iskey").toString().equalsIgnoreCase("0")) {
                CProperties pKey = new CProperties();
                j++;
                keys.put(Integer.toString(j), pKey);
                pKey.put("owner", pA.get("owner"));
                pKey.put("table_name", pA.get("table_name"));
                pKey.put("column_name", pA.get("column_name"));
                findAndSetKeyValue(pKey, pA);
            }
        }
        return keys;
    }

    private void findAndSetKeyValue(CProperties pKey, CProperties pA) {
        CFrame owner = getOwner();
        for (int k = 0; k < owner.getcFields().size(); k++) {
            CProperties pValue = owner.getcFields().get(k).getProperties();
            if (pValue.get("column_name").toString().equalsIgnoreCase(pA.get("column_name").toString())
                    && pValue.get("table_name").toString().equalsIgnoreCase(pA.get("table_name").toString())
                    && pValue.get("owner").toString().equalsIgnoreCase(pA.get("owner").toString())) {
                pKey.put("value", owner.getcFields().get(k).getValue());
                break;
            }
        }
    }

    public void apply() throws SQLException {
        // save data
        // exchane status to itself
        StringBuilder errorString = new StringBuilder();
        CInfoFrame owner = getOwner();
        CDataObject compare = owner.dataObj.getCDataObjectForUpdate((CProperties) owner.p.get("keys"));
        CDataObject original = getOriginal();
        Object[] o = new Object[original.size()];
        for (int i = 0; i < original.size(); i++) {
            o[i] = owner.getcFields().get(i).getValue();
        }
        setActual(new CDataObject(o));
        for (int i = 1; i <= original.size(); i++) {
            String originalString = toStringOrEmpty(original.get(i));
            String actualString = toStringOrEmpty(getActual().get(i));
            String compareString = toStringOrEmpty(compare.get(i));
            if (!originalString.equals(actualString) && !originalString.equals(compareString)) {
                errorString.append(owner.getcFields().get(i).getLabel()).append(": Original > ").append(originalString).append(", Ge�ndert > ").append(actualString).append(", Gespeichert > ").append(compareString).append("\n");
            }
        }
        if (interactionReturner(errorString, owner, LOGGER, getActual())) return;
        CProperties pAttributes = owner.dataObj.getAttributes();
        owner.p.put("keys", collectUpdatedKeys(pAttributes));
        owner.setStatus(new CInfoFrameStatusEditAll(owner));
    }

    static boolean interactionReturner(StringBuilder errorString, CInfoFrame owner, Logger logger, CDataObject actual) throws SQLException {
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
                logger.log(Level.SEVERE, "Failed to rollback transaction", e);
            }
            return true;
        }
        owner.dataObj.update((CProperties) owner.p.get("keys"), actual);
        return false;
    }


    public void ok() throws SQLException {
        // save data
        // exit dialog
        apply();
        getOwner().dispose();
    }
}
