package compucrash;
import javax.swing.JTextField;
public class CExtendedTextField extends JTextField implements CSelectParent {

	public void setValue(Object o) {
		if (o == null) {
			this.setText(null);
			return;
		}
		this.setText(o.toString());
	}
	
	public void resetSelectDialog() {
//	    selectDialog = null;
	}

	public CExtendedTextField(int cols) {
		super(cols);
	}
}
