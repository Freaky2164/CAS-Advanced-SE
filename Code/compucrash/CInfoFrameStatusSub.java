package compucrash;

import java.sql.SQLException;

public class CInfoFrameStatusSub extends CInfoFrameStatus {

    public CInfoFrameStatusSub(CInfoFrame owner) {
        super(owner);
        entry();
    }

    public void entry() {
        getOwner().setTitle("New");
        // get data
        // keys can be modified
        // all buttons active
        CProperties pAttributes = getOwner().dataObj.getAttributes();
        getOwner().bOk.setEnabled(true);
        getOwner().bApply.setEnabled(true);
        getOwner().bCancel.setEnabled(true);
        CMessage.print(pAttributes);
        CMessage.print(getOwner().p);
        CProperties ps = (CProperties) getOwner().p.get("sub");
        for (int i = 0; i < getOwner().getcFields().size(); i++) {
            getOwner().getcFields().get(i).setEditable(true);
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i + 1));
            for (int j = 1; j <= ps.size(); j++) {
                CProperties psa = (CProperties) ps.get(Integer.toString(j));
                if (psa.get("column_name").toString().equalsIgnoreCase(pA.get("column_name").toString())
                        && psa.get("table_name").toString().equalsIgnoreCase(pA.get("table_name").toString())
                        && psa.get("owner").toString().equalsIgnoreCase(pA.get("owner").toString())) {
                    getOwner().getcFields().get(i).setEditable(false);
                    getOwner().getcFields().get(i).setValue(psa.get("value"));
                }
            }
            if (pA.get("init") != null) {
                getOwner().getcFields().get(i).setValue(CDataManager.getInstance().getInit((String) pA.get("init")));
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
        CInfoFrameStatusNew.attributeApplication(getOwner());
        getOwner().setStatus(new CInfoFrameStatusEdit(getOwner()));
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
