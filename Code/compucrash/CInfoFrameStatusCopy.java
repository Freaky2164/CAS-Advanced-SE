package compucrash;
public class CInfoFrameStatusCopy extends CInfoFrameStatusNew {

	public CInfoFrameStatusCopy(CInfoFrame owner) {
		super(owner);
		entry();
	}

	public void entry() {
		owner.setTitle("Copy");
		// get data
		// all fields editable
		// all buttons active
		owner.bOk.setEnabled(true);
		owner.bApply.setEnabled(true);
		owner.bCancel.setEnabled(true);
		for (int i = 0; i < owner.cFields.size(); i++) {
			((CDisplayField) owner.cFields.get(i)).setEditable(true);
		}
	}

	public void exit() {
		// no action required
	}

	public void action() {
		// no activity requested
	}

	public void apply() {
		// save data
		// change to EDIT
		owner.p.put("key",((CDisplayField) owner.cFields.get(0)).getValue());
		owner.status = new CInfoFrameStatusEdit(owner);
	}

	public void ok() {
		// save data
		// exit dialog
		owner.dispose();
	}
}
