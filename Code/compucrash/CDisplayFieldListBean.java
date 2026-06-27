package compucrash;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CDisplayFieldListBean extends CDisplayFieldTableBean implements CSelectParent {

    private static final Logger LOGGER = Logger.getLogger(CDisplayFieldListBean.class.getName());

    private CSelectDialog selectDialog = null;
    private transient CInfoDataObject infObj = null;
    private CProperties pNew = null;
    private CProperties pInfObjAttributes = null;
    private int maxAttribut;
    private transient Object[] o;

    public CDisplayFieldListBean(CProperties p, CInfoFrame frame) {
        super(p, frame);

        CMessage.print("CDisplayFieldListBean.pTab:");
        CMessage.print(pTab);
    }

    private static Object parseDateAttributeValue(Object value, DateTimeFormatter fmt) {
        if (value instanceof LocalDate) {
            return value;
        } else if (value != null) {
            String text = value.toString().trim();
            if (text.isEmpty()) return null;
            try {
                return LocalDate.parse(text, fmt);
            } catch (DateTimeParseException _) {
                return LocalDate.parse(text);
            }
        }
        return null;
    }

    public void resetSelectDialog() {
        selectDialog = null;
    }

    protected void fieldBNew() {
        Component component = this;
        if (selectDialog != null) {
            selectDialog.setVisible(true);
            selectDialog.toFront();
            return;
        }
        pNew = getNewElement();
        infObj = CDataObjectFactory.getCInfoDataObject(pNew.get("objectName").toString());
        pInfObjAttributes = infObj.getAttributes();
        String pSelectField = null;
        for (int i = 1; i <= pInfObjAttributes.size(); i++) {
            if (infoKeys.get(Integer.toString(i)) == null) {
                pSelectField = (((CProperties) (pInfObjAttributes.get(Integer.toString(i)))).get("source")).toString();
                break;
            }
        }
        CListDataObject listObj = CDataObjectFactory.getCListDataObject(pSelectField);
        CMessage.print(listObj.getCProperties());

        int offsetX = tab.getX();
        int offsetY = tab.getY();
        while (component.getClass() != CInfoFrame.class) {
            Component parent = component.getParent();
            if (parent == null) {
                break;
            }
            component = parent;
            offsetX += component.getX();
            offsetY += component.getY();
        }
        selectDialog = new CSelectDialog(component, this, listObj);
        if (component instanceof CInfoFrame) {
            positionSelectDialog(component, offsetX, offsetY);
        }
    }

    private void positionSelectDialog(Component o, int offsetX, int offsetY) {
        try {
            ((CInfoFrame) o).getDesktopPane().add(selectDialog, JLayeredPane.MODAL_LAYER);
        } catch (Exception ex) {
            CMessage.print(ex);
        }
        int x = offsetX;
        if (x + selectDialog.getWidth() > o.getWidth()) {
            x = o.getWidth() - selectDialog.getWidth();
        }
        if (x < 0) {
            x = 0;
            selectDialog.setSize(o.getWidth(), selectDialog.getHeight());
        }
        int y = offsetY;
        if (y + selectDialog.getHeight() > o.getHeight() - 30) {
            y = o.getHeight() - 30 - selectDialog.getHeight();
        }
        if (y < 0) {
            y = 0;
            selectDialog.setSize(selectDialog.getWidth(), o.getHeight() - 30);
        }
        selectDialog.setBounds(x, y, selectDialog.getWidth(), selectDialog.getHeight());
    }

    public void insert(Object[] values) {
        // masseninsert in Objekt
        int size = pInfObjAttributes.size();
        for (int i = 1; i < pInfObjAttributes.size(); i++) {
            if (pInfObjAttributes.get(Integer.toString(i)) == null) {
                size = i;
                break;
            }
        }
        o = new Object[size];
        // keys setzen
        CProperties pSub = (CProperties) pNew.get("sub");
        maxAttribut = pSub.size();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        for (int i = 1; i <= pSub.size(); i++) {
            CProperties attribut = (CProperties) (pSub.get(Integer.toString(i)));
            if (((CProperties) (pInfObjAttributes.get(Integer.toString(i)))).get("data_type").toString().equalsIgnoreCase("DATE")) {
                o[i - 1] = parseDateAttributeValue(attribut.get("value"), fmt);
            } else {
                o[i - 1] = attribut.get("value");
            }
        }
        for (int i = 0; i < values.length; i++) {
            applyOnly(values[i]);
        }
        refresh();
    }

    private void applyOnly(Object value) {
        o[maxAttribut] = value;
        CDataObject actual = new CDataObject(o);
        try {
            infObj.insert(actual);
        } catch (SQLException _) {
            try {
                CDataManager.getInstance().getConnection().rollback();
            } catch (SQLException e1) {
                LOGGER.log(Level.SEVERE, "Failed to rollback after insert failure", e1);
            }
        }

    }

}
