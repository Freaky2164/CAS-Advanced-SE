package compucrash;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class CSearchTableBean extends CAddonTableBean {

	private JTextField search = new JTextField(60);
	private JLabel searchLabel = new JLabel("Suchen   ");
	private CListDataObject obj;

    public CSearchTableBean(CTable tab, CListParent parent) {
        super(tab, parent);
        searchLabel.setPreferredSize(new Dimension(100, searchLabel.getPreferredSize().height));
	    obj = tab.getCListDataObject();
		search.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                search_actionPerformed();
            }		    
		});
		search.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                	up();
                    return;
                case KeyEvent.VK_DOWN:
                	down();
                    return;
                }
                bSearch();              
            }		    
		});
		setLayout(new BorderLayout(5,5));
		add(searchLabel, BorderLayout.WEST);
		add(search, BorderLayout.CENTER);
		
    }

    public JTextField getTextField() {
        return search;
    }
    
    protected void down() {
        int position = Math.max(0,tab.getSelectedRow());
        if (position < tab.getRowCount() - 1) {
            tab.setRowSelectionInterval(position + 1,position + 1);
        } else {
            tab.setRowSelectionInterval(0,0);   
        }
    }

    protected void up() {
        int position = Math.max(0,tab.getSelectedRow());
        if (position == 0) {
            tab.setRowSelectionInterval(tab.getRowCount() - 1,tab.getRowCount() - 1);
        } else {
            tab.setRowSelectionInterval(position - 1, position - 1);   
        }
    }

    protected void search_actionPerformed() {
        parent.bEdit();
    }

    protected void bSearch() {
		tab.setModel(obj.search(search.getText()));
		tab.setWidth(obj.getCProperties());
		if (tab.getRowCount() > 0) {
		    tab.setRowSelectionInterval(0,0);
		} else {
		    Toolkit.getDefaultToolkit().beep();
		}
		parent.setStatusLine(Integer.toString(tab.getRowCount()) + " Einträge");
	}

    public void setColor(Color c) {
        this.setBackground(c);
        
    }
}
