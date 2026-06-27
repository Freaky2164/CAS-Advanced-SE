package compucrash;

public class CInfoFrameStatusCopy extends CInfoFrameStatusNew {

    public CInfoFrameStatusCopy(CInfoFrame owner) {
        super(owner);
        entry();
    }

    @Override
    public void entry() {
        CInfoFrame owner = getOwner();
        owner.setTitle("Copy");
        // get data
        // all fields editable
        // all buttons active
        owner.bOk.setEnabled(true);
        owner.bApply.setEnabled(true);
        owner.bCancel.setEnabled(true);
        for (int i = 0; i < owner.getcFields().size(); i++) {
            owner.getcFields().get(i).setEditable(true);
        }
    }

    @Override
    public void exit() {
        // no action required
    }

    @Override
    public void action() {
        // no activity requested
    }

    @Override
    public void apply() {
        // save data
        // change to EDIT
        CInfoFrame owner = getOwner();
        owner.p.put("key", owner.getcFields().getFirst().getValue());
        owner.setStatus(new CInfoFrameStatusEdit(owner));
    }

    @Override
    public void ok() {
        // save data
        // exit dialog
        getOwner().dispose();
    }
}
