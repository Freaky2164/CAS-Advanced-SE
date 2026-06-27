
package compucrash;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class CDisplayFieldHistoryBean extends CDisplayFieldBean implements CInfoParent {
//  Änderungen bei Dokumenten werden nicht registriert
	private JScrollPane sp;
	private CTable tab;
	protected CButton bNew;
	protected CButton bEdit;
	protected CButton bDelete;
	private CListDataObject objList;
	private CInfoDataObject objInfo;
	private CDataObject obj;
	private CProperties pTab;
	private CProperties infoKeys;
	private JPanel p2 = new JPanel();
	private CProperties pb = null;
	private JLabel label = new JLabel();
	private CButton bSource;
	private CProperties paList;

	public CDisplayFieldHistoryBean(CProperties p, CInfoFrame frame) {
	    super(p, frame);
		this.objInfo = frame.dataObj;
		// source owner und table_name?
		//
		if (frame.p.get("keys") != null ) {
		    System.out.println(p);
		    System.out.println(p.getClass());
			infoKeys = (CProperties)((CProperties) (frame.p.get("keys"))).clone();
		} else {
			infoKeys = new CProperties();
		}
		objList = CDataObjectFactory.getCListDataObject(p.get("source").toString());
		paList = (CProperties) objList.getCProperties().get("attributes");
		
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
		pb = objList.getCProperties();
		setLayout(new FlowLayout(FlowLayout.LEFT));
		String labelString = (String)p.get("label") + "";
		label.setText(labelString);
		label.setPreferredSize(
			new Dimension(
				Integer.parseInt((String) p.get("label_length")) * 7,
				label.getPreferredSize().height));
		if (p.get("tooltip") != null) {
			setToolTipText((String) p.get("tooltip"));
		}
		add(label);
		bSource = CButtonFactory.getButton("dropdown");
		add(bSource);
		bSource.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bSource();
			}			
		});

		tab = new CTable();
		int width = Integer.parseInt((String) p.get("data_scale")) * 15;
		int height = Integer.parseInt((String) p.get("data_height")) * 20;
		
		tab.setPreferredSize(new Dimension(width, 16));
		
		add(tab, BorderLayout.CENTER);
		JPanel p1 = new JPanel();
		add(p1, BorderLayout.EAST);
		p1.setLayout(new BorderLayout());
		p1.add(p2, BorderLayout.NORTH);
		p2.setLayout(new GridLayout(0, 1));
		if (infoKeys.size() == 0 ) {
			return;
		}
		setPTab();
/*		pTab = new CProperties();
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
		pTab.put("exclude", infoKeys);*/
		tab.setModel(objList.select(pTab));
		//		tab.setParent(this);
		lostFocus();
		tab.setWidth(objList.getCProperties());
		tab.exclude(1);
		tab.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int clickCount = e.getClickCount();
				if (clickCount >= 2) {
						bSource();
				}
			}
		});
	}
	
	private void setPTab() {
		if ((CProperties) frame.p.get("keys") != null ) {
			infoKeys = (CProperties)((CProperties) frame.p.get("keys")).clone();
		} else {
			infoKeys = new CProperties();
		}
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
	}
	
	public Component getTextField() {
	    return null;
	}

	public void setEditedColor() {
	}

	public void resetEditedColor() {
	}	

	protected void bSource() {
	    new CHistoryDialog(p, frame, this);
	}

	public CProperties getSelectedElement() {
		CProperties p = new CProperties();
		p.put("object_name", objList.getCProperties().get("object_name").toString());
		p.put("keys", tab.getKeys());
		return p;
	}

	public void setValue(Object o) {
	}

	public void setEditable(int i) {
	}

	public void refresh () {
	    setPTab();
	    System.out.println("CDisplayFieldHistoryBean.refresh()=========================");
	    System.out.println(pTab);
//		if (pb.get("order") != null) {
			tab.setModel(objList.select(pTab));
//		} else {
//			tab.setModel(objList.select(1));
//		}
		tab.exclude(1);
		lostFocus();
	}

	public Object getValue() {
		return null;
	}
}