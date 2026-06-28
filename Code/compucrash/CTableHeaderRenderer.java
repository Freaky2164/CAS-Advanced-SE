package compucrash;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class CTableHeaderRenderer implements TableCellRenderer {

    public static final int ASC = 1;
    public static final int NONE = 0;
    public static final int DESC = -1;
    private int order = 0;
    private int zustand = 0;
    private final ImageIcon orderAsc = new ImageIcon("images/orderAsc.gif");
    private final ImageIcon orderDesc = new ImageIcon("images/orderDesc.gif");
    private final ImageIcon orderNone = new ImageIcon("images/orderNone.gif");
    private static final CTable table = null;
    private JPanel panel;


    public int getZustand() {
        return zustand;
    }
    public void setZustand(int zustand) {
        this.zustand = zustand;
    }
    public void setOrder(int order) {
        this.order = order;
    }
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int col) {
        JLabel orderButton;
        JLabel label;
        if (panel == null) {
            panel = new JPanel();
            panel.setLayout(new FlowLayout(FlowLayout.LEFT));
            label = new JLabel(table.getColumnName(col));
            panel.add(label);
            orderButton = new JLabel(orderAsc);
            panel.add(orderButton);
            orderButton.setToolTipText("Hilfe");
            switch (zustand) {
                case -1 -> orderButton.setIcon(orderDesc);
                case 1 -> orderButton.setIcon(orderAsc);
                default -> orderButton.setIcon(orderNone);
            }
            LookAndFeel.installColorsAndFont(panel,
                    "TableHeader.background",
                    "TableHeader.foreground",
                    "TableHeader.font");
            LookAndFeel.installBorder(panel, "TableHeader.cellBorder");
        }
        return panel;
    }

    public int getOrder() {
        return order;
    }

    public CTable getTable() {
        return table;
    }
}
