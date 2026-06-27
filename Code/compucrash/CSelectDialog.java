package compucrash;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class CSelectDialog extends JInternalFrame implements CInfoParent {
	
	CProperties p;
	CTable tab = new CTable();
	JScrollPane sp = new JScrollPane(tab);
	CButton bCancel;
	CButton bSelect;
	Component o;
	CSelectParent t;
	private CButton bNew;
	private CButton bEdit;
	private CButton bDelete;
	private CListDataObject ldo;
	CProperties p_ldo;
	private JPanel p1 = new JPanel();
	private JPanel p2 = new JPanel();
	private JPanel p3 = new JPanel();

	public CSelectDialog(Component o, CSelectParent t, CProperties p) {
//		super((Frame)o,true);
	    super("Auswahl",true, true, true);
		this.o = o;
		this.t = t;
		this.p = p;
///		setIconImage(Toolkit.getDefaultToolkit().createImage("../images/logo.gif"));
//		setLocationRelativeTo(t);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		CTableModel tabModel = new CTableModel();
		tabModel.addColumn((String) p.get("label"));
		tabModel.addRows(CDataManager.getInstance().getSelect(p));
		tab.setModel(tabModel);
		tab.setAutoResizeMode(CTable.AUTO_RESIZE_ALL_COLUMNS);
		int height = Math.min(Math.max((tab.getRowCount() + 1) * 20,100), 600);
		int width = 200;
		sp.setPreferredSize(new Dimension(width, height));
		tab.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int clickCount = e.getClickCount();
				if (clickCount >= 2) {
					bSelect();
				}
			}
		});
///		setTitle((String) p.get("label"));
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(sp, BorderLayout.CENTER);
		p1.setLayout(new BorderLayout());
		cp.add(p1, BorderLayout.SOUTH);
		p1.add(p2, BorderLayout.WEST);
		p2.setLayout(new FlowLayout(FlowLayout.LEFT));
		bSelect = CButtonFactory.getButton("ok");
		bSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bSelect();
			}
		});
		p2.add(bSelect);
		p1.add(p3, BorderLayout.EAST);
		p3.setLayout(new FlowLayout(FlowLayout.RIGHT));
		bCancel = CButtonFactory.getButton("cancel");
		bCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bCancel();
			}
		});
		p3.add(bCancel);
		
		pack();
		setVisible(true);
	}

	public CSelectDialog(Component o, CSelectParent t, CListDataObject ldo) {
//		super((Frame)o,true);
	    super("Auswahl",true,true,true);
	    CMessage.print("Hallo");
		this.o = o;
		this.t = t;
		this.ldo = ldo;
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
///		setIconImage(Toolkit.getDefaultToolkit().createImage("../images/logo.gif"));
//		if (t instanceof Component) {
//			setLocationRelativeTo((Component)t);
//		}
		p_ldo = ldo.getCProperties();
		tab.setCListDataObject(ldo);
		if (p_ldo.get("order") != null) {
			tab.setModel(ldo.select(p_ldo));
		}else {
			tab.setModel(ldo.select(1));
		}
		int height = Math.min(Math.max((tab.getRowCount() + 1) * 20,100), 600);
		int width = 200;
		sp.setPreferredSize(new Dimension(width, height));
		tab.setWidth(ldo.getCProperties());
		tab.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int clickCount = e.getClickCount();
				if (clickCount >= 2) {
					bSelect();
				}
			}
		});
//		setTitle((String) p.get("label"));
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(sp, BorderLayout.CENTER);
		p1.setLayout(new BorderLayout());
		cp.add(p1, BorderLayout.SOUTH);
		p1.add(p2, BorderLayout.WEST);
		p2.setLayout(new FlowLayout(FlowLayout.LEFT));
		bSelect = CButtonFactory.getButton("ok");
		bSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bSelect();
			}
		});
		p2.add(bSelect);
		bNew = CButtonFactory.getButton("new");
		if (p_ldo.get("bnew") == null || p_ldo.get("bnew").toString().equalsIgnoreCase("")) {
			bNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bNew();
			}
		});
		}
//		p2.add(bNew);
		bEdit = CButtonFactory.getButton("edit");
		if (p_ldo.get("bedit") == null || p_ldo.get("bedit").toString().equalsIgnoreCase("")) {
		bEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bEdit();
			}
		});
		}
//		p2.add(bEdit);
		bDelete = CButtonFactory.getButton("delete");
		if (p_ldo.get("bdelete") == null || p_ldo.get("bdelete").toString().equalsIgnoreCase("")) {
		bDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bDelete();
			}
		});
		}
//		p2.add(bDelete);
		
		p1.add(p3, BorderLayout.EAST);
		p3.setLayout(new FlowLayout(FlowLayout.RIGHT));
		bCancel = CButtonFactory.getButton("cancel");
		bCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bCancel();
			}
		});
		addButton(bNew,p_ldo.get("bnew"));
		addButton(bEdit,p_ldo.get("bedit"));
		addButton(bDelete,p_ldo.get("bdelete"));
		p3.add(bCancel);

		if (p_ldo.get("color") != null) {
		    String color = p_ldo.get("color").toString();
		    int r = Integer.parseInt(color.substring(0,3));
		    int g = Integer.parseInt(color.substring(3,6));
		    int b = Integer.parseInt(color.substring(6));
			setColor(new Color(r,g,b));		    
		}
		pack();
		setVisible(true);
	}
	
	public void dispose() {
	    t.resetSelectDialog();
	    super.dispose();
	}
	
	public void setColor(Color c) {
	    getContentPane().setBackground(c);
	    p1.setBackground(c);
	    p2.setBackground(c);
	    p3.setBackground(c);
	}

	protected void bCancel() {
		dispose();
	}

	protected void bDelete() {
		new CInfoFrame(CInfoFrame.DELETE, getSelectedElement(), this);
	}

	protected void bEdit() {
		new CInfoFrame(CInfoFrame.EDITALL, getSelectedElement(), this);
	}

	protected void bNew() {
		new CInfoFrame(CInfoFrame.NEW, getNewElement(), this);
	}

	private CProperties getSelectedElement() {
		CProperties p = new CProperties();
		p.put("object_name", ldo.getCProperties().get("object_name").toString());
		p.put("keys",tab.getKeys());
		return p;
	}
	
	protected CProperties getNewElement() {
		CProperties p = new CProperties();
		p.put("object_name", ldo.getCProperties().get("object_name").toString());
		p.put("key","");
		return p;
	}

	protected void bSelect() {
	    if (t instanceof CDisplayFieldListBean) {
	        int[] rows = tab.getSelectedRows();
	        Object[] values = new Object[rows.length];
	        for (int i = 0; i < rows.length; i++) {
	            values[i] = tab.getValueAt(rows[i],0);
	        }
            ((CDisplayFieldListBean)t).insert(values);	        
	    } else {
	        t.setValue(tab.getValueAt(tab.getSelectedRow(),0));
	    }
		dispose();
	}

	public void refresh () {
		if (p_ldo.get("order") != null) {
			tab.setModel(ldo.select(p_ldo));
		}else {
			tab.setModel(ldo.select(1));
		}
	}


/*	public int getOrderColumn() {
		// Feste Sortierung, immer nach der ersten Spalte
		return 1;
	}*/
	
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
		if (option.toString().length() > 0) {
			CCommand command = null;
			try {
				command = (CCommand)Class.forName(option.toString()).newInstance();
				command.setOwner(this);
				button.setCCommand(command);
				p2.add(button);
				return;
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		p2.add(button);
		System.out.println("Invalid option for:");
		System.out.println(button);
		System.out.println(option.toString());		
	}


}
