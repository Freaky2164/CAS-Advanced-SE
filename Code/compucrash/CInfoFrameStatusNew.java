package compucrash;

import java.sql.SQLException;

public class CInfoFrameStatusNew extends CInfoFrameStatus {
    private static final String OWNERCONST = "owner";
    private static final String TABLE_NAME = "table_name";
    private static final String COLUMN_NAME = "column_name";
    public CInfoFrameStatusNew(CInfoFrame owner) {
        super(owner);
        entry();
    }

    public void entry() {
        CInfoFrame owner = getOwner();
        owner.setTitle("New");
        // get data
        // keys can be modified
        // all buttons active
        CProperties pAttributes = owner.dataObj.getAttributes();
        owner.bOk.setEnabled(true);
        owner.bApply.setEnabled(true);
        owner.bCancel.setEnabled(true);
        CMessage.print(pAttributes);
        for (int i = 0; i < owner.getcFields().size(); i++) {
            owner.getcFields().get(i).setEditable(true);
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i + 1));
            if (pA.get("init") != null) {
                owner.getcFields().get(i).setValue(CDataManager.getInstance().getInit((String) pA.get("init")));
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
        CInfoFrame owner = getOwner();

        attributeApplication(owner);
        for (int k = 0; k < owner.getcFields().size(); k++) {
            owner.getcFields().get(k).refresh();
        }
        owner.setStatus(new CInfoFrameStatusEdit(owner));
    }

    static void attributeApplication(CInfoFrame owner) {
        CProperties pAttributes = owner.dataObj.getAttributes();
        int j = 0;
        CProperties keys = new CProperties();
        for (int i = 1; i <= pAttributes.size(); i++) {
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
            if (!pA.get("iskey").toString().equalsIgnoreCase("0")) {
                CProperties pKey = new CProperties();
                j++;
                keys.put(Integer.toString(j), pKey);
                pKey.put(OWNERCONST, pA.get(OWNERCONST));
                pKey.put(TABLE_NAME, pA.get(TABLE_NAME));
                pKey.put(COLUMN_NAME, pA.get(COLUMN_NAME));
                for (int k = 0; k < owner.getcFields().size(); k++) {
                    CProperties pValue = owner.getcFields().get(k).getProperties();
                    if (pValue.get(COLUMN_NAME).toString().equalsIgnoreCase(pA.get(COLUMN_NAME).toString())
                            && pValue.get(TABLE_NAME).toString().equalsIgnoreCase(pA.get(TABLE_NAME).toString())
                            && pValue.get(OWNERCONST).toString().equalsIgnoreCase(pA.get(OWNERCONST).toString())) {
                        pKey.put("value", owner.getcFields().get(k).getValue());
                        break;
                    }
                }
            }
        }
        owner.p.put("keys", keys);
    }

    public void ok() throws SQLException {
        // save data
        applyOnly();
        // exit dialog
        getOwner().dispose();
    }

    private void applyOnly() throws SQLException {
        Object[] o = new Object[getOwner().getcFields().size()];
        for (int i = 0; i < getOwner().getcFields().size(); i++) {
            o[i] = getOwner().getcFields().get(i).getValue();
        }
        setActual(new CDataObject(o));
        getOwner().dataObj.insert(getActual());
    }
}
