package compucrash;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.LookAndFeel;
import javax.swing.table.TableCellRenderer;

public class CTableHeaderRenderer implements TableCellRenderer {
	
	private JPanel panel;
	public static final int ASC = 1;
	public static final int NONE = 0;
	public static final int DESC = -1;
	private int order = 0;
	private int zustand = 0;
	private ImageIcon orderAsc = new ImageIcon("images/orderAsc.gif");
	private ImageIcon orderDesc = new ImageIcon("images/orderDesc.gif");
	private ImageIcon orderNone = new ImageIcon("images/orderNone.gif");
	private CTable table = null;
	private JLabel label;
	private JLabel orderButton;
	
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int col) {
			if (panel == null) {
				panel = new JPanel();
				panel.setLayout(new FlowLayout(FlowLayout.LEFT));
				label = new JLabel(table.getColumnName(col));
				panel.add(label);
				orderButton = new JLabel(orderAsc);
				panel.add(orderButton);
				orderButton.setToolTipText("Hilfe");
				if (zustand == -1) {
					orderButton.setIcon(orderDesc);
				} else if (zustand == 1) {
					orderButton.setIcon(orderAsc);
				} else {
					orderButton.setIcon(orderNone);
				}
				LookAndFeel.installColorsAndFont(panel, 
						"TableHeader.background",
						"TableHeader.foreground",
						"TableHeader.font");
				LookAndFeel.installBorder(panel, "TableHeader.cellBorder");
			}
		return panel;
	}
}
