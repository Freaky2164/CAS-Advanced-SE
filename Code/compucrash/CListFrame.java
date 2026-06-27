package compucrash;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;

import javax.swing.JScrollPane;

public class CListFrame extends CFrame implements CInfoParent, CListParent {

	protected CButton bNew;
	protected CButton bEdit;
	protected CButton bDelete;
	protected CButton bCopy;
	protected CButton bDisplay;
	protected CButton bCancel;
	private CListDataObject obj;
	protected CTable tab = new CTable();
	private JScrollPane sp = new JScrollPane(tab);
	private String objectName;
	protected CProperties p;
	private CAddonTableBean searchBean = null;
	
	public CListFrame(String objDesc, CFrame parent) {
		super(parent);
		this.objectName = objDesc;	
		this.name = objDesc + ".list";
		obj = CDataObjectFactory.getCListDataObject(objDesc);
		p = obj.getCProperties();
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
		bCopy = CButtonFactory.getButton("copy");
		bCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bCopy();
			}
		});
		bDisplay = CButtonFactory.getButton("display");
//		if (p.get("bdisplay") == null || p.get("bdisplay").toString().equalsIgnoreCase("")) {
		bDisplay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bDisplay();
			}
		});
		bCancel = CButtonFactory.getButton("cancel");
		bCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bCancel();
			}
		});
		
		addButton(bNew,p.get("bnew"));
		addButton(bEdit,p.get("bedit"));
		addButton(bDelete,p.get("bdelete"));
		addButton(bCopy,p.get("bcopy"));
		addButton(bDisplay,p.get("bdisplay"));
		
		getButtonPaneRight().add(bCancel);
		tab.setCListDataObject(obj);
		if (p.get("order") != null) {
			tab.setModel(obj.select(p));
		}else {
			tab.setModel(obj.select(1));
		}
		setStatusLine(Integer.toString(tab.getRowCount()) + " Einträge");
		tab.setListParent(this);
		tab.setWidth(obj.getCProperties());
		tab.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int clickCount = e.getClickCount();
				if (clickCount >= 2) {
					Object o = obj.getCProperties().get("default_button");
					if (o != null && o.toString().equalsIgnoreCase("bEdit")) {
						bEdit();
					} else {
						bDisplay();
					}
				}
			}
		});
		searchBean = new CSearchTableBean(tab, this);
		getMainPaneTopLeft().add(searchBean);
		
		getMainPane().add(sp);

		setTitle(obj.getObjectLabel() + " - Übersicht");
		
		if (p.get("color") != null) {
		    String color = p.get("color").toString();
		    int r = Integer.parseInt(color.substring(0,3));
		    int g = Integer.parseInt(color.substring(3,6));
		    int b = Integer.parseInt(color.substring(6));
			setColor(new Color(r,g,b));		    
		}
		addCustButtons();
