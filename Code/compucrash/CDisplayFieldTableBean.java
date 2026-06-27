
package compucrash;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class CDisplayFieldTableBean extends CDisplayFieldBean implements CInfoParent {

	private JScrollPane sp;
	protected CTable tab;
	protected CButton bNew;
	protected CButton bEdit;
	protected CButton bDelete;
	protected CListDataObject objList;
	protected CInfoDataObject objInfo;
	protected CDataObject obj;
	protected CProperties pTab;
	protected CProperties infoKeys;
	private JPanel p2 = new JPanel();
	protected CProperties pb = null;
    private JPanel p1;

	public CDisplayFieldTableBean(CProperties p, CInfoFrame frame) {
	    super(p, frame);
		this.objInfo = frame.dataObj;
		initKeys();
		System.out.println("CDisplayFieldTableBean.infoKeys:" + infoKeys);
		pb = objList.getCProperties();
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createEtchedBorder(), (String) p.get("label")));
		bNew = CButtonFactory.getButton("new");
			bNew.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					bNew();
				}
			});
		bEdit = CButtonFactory.getButton("edit");
		bEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bEdit();
			}
		});
		bDelete = CButtonFactory.getButton("delete");
		bDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bDelete();
			}
		});
		tab = new CTable();
		int width = Integer.parseInt((String) p.get("data_scale")) * 15;
		int height = Integer.parseInt((String) p.get("data_height")) * 20;
		sp = new JScrollPane(tab);
		sp.setPreferredSize(new Dimension(width, height));
		add(sp, BorderLayout.CENTER);
		p1 = new JPanel();
		add(p1, BorderLayout.EAST);
		p1.setLayout(new BorderLayout());
		p1.add(p2, BorderLayout.NORTH);
		p2.setLayout(new GridLayout(0, 1));
		addButton(bNew,pb.get("bnew"));
		addButton(bEdit,pb.get("bedit"));
		addButton(bDelete,pb.get("bdelete"));
		if (infoKeys.size() == 0 ) {
			return;
		}
		pTab = new CProperties();
		CProperties po = new CProperties();
		if (objList.getCProperties().get("order") == null) {
		pTab.put("order", po);
		po.put("1", "1");
		} else {
			pTab.put("order", objList.getCProperties().get("order"));
		}
		CProperties pf = new CProperties();
		pTab.put("filter_and", infoKeys);
		CProperties pe = new CProperties();
		pTab.put("exclude", infoKeys);
		tab.setModel(objList.select(pTab));
		//		tab.setParent(this);
		tab.setWidth(objList.getCProperties());
		tab.exclude(1);
		tab.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int clickCount = e.getClickCount();
				if (clickCount >= 2) {
					Object o = objList.getCProperties().get("default_button");
					if (o != null && o.toString().equalsIgnoreCase("bEdit")) {
						bEdit();
					} else if (o != null && o.toString().equalsIgnoreCase("bDelete")){
						bDelete();
					} else {
						bDisplay();					    
					}
				}
			}
		});
