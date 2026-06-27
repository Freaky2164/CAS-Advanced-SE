package compucrash;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

public class CInfoFrame extends CFrame {

	public CInfoFrameStatus status;
	protected CButton bOk;
	protected CButton bApply;
	protected CButton bCancel;
	protected CProperties p;
	protected CInfoDataObject dataObj;
	
	public static final int NEW = 0;
	public static final int EDIT = 1;
	public static final int DISPLAY = 2;
	public static final int COPY = 3;
	public static final int DELETE = 4;
	public static final int SUB = 5;
	public static final int EDITALL = 6;
	
	public Vector cFields = new Vector();
	public CTabbedPane tabbedPane = new CTabbedPane();
	private CInfoParent parent;
	public CProperties attributeValues = new CProperties();
    private CProperties pc;
    protected String object_name;
    protected boolean edited = false;
    protected CProperties attributeActions = new CProperties();
    private CCommand c;

    private CProperties papply = new CProperties();
    private CProperties pok = new CProperties();

	public CInfoFrame(int modus, CProperties p, CInfoParent parent) throws HeadlessException {
		super(null);
		this.parent = parent;
		this.p = p;

		this.object_name = p.get("object_name").toString();
		this.name =  this.object_name + ".info";
		dataObj = CDataObjectFactory.getCInfoDataObject(this.object_name);
	    papply.put("ok", "0");
	    pok.put("ok", "1");
		bOk = CButtonFactory.getButton("ok");
		bApply = CButtonFactory.getButton("apply");
		if (dataObj.getCProperties().get("apply") == null) {
			bOk.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ok();
				}
			});
			bApply.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					apply();
				}
			});
		} else {
		    try {
                c = (CCommand)Class.forName(dataObj.getCProperties().get("apply").toString()).newInstance();
                c.setOwner(this);
    			bOk.addActionListener(new ActionListener() {
    				public void actionPerformed(ActionEvent e) {
    					c_execute(pok);
    				}
    			});
    			bApply.addActionListener(new ActionListener() {
    				public void actionPerformed(ActionEvent e) {
    					c_execute(papply);
    				}
    			});
            } catch (InstantiationException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IllegalAccessException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (ClassNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
		}
		bCancel = CButtonFactory.getButton("cancel");
		bCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
		getButtonPaneLeft().add(bOk);
		getButtonPaneLeft().add(bApply);
		getButtonPaneRight().add(bCancel);
		CProperties pAttributes = dataObj.getAttributes();
		JScrollPane sp = new JScrollPane();
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		Object o = CPropertyManager.getInstance().getGlobal("SEARCH_IN_INFO");
		if (o != null && ((Boolean)o).booleanValue()) {
		    getContentPane().add(new CInfoSearchBean(this), BorderLayout.NORTH);
		}
		for (int i = 1; i <= pAttributes.size(); i++) {
		    CProperties aProp = (CProperties) pAttributes.get(Integer.toString(i));
			CDisplayField field = new CDisplayField(aProp, this);
			cFields.add(field);
			String name = aProp.get("owner") + "." +
					aProp.get("table_name") + "." +
					aProp.get("column_name");
			attributeValues.put(name,"init");
			String actionCommand = (String)aProp.get("action");
			if (actionCommand != null) {
			    try {
				    attributeActions.put(name,Class.forName(actionCommand).newInstance());
				    CMessage.print("ActionCommand gefunden für");
				    CMessage.print(name);
				    CMessage.print(attributeActions.get(name));
			    } catch (Exception e) {
			        // nichts zu tun
			        CMessage.print("Fehler beim Erzeugen des ActionCommands");
			    }
			}
			c.gridwidth = GridBagConstraints.REMAINDER;
			if (((CProperties) pAttributes.get(Integer.toString(i))).get("gridwidth") != null) {
				c.gridwidth = Integer
						.parseInt((String) ((CProperties) pAttributes
						.get(Integer.toString(i))).get("gridwidth"));
			} else {
				c.gridwidth = GridBagConstraints.REMAINDER;
			}
			if (field.getPanel().equalsIgnoreCase("TOP")) {
				if (field.getViewPanel().equalsIgnoreCase("TL")) {
					getMainPaneTopLeft().add(
							(CDisplayField) cFields.get(i - 1), c);
				} else if (field.getViewPanel().equalsIgnoreCase("TC")) {
					getMainPaneTop().add((CDisplayField) cFields.get(i - 1), c);
				} else if (field.getViewPanel().equalsIgnoreCase("TR")) {
					getMainPaneTopRight().add(
							(CDisplayField) cFields.get(i - 1), c);
				}
			} else if (field.getPanel().equalsIgnoreCase("MAIN")) {
			} else {
				getMainPane().add(tabbedPane);
				tabbedPane.addTab(field.getPanel());
				if (field.getViewPanel().equalsIgnoreCase("TL")) {
					tabbedPane.getTab(field.getPanel()).add(field, CPanel.LEFT,
							c);
				} else if (field.getViewPanel().equalsIgnoreCase("TC")) {
					tabbedPane.getTab(field.getPanel()).add(field,
							CPanel.CENTER, c);
				} else if (field.getViewPanel().equalsIgnoreCase("TR")) {
					tabbedPane.getTab(field.getPanel()).add(field,
							CPanel.RIGHT, c);
				}

			}
		}
		switch (modus) {
		case NEW:
			status = new CInfoFrameStatusNew(this);
			break;
		case EDIT:
			status = new CInfoFrameStatusEdit(this);
			break;
		default:
		case DISPLAY:
			status = new CInfoFrameStatusDisplay(this);
			break;
		case COPY:
			status = new CInfoFrameStatusCopy(this);
			break;
		case DELETE:
			status = new CInfoFrameStatusDelete(this);
			break;
		case SUB:
			status = new CInfoFrameStatusSub(this);
			break;
		case EDITALL:
			status = new CInfoFrameStatusEditAll(this);
			break;
		}

		pc = this.dataObj.getCProperties();
		if (pc.get("color") != null) {
		    String color = pc.get("color").toString();
		    int r = Integer.parseInt(color.substring(0,3));
		    int g = Integer.parseInt(color.substring(3,6));
		    int b = Integer.parseInt(color.substring(6));
			setColor(new Color(r,g,b));		    
		}
		addCustButtons();

		setFrameSize();
		setVisible(true);
	}
	
	protected void c_execute(CProperties prop) {
	    c.execute(prop);
    }

    protected void addCustButtons() {
	    CProperties cbs = (CProperties)pc.get("cust_buttons");
	    for (int i = 1; i <= cbs.size(); i++) {
	        CProperties cb = (CProperties)cbs.get(Integer.toString(i));
	        CButton button = CButtonFactory.getButton(cb.get("bez").toString());
	        getCustomButtonPane().add(button);
	        button.getCommand().setOwner(this);
/*	        button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    custButton(e);
                }       
	        });*/
	    }
	}
	
/*	public void custButton(ActionEvent e) {
	    ((CButton)e.getSource()).getCommand().execute(null);
	}*/
	
	public boolean setAttributeValue(String name, Object value) {
	    Object o = attributeValues.get(name);
	    if ((o == null && value == null) || (o != null && value != null && o.toString().equalsIgnoreCase(value.toString()))) {
	        // keine Veränderung
	    } else if (o != null && o.toString().equalsIgnoreCase("init")) {
	        // Bei Initialisierung kein Refresh aber Wert zuweisen
	        if (value == null) {
	            attributeValues.remove(name);
	        } else {
	            attributeValues.put(name, value);	
	        }
	        CActionCommand ac = (CActionCommand)attributeActions.get(name);
	        if (ac != null) {
	            ac.setOwner(this);
	            ac.execute(name);
	        }
	    } else {
	        if (value == null) {
	            attributeValues.remove(name);
	        } else {
	            attributeValues.put(name, value);	
	        }
	        CActionCommand ac = (CActionCommand)attributeActions.get(name);
	        if (ac != null) {
	            ac.setOwner(this);
	            ac.executeChange(name);
	        }
	        refresh();
	        this.edited = true;
	        return true;
	    }
	    return false;
	}
	
	public Object getAttributeValue(String name) {
	    return attributeValues.get(name);
	}
	
	public void refresh() {
	    // TODO refrehbare Attribute aktualisieren
//	    CMessage.print("refresh durchführen");
	}
	
	public void setColor(Color c) {
	    super.setColor(c);
	    for (int i = 0; i < cFields.size(); i++) {
	        ((CDisplayField)(cFields.get(i))).setColor(c);
	    }
	    tabbedPane.setColor(c);
	}

	protected void cancel() {
		dispose();
	}

	public void apply() {
		try {
			status.apply();
			parent.refresh();
			clearStatusLine();
			edited = false;
//	        CMessage.print("edited = false");
			for (Enumeration en = cFields.elements(); en.hasMoreElements();) {
			    ((CDisplayField)en.nextElement()).resetEditedColor();			    
			}
		} catch (SQLException e) {
			status.rollback();
			Toolkit.getDefaultToolkit().beep();
			setStatusLine(e.toString());
		}
	}

	public void ok() {
		try {
			status.apply();
			parent.refresh();
			clearStatusLine();
			edited = false;
			dispose();
		} catch (SQLException e) {
			status.rollback();
			Toolkit.getDefaultToolkit().beep();
			setStatusLine(e.toString());
		}
	}

	protected CInfoDataObject getCInfoDataObject() {
		return dataObj;
	}
	
	public void dispose() {
	    if (edited) {
			int returnValue;
			Object[] options = {"Weiter", "Abbrechen"};
			returnValue = JOptionPane.showOptionDialog(null,"Achtung, die Daten wurden verändert.\nWollen Sie den Dialog wirklich verlassen?","Warnung",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,options, options[0]);			
			if (returnValue != JOptionPane.OK_OPTION) return;
	    }
		super.dispose();
	}
	
    public void setFieldValue(String name, Object value) {
        for (int i = 0; i < cFields.size(); i++) {
            CDisplayField o = (CDisplayField) cFields.get(i);
//            CMessage.print(o.getName());          
            if (o.getName().equalsIgnoreCase(name)) {
                o.setValue(value);
//                CMessage.print("gefunden");
                return;
            }
        }      
    }   
    
    public CDisplayField getField(String name) {
        for (int i = 0; i < cFields.size(); i++) {
            CDisplayField o = (CDisplayField) cFields.get(i);
//            CMessage.print(o.getName());          
            if (o.getName().equalsIgnoreCase(name)) {
                return (CDisplayField)o;
            }
        }      
        return null;
    }
}