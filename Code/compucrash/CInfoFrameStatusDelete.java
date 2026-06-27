package compucrash;
import java.sql.SQLException;

import javax.swing.JOptionPane;

public class CInfoFrameStatusDelete extends CInfoFrameStatus {

	public CInfoFrameStatusDelete(CInfoFrame owner) {
		super(owner);
		entry();
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
			((CDisplayField) owner.cFields.get(i)).setEditable(false);
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
		String errorString = new String();
		compare = owner.dataObj.getCDataObjectForUpdate((CProperties)owner.p.get("keys"));
		Object[] o = new Object[original.size()];
		for (int i = 0; i < original.size(); i++) {
			o[i] = ((CDisplayField) owner.cFields.get(i)).getValue();
		}
		actual = new CDataObject(o);
		for (int i = 0; i < original.size(); i++) {
			String originalString = null;
			if (original.get(i) != null) originalString = original.get(i).toString();
			String compareString = null;
			if (compare.get(i) != null) compareString = compare.get(i).toString();
			if (originalString == null) originalString = new String();
			if (compareString == null) compareString = new String();
			if (!originalString.equals(compareString)) {
				// Data changed while dialog opened - lost update problem
				errorString += ((CDisplayField) owner.cFields.get(i)).getLabel() + 
				": Original > " + originalString + 
				", Gespeichert > " + compareString + "\n";
			}
		}
		int returnValue;
		Object[] options = {"Weiter", "Abbrechen"};
		if (errorString.length() > 0) {
			returnValue = JOptionPane.showOptionDialog(null,"Achtung, die Daten wurden verändert.\n" + errorString + "Wollen Sie die Daten wirklich löschen?","Warnung",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,options, options[0]);			
		} else {
			returnValue = JOptionPane.showOptionDialog(null,"Wollen Sie die Daten wirklich löschen?","Information",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE,null,options, options[0]);			
		}
		if (returnValue != JOptionPane.OK_OPTION) {
			try {
				CDataManager.getInstance().getConnection().rollback();
				System.out.println("rollback");
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return;
		} 
		owner.dataObj.delete((CProperties)owner.p.get("keys"));
		// hier Übersicht aktualisieren
//		owner.dispose();
	}
}
