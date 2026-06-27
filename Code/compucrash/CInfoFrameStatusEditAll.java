package compucrash;

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
        owner.setTitle("Edit");
        // get data
        // set keys uneditable
        // all buttons active
        owner.bOk.setEnabled(true);
        owner.bApply.setEnabled(true);
        owner.bCancel.setEnabled(true);
        for (int i = 0; i < owner.cFields.size(); i++) {
            owner.cFields.get(i).setEditable(true);
        }
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
        for (int k = 0; k < owner.cFields.size(); k++) {
            CProperties pValue = owner.cFields.get(k).getProperties();
            if (pValue.get("column_name").toString().equalsIgnoreCase(pA.get("column_name").toString())
                    && pValue.get("table_name").toString().equalsIgnoreCase(pA.get("table_name").toString())
                    && pValue.get("owner").toString().equalsIgnoreCase(pA.get("owner").toString())) {
                pKey.put("value", owner.cFields.get(k).getValue());
                break;
            }
        }
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
                errorString += owner.cFields.get(i).getLabel() +
                        ": Original > " + originalString +
                        ", Ge�ndert > " + actualString +
                        ", Gespeichert > " + compareString + "\n";
            }
        }
        int returnValue;
        Object[] options = {"Weiter", "Abbrechen"};
        if (!errorString.isEmpty()) {
            returnValue = JOptionPane.showOptionDialog(null, "Achtung, die Daten wurden ver�ndert.\n" + errorString + "Wollen Sie die Daten wirklich l�schen?", "Warnung", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        } else {
//			returnValue = JOptionPane.showOptionDialog(null,"Wollen Sie die Daten wirklich �ndern?","Information",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE,null,options, options[0]);			
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
//		System.out.println(owner.p.get("keys"));
        owner.dataObj.update((CProperties) owner.p.get("keys"), actual);
        // ge�nderten Schl�ssel �bergeben
        CProperties pAttributes = owner.dataObj.getAttributes();
        owner.p.put("keys", collectUpdatedKeys(pAttributes));
        //Statuswechsel
        owner.status = new CInfoFrameStatusEditAll(owner);
    }


    public void ok() throws SQLException {
        // save data
        // exit dialog
        apply();
        owner.dispose();
    }
}
