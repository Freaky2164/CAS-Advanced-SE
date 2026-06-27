package compucrash;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class CTable extends JTable {
	
	JPopupMenu popup = new JPopupMenu("Tabelle");
	JMenuItem saveCSV = new JMenuItem("Speichern als CSV");
	JMenuItem saveXLS = new JMenuItem("Speichern als XLS");
	private int orderColumn = 1;
	private CListFrame parent = null;
	private CListDataObject obj = null;

	public CTable() {
		super();
		setCellSelectionEnabled(false);
		setRowSelectionAllowed(true);
		setColumnSelectionAllowed(false);
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setDefaultRenderer(Float.class, new CTableCellRenderer());
		setDefaultRenderer(Double.class, new CTableCellRenderer());
		saveCSV.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getAsCSV();
			}
		});
		saveXLS.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getAsXLS();
			}
		});
		popup.add(saveCSV);
		popup.add(saveXLS);
//		setPopupMenu(popup);
		// MouseListener f�r Sortierung
//		JTableHeader hdr = getTableHeader();
/*		hdr.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				TableColumnModel tcm = getColumnModel();
				int vc = tcm.getColumnIndexAtX(e.getX());
				orderColumn = vc + 1;
				System.out.println(e.getButton());
				if (parent == null) return;
				parent.refresh(orderColumn);
			}
		});*/
	}
	
	public void setCListDataObject(CListDataObject obj) {
		this.obj = obj;
	}
	
	public CListDataObject getCListDataObject() {
		return obj;
	}

    public CProperties getKeys() {
        return null;
    }

	public int getOrderColumn() {
		return orderColumn;
	}
	
	public void setListParent(CListFrame parent) {
		this.parent = parent;
	}
	
	protected CListFrame getListParent() {
		return parent;
	}
	
	protected void getAsXLS() {
		JFileChooser chooser = new JFileChooser();
		int state = chooser.showSaveDialog(this);
		File file = chooser.getSelectedFile();
		if (file == null | state != JFileChooser.APPROVE_OPTION) return;
		file.getPath();		
		try {
			if (!file.createNewFile()) {
				Object[] options = {"�berschreiben", "Abbrechen"};
				int returnValue = JOptionPane.showOptionDialog(null,"Wollen Sie die Datei wirklich �berschreiben?","Information",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE,null,options, options[0]);
				if (returnValue != JOptionPane.OK_OPTION) return;
			}
			FileOutputStream out = new FileOutputStream(file);
			
			HSSFWorkbook wb = new HSSFWorkbook();
			HSSFSheet sheet1 = wb.createSheet("Tabelle1");
			HSSFRow rowA = sheet1.createRow((short)0);
			HSSFCell cell1;
			for (int j = 0; j < getColumnCount(); j++) {
				cell1 = rowA.createCell((short)0);
				cell1.setCellValue(getColumnName(j));
			}
			for (int i = 0; i < getRowCount(); i++) {
				rowA = sheet1.createRow((short)(i+1));
				for (int j = 0; j < getColumnCount(); j++) {
					cell1 = rowA.createCell((short)j);
					if (getValueAt(i, j) != null) cell1.setCellValue(getValueAt(i, j).toString());
				}
			}
			wb.write(out);
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void setModel(TableModel model) {
		super.setModel(model);
//		for (int i = 0; i < model.getColumnCount(); i++ ) {
//			TableColumn column = getColumn(model.getColumnName(i));
//			column.setHeaderRenderer(new CTableHeaderRenderer());
//		}
	}
	

	public void setWidth(CProperties p) {
		CProperties pAttributes = (CProperties) p.get("attributes");
		for (int i = 1; i <= pAttributes.size(); i++) {
			CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
			int listDataScale = Integer.parseInt(pA.get("list_data_scale").toString());
			if (listDataScale != 0) {
				if (listDataScale > 0) {
					getColumnModel().getColumn(i-1).setPreferredWidth(listDataScale);
				} else {
					getColumnModel().getColumn(i-1).setPreferredWidth(listDataScale);
					getColumnModel().getColumn(i-1).setMinWidth(listDataScale);					
					getColumnModel().getColumn(i-1).setMaxWidth(listDataScale);					
				}
			}
		}		
	}
	public void getAsCSV() {
		JFileChooser chooser = new JFileChooser();
		int state = chooser.showSaveDialog(this);
		File file = chooser.getSelectedFile();
		if (file == null | state != JFileChooser.APPROVE_OPTION) return;
		file.getPath();		
		try {
			if (!file.createNewFile()) {
				Object[] options = {"�berschreiben", "Abbrechen"};
				int returnValue = JOptionPane.showOptionDialog(null,"Wollen Sie die Datei wirklich �berschreiben?","Information",JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE,null,options, options[0]);
				if (returnValue != JOptionPane.OK_OPTION) return;
			}
			FileOutputStream out = new FileOutputStream(file);
			String output = new String();
			for (int j = 0; j < getColumnCount(); j++) {
				output += getColumnName(j) + ";";
			}
			output = output.substring(0, output.length() - 1);
			output += "\n";
			out.write(output.getBytes());
			for (int i = 0; i < getRowCount(); i++) {
				output = new String();
/*				
 * DIeser CodeTeil ist erst ab 1.5.0 m�glich
				for (int j = 0; j < getColumnCount(); j++) {
					if (getValueAt(i, j) == null) {
						output += ";";
					} else if (getValueAt(i, j).toString().contains(";")) {
						output += "\"" + getValueAt(i, j).toString().replaceAll("\"","\"\"") + "\";";
					} else {
						output += getValueAt(i, j).toString().replaceAll("\"","\"\"") + ";";
					}
				}
				*/
				for (int j = 0; j < getColumnCount(); j++) {
					if (getValueAt(i, j) == null) {
						output += ";";
					} else {
						output += "\"" + getValueAt(i, j).toString().replaceAll("\"","\"\"") + "\";";
					}
				}
// Bis hierher Ersatz f�r 1.4.1_07
				
				
				output = output.substring(0, output.length() - 1);
				output += "\n";
				out.write(output.getBytes());
			}
			out.flush();   
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exclude(int col) {
		getColumnModel().getColumn(col-1).setMaxWidth(0);
		getColumnModel().getColumn(col-1).setMinWidth(0);
		getColumnModel().getColumn(col-1).setPreferredWidth(0);
	}

    public void findRow(String text) {
       for (int i = 0; i < getRowCount();i++) {
           if (getValueAt(i,0).toString().toLowerCase().startsWith(text.toLowerCase())) {
               setRowSelectionInterval(i,i);
               scrollRectToVisible(new Rectangle(0, i * getRowHeight(), getWidth(), getHeight()));
               return;
           }
       }
       Toolkit.getDefaultToolkit().beep();
    }

	public void setModel(CTableModel select) {
	}
}
