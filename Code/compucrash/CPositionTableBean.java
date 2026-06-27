package compucrash;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class CPositionTableBean extends CAddonTableBean {

	protected CButton bSearch;
	private JTextField search = new JTextField(60);
	private JLabel searchLabel = new JLabel("Positionieren   ");
	private CListDataObject obj;

    public CPositionTableBean(CTable tab, CListFrame parent) {
        super(tab, parent);
        searchLabel.setPreferredSize(new Dimension(100, searchLabel.getPreferredSize().height));
	    obj = tab.getCListDataObject();
		bSearch = CButtonFactory.getButton("search");
		bSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bSearch();
			}
		});
		search.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bSearch();
            }		    
		});
		search.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                bSearch();              
            }		    
		});
		setLayout(new BorderLayout(5,5));
		add(searchLabel, BorderLayout.WEST);
		add(search, BorderLayout.CENTER);
		add(bSearch, BorderLayout.EAST);
    }

	protected void bSearch() {
	    tab.findRow(search.getText());
//		tab.setModel(obj.search(search.getText()));
//		tab.setWidth(obj.getCProperties());
//		parent.setStatusLine(Integer.toString(tab.getRowCount()) + " Einträge");
	}

    public void setColor(Color c) {
        // TODO muss noch implementiert werden, Bean wir aber eh nice verwendet, wil search bean viel besser ist
        
    }

    public JTextField getTextField() {
        // TODO Auto-generated method stub
        return null;
    }


}