//		pack();
		setFrameSize();
		CPropertyManager.getInstance().setDialog(name, this);
		setVisible(true);
	}
		
	public CAddonTableBean getSearchBean() {
	    return searchBean;
	}
	
	protected void addCustButtons() {
	    CProperties cbs = (CProperties)p.get("cust_buttons");
	    for (int i = 1; i <= cbs.size(); i++) {
	        CProperties cb = (CProperties)cbs.get(Integer.toString(i));
	        CButton button = CButtonFactory.getButton(cb.get("bez").toString());
	        getCustomButtonPane().add(button);
	        button.getCommand().setOwner(this);
	        button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    custButton(e);
                }       
	        });
	    }
	}
	
	public void custButton(ActionEvent e) {
	    ((CButton)e.getSource()).getCommand().execute(null);
	}
	
	public void setColor(Color c) {
	    super.setColor(c);
	    if (searchBean != null) searchBean.setColor(c);
	}

	private void addButton(CButton button, Object option) {
		if (option == null || option.toString() == null || option.toString().equalsIgnoreCase("")) {
			getButtonPaneLeft().add(button);
			return;
		}
		if (option.toString().equalsIgnoreCase("DISABLED")) {
			getButtonPaneLeft().add(button);
			button.setEnabled(false);
			return;
		}
		if (option.toString().equalsIgnoreCase("NONE")) {
			return;
		}
		getButtonPaneLeft().add(button);
	}
		
	public void refresh () {
		if (p.get("order") != null) {
			tab.setModel(obj.select(p));
			tab.setWidth(obj.getCProperties());
		}else {
			tab.setModel(obj.select(1));
			tab.setWidth(obj.getCProperties());
		}
	}

	protected void bCancel() {
		dispose();
	}

	protected void bDisplay() {
		if (p.get("bdisplay") == null || p.get("bdisplay").toString().equalsIgnoreCase("")) {
		    new CInfoFrame(CInfoFrame.DISPLAY, getSelectedElement(), this);
		} else if (!p.get("bdisplay").toString().equalsIgnoreCase("NONE") 
		        && !p.get("bdisplay").toString().equalsIgnoreCase("DISABLED")) {
				CCommand command;
                try {
                    command = (CCommand)Class.forName(p.get("bdisplay").toString()).newInstance();
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

	private CProperties getSelectedElement() {
		CProperties p = new CProperties();
		String key;
		p.put("object_name", objectName);
		p.put("keys",tab.getKeys());
		return p;
	}

	protected void bCopy() {
		if (p.get("bcopy") == null || p.get("bcopy").toString().equalsIgnoreCase("")) {
		new CInfoFrame(CInfoFrame.COPY, getSelectedElement(), this);		
		} else if (!p.get("bcopy").toString().equalsIgnoreCase("NONE") 
		        && !p.get("bcopy").toString().equalsIgnoreCase("DISABLED")) {
				CCommand command;
                try {
                    command = (CCommand)Class.forName(p.get("bcopy").toString()).newInstance();
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

	protected void bDelete() {
		if (p.get("bdelete") == null || p.get("bdelete").toString().equalsIgnoreCase("")) {
		new CInfoFrame(CInfoFrame.DELETE, getSelectedElement(), this);
		} else if (!p.get("bdelete").toString().equalsIgnoreCase("NONE") 
		        && !p.get("bdelete").toString().equalsIgnoreCase("DISABLED")) {
				CCommand command;
                try {
                    command = (CCommand)Class.forName(p.get("bdelete").toString()).newInstance();
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

	public void bEdit() {
		if (p.get("bedit") == null || p.get("bedit").toString().equalsIgnoreCase("")) {
		new CInfoFrame(CInfoFrame.EDITALL, getSelectedElement(), this);
		} else if (!p.get("bedit").toString().equalsIgnoreCase("NONE") 
		        && !p.get("bedit").toString().equalsIgnoreCase("DISABLED")) {
				CCommand command;
                try {
                    command = (CCommand)Class.forName(p.get("bedit").toString()).newInstance();
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
		if (p.get("bnew") == null || p.get("bnew").toString().equalsIgnoreCase("")) {
		new CInfoFrame(CInfoFrame.NEW, getNewElement(), this);
		} else if (!p.get("bnew").toString().equalsIgnoreCase("NONE") 
		        && !p.get("bnew").toString().equalsIgnoreCase("DISABLED")) {
				CCommand command;
                try {
                    command = (CCommand)Class.forName(p.get("bnew").toString()).newInstance();
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

	protected CProperties getNewElement() {
		CProperties p = new CProperties();
		p.put("object_name", objectName);
//		p.put("keys",null);
		return p;
	}
	
	public void dispose() {
		try {
			((CTableModel)tab.getModel()).getStatement().close();
		} catch (SQLException e) {
//			e.printStackTrace();
		}
        CPropertyManager.getInstance().setDialog(name,null);
		super.dispose();
	}
}
