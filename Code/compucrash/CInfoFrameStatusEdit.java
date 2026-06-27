package compucrash;
import java.sql.Blob;
import java.sql.SQLException;

import javax.swing.JOptionPane;

public class CInfoFrameStatusEdit extends CInfoFrameStatus {

	public CInfoFrameStatusEdit(CInfoFrame owner) {
		super(owner);
		entry();
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
			if (((CDisplayField) owner.cFields.get(i)).getProperties().get("iskey").toString().equalsIgnoreCase("0")) {
				((CDisplayField) owner.cFields.get(i)).setEditable(true);				
			} else {
				((CDisplayField) owner.cFields.get(i)).setEditable(false);
			}
		}
	}

	public void exit() {
		// no action required
	}

	public void action() {
		//no activity required
	}

	public void apply() throws SQLException {
		// save data
		// exchane status to itself
		String errorString = new String();
		compare = owner.dataObj.getCDataObjectForUpdate((CProperties)owner.p.get("keys"));
		Object[] o = new Object[original.size()];
		for (int i = 0; i < original.size(); i++) {
			o[i] = ((CDisplayField) owner.cFields.get(i)).getValue();
		}
		
		actual = new CDataObject(o);
		for (int i = 1; i <= original.size(); i++) {
			String originalString;
			String actualString;
			String compareString;
			if (original.get(i) == null) {
				originalString = new String();
			} else {
				originalString = original.get(i).toString();
			}
			if (actual.get(i) == null) {
				actualString = new String();
			} else {
				actualString = actual.get(i).toString();
			}
			if (compare.get(i) == null) {
				compareString = new String();
			} else {
				compareString = compare.get(i).toString();
			}
			if (!originalString.equals(actualString)) {
				if (!originalString.equals(compareString)) {
					// Data changed while dialog opened - lost update problem
					if (!(actual.get(i) instanceof Blob)) {
					errorString += ((CDisplayField) owner.cFields.get(i-1)).getLabel() + 
					": Original > " + originalString + 
					", Geändert > " + actualString + 
					", Gespeichert > " + compareString + "\n";
					}
				} 
			} else {
			}
		}
		int returnValue;
		Object[] options = {"Weiter", "Abbrechen"};
		if (errorString.length() > 0) {
			returnValue = JOptionPane.showOptionDialog(null,"Achtung, die Daten wurden verändert.\n" + errorString + "Wollen Sie die Daten wirklich löschen?","Warnung",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,options, options[0]);			
		} else {
			returnValue = JOptionPane.OK_OPTION;
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
		owner.dataObj.update((CProperties)owner.p.get("keys"), actual);
		//Statuswechsel
		owner.status = new CInfoFrameStatusEdit(owner);
	}


	public void ok() throws SQLException {
		// save data
		// exit dialog	
		apply();
		owner.dispose();	
	}
}
