package compucrash;
import java.sql.SQLException;

import javax.swing.JOptionPane;

public class CInfoFrameStatusEditAll extends CInfoFrameStatus {

	public CInfoFrameStatusEditAll(CInfoFrame owner) {
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
			((CDisplayField) owner.cFields.get(i)).setEditable(true);				
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
					errorString += ((CDisplayField) owner.cFields.get(i)).getLabel() + 
					": Original > " + originalString + 
					", Geändert > " + actualString + 
					", Gespeichert > " + compareString + "\n";
				} 
			} else {
//				actual.remove(Integer.toString(i));
			}
		}
		int returnValue;
		Object[] options = {"Weiter", "Abbrechen"};
		if (errorString.length() > 0) {
			returnValue = JOptionPane.showOptionDialog(null,"Achtung, die Daten wurden verändert.\n" + errorString + "Wollen Sie die Daten wirklich löschen?","Warnung",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,options, options[0]);			
		} else {
//			returnValue = JOptionPane.showOptionDialog(null,"Wollen Sie die Daten wirklich ändern?","Information",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE,null,options, options[0]);			
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
//		System.out.println(owner.p.get("keys"));
		owner.dataObj.update((CProperties)owner.p.get("keys"), actual);
		// geänderten Schlüssel übergeben
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
					CProperties pValue = ((CDisplayField)owner.cFields.get(k)).getProperties();
					if (pValue.get("column_name").toString().equalsIgnoreCase(pA.get("column_name").toString()) 
							&& pValue.get("table_name").toString().equalsIgnoreCase(pA.get("table_name").toString())
							&& pValue.get("owner").toString().equalsIgnoreCase(pA.get("owner").toString())) {
						pKey.put("value",((CDisplayField) owner.cFields.get(k)).getValue());
						break;
					}
				}				
			}
		}
		owner.p.put("keys",keys);
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
