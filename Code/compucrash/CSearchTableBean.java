package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class CSearchTableBean extends CAddonTableBean {

    private final JTextField search = new JTextField(60);
    private final JLabel searchLabel = new JLabel("Suchen   ");
    private final CListDataObject obj;

    public CSearchTableBean(CTable tab, CListParent parent) {
        super(tab, parent);
        searchLabel.setPreferredSize(new Dimension(100, searchLabel.getPreferredSize().height));
        obj = tab.getCListDataObject();
        search.addActionListener(e -> search_actionPerformed());
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
        setLayout(new BorderLayout(5, 5));
        add(searchLabel, BorderLayout.WEST);
        add(search, BorderLayout.CENTER);

    }

    @Override
    public JTextField getTextField() {
        return search;
    }

    protected void down() {
        CTable tab = getTab();
        int position = Math.max(0, tab.getSelectedRow());
        if (position < tab.getRowCount() - 1) {
            tab.setRowSelectionInterval(position + 1, position + 1);
        } else {
            tab.setRowSelectionInterval(0, 0);
        }
    }

    protected void up() {
        CTable tab = getTab();
        int position = Math.max(0, tab.getSelectedRow());
        if (position == 0) {
            tab.setRowSelectionInterval(tab.getRowCount() - 1, tab.getRowCount() - 1);
        } else {
            tab.setRowSelectionInterval(position - 1, position - 1);
        }
    }

    protected void search_actionPerformed() {
        getAddParent().onEdit();
    }

    protected void bSearch() {
        CTable tab = getTab();
        tab.setModel(obj.search(search.getText()));
        tab.setWidth(obj.getCProperties());
        if (tab.getRowCount() > 0) {
            tab.setRowSelectionInterval(0, 0);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
        getAddParent().setStatusLine(tab.getRowCount() + " Eintr�ge");
    }

    @Override
    public void setColor(Color c) {
        this.setBackground(c);
        searchLabel.setBackground(c);
        search.setBackground(c);
        search.setOpaque(true);
        searchLabel.setOpaque(true);

    }
}
