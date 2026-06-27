package compucrash;

import javax.swing.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CButtonFactory {

    private static final Map<String, CProperties> cButtons = new HashMap<>();

    static {
        try {
            ResultSet rset =
                    CDataManager.getInstance().getButtons();
            while (rset.next()) {
                CProperties cbp = new CProperties();
                cbp.put("bez", rset.getString(2));
                cbp.put("label", rset.getString(3));
                cbp.put("icon", new ImageIcon(rset.getString(4)));
                cbp.put("position", Integer.valueOf(rset.getInt(5)));
                cbp.put("tooltip", rset.getString(6));
                cbp.put("command", rset.getString(7));
                cbp.put("mnemonic", rset.getString(8));
                cButtons.put(cbp.get("bez").toString(), cbp);
            }
        } catch (SQLException e) {
            CMessage.print(e);
        }
    }

    private CButtonFactory() {
    }

    public static CButton getButton(String bez) {
        if (cButtons.get(bez) == null) {
            CProperties cbp = new CProperties();
            cbp.put("bez", bez);
            cbp.put("label", bez);
            cbp.put("icon", new ImageIcon());
            cbp.put("position", Integer.valueOf(0));
            cbp.put("tooltip", null);
            cbp.put("command", null);
            return new CButton(cbp);
        }
        return new CButton(cButtons.get(bez));
    }

    public CProperties getCButtonProperty(String bez) {
        return cButtons.get(bez);
    }
}
