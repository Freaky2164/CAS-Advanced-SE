package compucrash;

public class CInfoFrameStatusDisplay extends CInfoFrameStatus {

    public CInfoFrameStatusDisplay(CInfoFrame owner) {
        super(owner);
        entry();
    }

    public void entry() {
        CInfoFrame owner = getOwner();
        owner.setTitle("Display");
        // get data
        // set all fields uneditable
        // set only cancel button to active
        owner.bOk.setEnabled(false);
        owner.bApply.setEnabled(false);
        owner.bCancel.setEnabled(true);
        for (int i = 0; i < owner.getcFields().size(); i++) {
            owner.getcFields().get(i).setEditable(false);
        }
    }

    public void exit() {
        // no action required
    }

    public void action() {
        // no activity required
    }

    public void apply() {
        // not used
    }

    public void ok() {
        // exit dialog
        getOwner().dispose();
    }

}
