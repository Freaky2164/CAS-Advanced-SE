package compucrash;
import java.awt.Component;
import java.awt.FlowLayout;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.LookAndFeel;
import javax.swing.table.DefaultTableCellRenderer;

public class CTableCellRenderer extends DefaultTableCellRenderer {

	private NumberFormat nf = NumberFormat.getInstance();

	public CTableCellRenderer() {
		super();
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);
	}
	
	public Component getTableCellRendererComponent(JTable tab, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
		JLabel label = new JLabel();
		if (value != null) label = new JLabel(nf.format(value));
		JPanel panel = new JPanel();
		FlowLayout layout = new FlowLayout(FlowLayout.RIGHT);
		layout.setHgap(0);
		layout.setVgap(0);
		panel.setLayout(layout);
		panel.add(label);
		LookAndFeel.installColorsAndFont(panel, 
				"TableCellRenderer.background",
				"TableCellRenderer.foreground",
				"TableCellRenderer.font");
		LookAndFeel.installColorsAndFont(label, 
				"TableCellRenderer.background",
				"TableCellRenderer.foreground",
				"TableCellRenderer.font");
		return panel;
	}
}