//		lostFocus(); unnötig
	}
	
	public void setEditedColor() {
	}

	public void resetEditedColor() {
	}	

	private void initKeys() {
		if ((CProperties) frame.p.get("keys") != null ) {
			infoKeys = (CProperties)((CProperties) frame.p.get("keys")).clone();
		} else {
			infoKeys = new CProperties();
		}
		objList = CDataObjectFactory.getCListDataObject(p.get("source").toString());
		CProperties paList = (CProperties) objList.getCProperties().get("attributes");
		
		for (int i = 1; i <= infoKeys.size(); i++){
			CProperties pak = (CProperties)infoKeys.get(Integer.toString(i));
			for (int j = 1; j <= paList.size(); j++) {
				CProperties pal = (CProperties)paList.get(Integer.toString(j));
				if (pal.get("column_name").toString().equalsIgnoreCase(pak.get("column_name").toString())) {
					((CProperties)infoKeys.get(Integer.toString(i))).put("owner",pal.get("owner"));			
					((CProperties)infoKeys.get(Integer.toString(i))).put("table_name",pal.get("table_name"));
					((CProperties)infoKeys.get(Integer.toString(i))).put("operator","=");	
				}
			}
		}

	}
	protected void bDelete() {
		if (pb.get("bdelete") == null || pb.get("bdelete").toString().equalsIgnoreCase("")) {
		    new CInfoFrame(CInfoFrame.DELETE, getSelectedElement(), this);
		} else if (!pb.get("bdelete").toString().equalsIgnoreCase("NONE") 
		        && !pb.get("bdelete").toString().equalsIgnoreCase("DISABLED")) {
				CCommand command;
                try {
                    command = (CCommand)Class.forName(pb.get("bdelete").toString()).newInstance();
                    command.setOwner(this);
                    command.execute(null);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                   e.printStackTrace();
                }	    
		}
	}

	protected void bEdit() {
		if (pb.get("bedit") == null || pb.get("bedit").toString().equalsIgnoreCase("")) {
		    new CInfoFrame(CInfoFrame.EDIT, getSelectedElement(), this);
		} else if (!pb.get("bedit").toString().equalsIgnoreCase("NONE") 
		        && !pb.get("bedit").toString().equalsIgnoreCase("DISABLED")) {
				CCommand command;
                try {
                    command = (CCommand)Class.forName(pb.get("bedit").toString()).newInstance();
                    command.setOwner(this);
                    command.execute(null);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                   e.printStackTrace();
                }	    
		}
	}

	protected void bNew() {
		if (pb.get("bnew") == null || pb.get("bnew").toString().equalsIgnoreCase("")) {
		    new CInfoFrame(CInfoFrame.SUB, getNewElement(), this);
		} else if (!pb.get("bnew").toString().equalsIgnoreCase("NONE") 
		        && !pb.get("bnew").toString().equalsIgnoreCase("DISABLED")) {
				CCommand command;
                try {
                    command = (CCommand)Class.forName(pb.get("bnew").toString()).newInstance();
                    command.setOwner(this);
                    command.execute(null);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                   e.printStackTrace();
                }	    
		}
	}
	
	protected void bDisplay() {
		if (pb.get("bdisplay") == null || pb.get("bdisplay").toString().equalsIgnoreCase("")) {
		 new CInfoFrame(CInfoFrame.DISPLAY, getSelectedElement(), this);
		} else if (!pb.get("bdisplay").toString().equalsIgnoreCase("NONE") 
		        && !pb.get("bdisplay").toString().equalsIgnoreCase("DISABLED")) {
				CCommand command;
                try {
                    command = (CCommand)Class.forName(pb.get("bdisplay").toString()).newInstance();
                    command.setOwner(this);
                    command.execute(null);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                   e.printStackTrace();
                }	    
		}
	}

	public CProperties getSelectedElement() {
		CProperties p = new CProperties();
		p.put("object_name", objList.getCProperties().get("object_name").toString());
		p.put("keys", tab.getKeys());
		return p;
	}

	public Component getTextField() {
	    return null;
	}

	public CProperties getNewElement() {
		CProperties paList = (CProperties) objList.getCProperties().get("attributes");
		CProperties p = new CProperties();
		p.put("object_name", objList.getCProperties().get("object_name").toString());
		CProperties ps = new CProperties();
		p.put("sub", ps);
		int k = 0;
		for (int i = 1; i <= infoKeys.size(); i++) {
			CProperties pa = (CProperties) infoKeys.get(Integer.toString(i));
			for (int j = 1; j <= paList.size(); j++) {
				CProperties pal = (CProperties) paList.get(Integer.toString(i));
				if (pa.get("column_name").toString().equalsIgnoreCase(pal.get("column_name").toString())) {
					k++;
					CProperties psa = new CProperties();
					ps.put(Integer.toString(k), psa);
					psa.put("owner", pal.get("owner"));
					psa.put("table_name", pal.get("table_name"));
					psa.put("column_name", pal.get("column_name"));
					psa.put("value", pa.get("value"));
					break;
				}
			}
		}
		return p;
	}

	public void setColor(Color c) {
        super.setColor(c);
//        if (bNew != null) bNew.setBackground(c);
//        if (bEdit != null) bEdit.setBackground(c);
//        if (bDelete != null) bDelete.setBackground(c);
        p1.setBackground(c);
        p2.setBackground(c);
    }
	public String getText() {
		return null;
	}

	public void setValue(Object o) {
	}

	public void setEditable(int i) {
		boolean bool = false;
		if (i == 1) bool = true;
		initKeys();
		if (infoKeys.size() == 0) bool = false;
		bNew.setEnabled(bool);
		bEdit.setEnabled(bool);
		bDelete.setEnabled(bool);
		if (infoKeys.size() == 0 ) {
			return;
		}
		pTab = new CProperties();
		CProperties po = new CProperties();
		if (objList.getCProperties().get("order") == null) {
		pTab.put("order", po);
		po.put("1", "1");
		} else {
			pTab.put("order", objList.getCProperties().get("order"));
		}
		CProperties pf = new CProperties();
		pTab.put("filter_and", infoKeys);
		CProperties pe = new CProperties();
		pTab.put("exclude", infoKeys);
		tab.setModel(objList.select(pTab));
		//		tab.setParent(this);
		tab.setWidth(objList.getCProperties());
		tab.exclude(1);
		if (tab.getMouseListeners().length == 3) {
			tab.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					int clickCount = e.getClickCount();
					if (clickCount >= 2) {
						Object o = objList.getCProperties().get("default_button");
						if (o != null && o.toString().equalsIgnoreCase("bEdit")) {
							bEdit();
						} else if (o != null && o.toString().equalsIgnoreCase("bDelete")){
							bDelete();
						} else {
							bDisplay();					    
						}
					}
				}
			});
		}

	}
	
	public void refresh () {
	    if (pTab == null) return;
		tab.setModel(objList.select(pTab));
		tab.setWidth(objList.getCProperties());
		// TODO exclude in Tabelle mit Properties steuern
		tab.exclude(1);
		lostFocus();
	}
	
	public Object getValue() {
		return null;
	}

	private void addButton(CButton button, Object option) {
		if (option == null || option.toString().equalsIgnoreCase("")) {
			p2.add(button);
			return;
		}
		if (option.toString().equalsIgnoreCase("DISABLED")) {
			p2.add(button);
			button.setEnabled(false);
			return;
		}
		if (option.toString().equalsIgnoreCase("NONE")) {
			return;
		}
		p2.add(button);
	}

}