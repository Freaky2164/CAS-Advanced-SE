package compucrash;

import javax.swing.*;

public class CExtendedTextField extends JTextField implements CSelectParent {

    public CExtendedTextField(int cols) {
        super(cols);
    }

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
}
