package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class CPositionTableBean extends CAddonTableBean {

    private final JTextField search = new JTextField(60);
    private final JLabel searchLabel = new JLabel("Positionieren   ");
    protected CButton bSearch;

    public CPositionTableBean(CTable tab, CListParent parent) {
        super(tab, parent);
        searchLabel.setPreferredSize(new Dimension(100, searchLabel.getPreferredSize().height));
        bSearch = CButtonFactory.getButton("search");
        bSearch.addActionListener(e -> positionSearch());
        search.addActionListener(e -> positionSearch());
        search.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                positionSearch();
            }
        });
        setLayout(new BorderLayout(5, 5));
        add(searchLabel, BorderLayout.WEST);
        add(search, BorderLayout.CENTER);
        add(bSearch, BorderLayout.EAST);
    }

    protected void positionSearch() {
        CTable tab = getTab();
        tab.findRow(search.getText());
    }

    @Override
    public void setColor(Color c) {
        this.setBackground(c);
        searchLabel.setBackground(c);
        search.setBackground(c);
        search.setOpaque(true);
        searchLabel.setOpaque(true);

    }

    @Override
    public JTextField getTextField() {
        return search;
    }


}
