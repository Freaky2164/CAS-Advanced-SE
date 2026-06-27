package compucrash;

import java.awt.Component;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.swing.JDesktopPane;

public class CDisplayFieldListBean extends CDisplayFieldTableBean implements CSelectParent {

    private CSelectDialog selectDialog = null;
    private CInfoDataObject infObj = null;
    private CProperties pNew = null;
    private CProperties p_infObjAttributes = null;
    private int maxAttribut;
    private Object[] o;
    
    public CDisplayFieldListBean(CProperties p, CInfoFrame frame) {
        super(p, frame);
        
        CMessage.print("CDisplayFieldListBean.pTab:");
        CMessage.print(pTab);
    }

	public void resetSelectDialog() {
	    selectDialog = null;
	}

	protected void bNew() {
		Component o = this;
		if (selectDialog instanceof CSelectDialog) {
			selectDialog.setVisible(true);
			selectDialog.toFront();
			return;
		}
		pNew = getNewElement();
		infObj = CDataObjectFactory.getCInfoDataObject(pNew.get("object_name").toString());
		p_infObjAttributes = infObj.getAttributes();
		String p_selectField = null;
		for (int i=1; i<= p_infObjAttributes.size(); i++) {
		    if(infoKeys.get(Integer.toString(i)) == null) {
		        p_selectField = (((CProperties)(p_infObjAttributes.get(Integer.toString(i)))).get("source")).toString();
		        break;
		    }
		}
		CListDataObject listObj = CDataObjectFactory.getCListDataObject(p_selectField);
		CMessage.print(listObj.getCProperties());
		
//		selectDialog = new CSelectDialog(o,this,listObj);
		int offsetX = tab.getX();
		int offsetY = tab.getY();
		while (o != null && o.getClass() != CInfoFrame.class) {
			o = o.getParent();
		    offsetX += o.getX();
		    offsetY += o.getY();
		}
			selectDialog = new CSelectDialog(o,this,listObj);
		if (o instanceof CInfoFrame) {
		    try {
		    ((CInfoFrame)o).getDesktopPane().add(selectDialog,JDesktopPane.MODAL_LAYER);
		    } catch (Exception ex) {
		    }
		    int x = offsetX;
		    if (x + selectDialog.getWidth() > o.getWidth()) {
		        x = o.getWidth() - selectDialog.getWidth();
		    }
		    if (x < 0) {
		        x = 0;
		        selectDialog.setSize(o.getWidth(),selectDialog.getHeight());
		    }
		    int y = offsetY;
		    if (y + selectDialog.getHeight() > o.getHeight() - 30) {
		        y = o.getHeight() - 30 - selectDialog.getHeight();
		    }
		    if (y < 0) {
		        y = 0;
		        selectDialog.setSize(selectDialog.getWidth(),o.getHeight() - 30);
		    }
		    selectDialog.setBounds(x,y,selectDialog.getWidth(),selectDialog.getHeight());
		}
	}

    public void insert(Object[] values) {
        // masseninsert in Objekt
        int size = p_infObjAttributes.size();
        for (int i = 1; i < p_infObjAttributes.size(); i++) {
            if (p_infObjAttributes.get(Integer.toString(i)) == null) {
                size = i;
                break;
            }
        }
        o = new Object[size];
        // keys setzen
        System.out.println(p_infObjAttributes);
        CProperties pSub = (CProperties) pNew.get("sub");
        maxAttribut = pSub.size();
        for (int i = 1; i <= pSub.size(); i++) {
            CProperties attribut = (CProperties)(pSub.get(Integer.toString(i)));
            if (((CProperties)(p_infObjAttributes.get(Integer.toString(i)))).get("data_type").toString().equalsIgnoreCase("DATE")) {
        		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        		Object value = attribut.get("value");
        		if (value instanceof LocalDate) {
        			o[i - 1] = value;
        		} else if (value != null) {
        			String text = value.toString().trim();
        			if (text.length() == 0) {
        				o[i - 1] = null;
        			} else {
        				try {
        					o[i - 1] = LocalDate.parse(text, fmt);
        				} catch (DateTimeParseException ex) {
        					o[i - 1] = LocalDate.parse(text);
        				}
        			}
        		} else {
        			o[i - 1] = null;
        		}
            } else {
                o[i-1] = attribut.get("value");
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
        } catch (SQLException e) {
            try {
                CDataManager.getInstance().getConnection().rollback();
                System.out.println("rollback");
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }

	} 

}
