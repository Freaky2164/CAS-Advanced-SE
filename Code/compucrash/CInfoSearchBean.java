package compucrash;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CInfoSearchBean extends JPanel {

	private JTextField search = new JTextField(60);
	private JLabel searchLabel = new JLabel("Suchen   ");
	private CInfoFrame parent;
	private JComboBox modes = new JComboBox();
	protected CButton bNew;
	protected CButton bEdit;
	protected CButton bDelete;
	protected CButton bCopy;
	protected CButton bDisplay;
	protected CProperties p;
	private JPanel p1 = new JPanel();
	private JPanel p2 = new JPanel();

    public CInfoSearchBean(CInfoFrame parent) {
        super();
        this.parent = parent;
        p = CDataObjectFactory.getCListDataObject(parent.object_name).getCProperties();
        p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Suchen"));
        p2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Modus"));
//		setLayout(new FlowLayout(FlowLayout.LEFT));
		setLayout(new BorderLayout());
		p1.add(search, BorderLayout.CENTER);
		p2.setPreferredSize(new Dimension(200, p2.getHeight()));
		search.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED) return;
                action(e);
            }		    
		});
		p2.add(modes, BorderLayout.EAST);
		add(p1, BorderLayout.CENTER);
		add(p2, BorderLayout.EAST);
		bNew = CButtonFactory.getButton("new");
		bNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bDo("bnew");
			}
		});									
		bEdit = CButtonFactory.getButton("edit");
		bEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bDo("bedit");
			}
		});
		bDelete = CButtonFactory.getButton("delete");
		bDelete.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			bDo("bdelete");
		}
	});
		bCopy = CButtonFactory.getButton("copy");
		bCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bDo("bcopy");
			}
		});
		bDisplay = CButtonFactory.getButton("display");
		bDisplay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bDo("bdisplay");
			}
		});
		
		modes.addItem("");
		addButton(bNew,p.get("bnew"));
		addButton(bEdit,p.get("bedit"));
		addButton(bDelete,p.get("bdelete"));
		addButton(bCopy,p.get("bcopy"));
		addButton(bDisplay,p.get("bdisplay"));
		
		modes.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        bDo(modes.getSelectedItem().toString());
		    }
		});		
    }

	protected void bDo(String whatis) {
	    String what = "";
	    // übel aber funktioniert.
	    // TODO überarbeiten
	    if (bNew.getText().equalsIgnoreCase(whatis)) what = "bnew";
	    if (bEdit.getText().equalsIgnoreCase(whatis)) what = "bedit";
	    if (bDelete.getText().equalsIgnoreCase(whatis)) what = "bdelete";
	    if (bCopy.getText().equalsIgnoreCase(whatis)) what = "bcopy";
	    if (bDisplay.getText().equalsIgnoreCase(whatis)) what = "bdisplay";
		if (p.get(what) == null || p.get(what).toString().equalsIgnoreCase("")) {
		    if (what.equalsIgnoreCase("bnew")) {
		        parent.attributeValues.clear();
				for (Enumeration en = parent.cFields.elements(); en.hasMoreElements();) {
				    CDisplayField f = ((CDisplayField)en.nextElement());
				    parent.attributeValues.put(f.getName(),"init");
				}
				parent.p = new CProperties();
				parent.p.put("object_name", parent.object_name);
				parent.p.put("key","");
		        parent.status = new CInfoFrameStatusNew(parent);		
		    } else if (what.equalsIgnoreCase("bedit")) {
		        parent.status = new CInfoFrameStatusEdit(parent);
		    } else if (what.equalsIgnoreCase("bdelete")) {
		        parent.status = new CInfoFrameStatusDelete(parent);
		    } else if (what.equalsIgnoreCase("bcopy")) {
		        parent.status = new CInfoFrameStatusCopy(parent);
		    } else if (what.equalsIgnoreCase("bdisplay")) {
		        parent.status = new CInfoFrameStatusDisplay(parent);
		    }
		} else if (!p.get(what).toString().equalsIgnoreCase("NONE") 
		        && !p.get(what).toString().equalsIgnoreCase("DISABLED")) {
				CCommand command;
                try {
                    command = (CCommand)Class.forName(p.get(what).toString()).newInstance();
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

	private void addButton(CButton button, Object option) {
		if (option == null || option.toString() == null || option.toString().equalsIgnoreCase("")) {
			modes.addItem(button.getText());
			return;
		}
		if (option.toString().equalsIgnoreCase("DISABLED")) {
		    // Inaktive Schaltflächen werden auch ausgeblendet
//			modes.addItem(button.getText());
			return;
		}
		if (option.toString().equalsIgnoreCase("NONE")) {
			return;
		}
		modes.addItem(button.getText());
	}

    protected void action(KeyEvent e) {
        String list = parent.object_name + ".list";
        CListFrame f = (CListFrame)CPropertyManager.getInstance().getDialog(list);
        if (f == null) {
            f = new CListFrame(parent.object_name,null);
        } else {
            f.toFront();
	        f.setVisible(true);	        
        }
        f.getSearchBean().getTextField().setText(String.valueOf(e.getKeyChar()));
        f.getSearchBean().getTextField().requestFocus();
    }

}
