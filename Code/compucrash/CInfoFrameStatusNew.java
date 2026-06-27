package compucrash;

import java.sql.SQLException;

public class CInfoFrameStatusNew extends CInfoFrameStatus {

    public CInfoFrameStatusNew(CInfoFrame owner) {
        super(owner);
        entry();
    }

    public void entry() {
        owner.setTitle("New");
        // get data
        // keys can be modified
        // all buttons active
        CProperties pAttributes = owner.dataObj.getAttributes();
        owner.bOk.setEnabled(true);
        owner.bApply.setEnabled(true);
        owner.bCancel.setEnabled(true);
        CMessage.print(pAttributes);
        for (int i = 0; i < owner.cFields.size(); i++) {
            owner.cFields.get(i).setEditable(true);
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i + 1));
            if (pA.get("init") != null) {
                owner.cFields.get(i).setValue(CDataManager.getInstance().getInit((String) pA.get("init")));
            }
        }
    }

    public void exit() {
        // no action required
    }

    public void action() {
        // no activity
    }

    public void apply() throws SQLException {
        applyOnly();
        // set new key
        // save data and change to EDIT
        CProperties pAttributes = owner.dataObj.getAttributes();
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
        }
        owner.p.put("keys", keys);
        for (int k = 0; k < owner.cFields.size(); k++) {
            owner.cFields.get(k).refresh();
        }
        owner.status = new CInfoFrameStatusEdit(owner);
    }

    public void ok() throws SQLException {
        // save data
        applyOnly();
        // exit dialog
        owner.dispose();
    }

    private void applyOnly() throws SQLException {
        Object[] o = new Object[owner.cFields.size()];
        for (int i = 0; i < owner.cFields.size(); i++) {
            o[i] = owner.cFields.get(i).getValue();
        }
        actual = new CDataObject(o);
        owner.dataObj.insert(actual);
    }
}
