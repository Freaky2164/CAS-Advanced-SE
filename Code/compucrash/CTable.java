package compucrash;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CTable extends JTable {

    private static final Logger LOGGER = Logger.getLogger(CTable.class.getName());
    private final int orderColumn = 1;
    JPopupMenu popup = new JPopupMenu("Tabelle");
    JMenuItem saveCSV = new JMenuItem("Speichern als CSV");
    JMenuItem saveXLS = new JMenuItem("Speichern als XLS");
    private CListFrame owner = null;
    private CListDataObject obj = null;

    public CTable() {
        super();
        setCellSelectionEnabled(false);
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setDefaultRenderer(Float.class, new CTableCellRenderer());
        setDefaultRenderer(Double.class, new CTableCellRenderer());
        saveCSV.addActionListener(e -> getAsCSV());
        saveXLS.addActionListener(e -> getAsXLS());
        popup.add(saveCSV);
        popup.add(saveXLS);
    }

    public CListDataObject getCListDataObject() {
        return obj;
    }

    public void setCListDataObject(CListDataObject obj) {
        this.obj = obj;
    }

    public CProperties getKeys() {
        return null;
    }

    public int getOrderColumn() {
        return orderColumn;
    }

    protected CListFrame getListParent() {
        return owner;
    }

    public void setListParent(CListFrame parent) {
        this.owner = parent;
    }

    protected void getAsXLS() {
        JFileChooser chooser = new JFileChooser();
        int state = chooser.showSaveDialog(this);
        File file = chooser.getSelectedFile();
        if (file == null || state != JFileChooser.APPROVE_OPTION) return;
        try {
            if (!file.createNewFile()) {
                Object[] options = {"\u00dcberschreiben", "Abbrechen"};
                int returnValue = JOptionPane.showOptionDialog(null, "Wollen Sie die Datei wirklich \u00fcberschreiben?", "Information", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
                if (returnValue != JOptionPane.OK_OPTION) return;
            }
            try (FileOutputStream out = new FileOutputStream(file);
                 HSSFWorkbook wb = new HSSFWorkbook()) {
                HSSFSheet sheet1 = wb.createSheet("Tabelle1");
                HSSFRow rowA = sheet1.createRow((short) 0);
                HSSFCell cell1;
                for (int j = 0; j < getColumnCount(); j++) {
                    cell1 = rowA.createCell((short) j);
                    cell1.setCellValue(getColumnName(j));
                }
                for (int i = 0; i < getRowCount(); i++) {
                    rowA = sheet1.createRow((short) (i + 1));
                    for (int j = 0; j < getColumnCount(); j++) {
                        cell1 = rowA.createCell((short) j);
                        if (getValueAt(i, j) != null) cell1.setCellValue(getValueAt(i, j).toString());
                    }
                }
                wb.write(out);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save as XLS", e);
        }
    }

    public void setModel(TableModel model) {
        super.setModel(model);
    }

    public void setWidth(CProperties p) {
        CProperties pAttributes = (CProperties) p.get("attributes");
        for (int i = 1; i <= pAttributes.size(); i++) {
            CProperties pA = (CProperties) pAttributes.get(Integer.toString(i));
            int listDataScale = Integer.parseInt(pA.get("list_data_scale").toString());
            if (listDataScale != 0) {
                if (listDataScale > 0) {
                    getColumnModel().getColumn(i - 1).setPreferredWidth(listDataScale);
                } else {
                    getColumnModel().getColumn(i - 1).setPreferredWidth(listDataScale);
                    getColumnModel().getColumn(i - 1).setMinWidth(listDataScale);
                    getColumnModel().getColumn(i - 1).setMaxWidth(listDataScale);
                }
            }
        }
    }

    public void getAsCSV() {
        JFileChooser chooser = new JFileChooser();
        int state = chooser.showSaveDialog(this);
        File file = chooser.getSelectedFile();
        if (file == null || state != JFileChooser.APPROVE_OPTION) return;
        try {
            if (!file.createNewFile()) {
                Object[] options = {"\u00dcberschreiben", "Abbrechen"};
                int returnValue = JOptionPane.showOptionDialog(null, "Wollen Sie die Datei wirklich \u00fcberschreiben?", "Information", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
                if (returnValue != JOptionPane.OK_OPTION) return;
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                StringBuilder header = new StringBuilder();
                for (int j = 0; j < getColumnCount(); j++) {
                    header.append(getColumnName(j)).append(";");
                }
                header.deleteCharAt(header.length() - 1);
                header.append("\n");
                out.write(header.toString().getBytes());
                for (int i = 0; i < getRowCount(); i++) {
                    StringBuilder row = new StringBuilder();
                    for (int j = 0; j < getColumnCount(); j++) {
                        if (getValueAt(i, j) == null) {
                            row.append(";");
                        } else if (getValueAt(i, j).toString().contains(";")) {
                            row.append("\"").append(getValueAt(i, j).toString().replaceAll("\"", "\"\"")).append("\";");
                        } else {
                            row.append(getValueAt(i, j).toString().replaceAll("\"", "\"\"")).append(";");
                        }
                    }
                    row.deleteCharAt(row.length() - 1);
                    row.append("\n");
                    out.write(row.toString().getBytes());
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save as CSV", e);
        }
    }

    public void exclude(int col) {
        getColumnModel().getColumn(col - 1).setMaxWidth(0);
        getColumnModel().getColumn(col - 1).setMinWidth(0);
        getColumnModel().getColumn(col - 1).setPreferredWidth(0);
    }

    public void findRow(String text) {
        for (int i = 0; i < getRowCount(); i++) {
            if (getValueAt(i, 0).toString().toLowerCase().startsWith(text.toLowerCase())) {
                setRowSelectionInterval(i, i);
                scrollRectToVisible(new Rectangle(0, i * getRowHeight(), getWidth(), getHeight()));
                return;
            }
        }
        Toolkit.getDefaultToolkit().beep();
    }

    public void setModel(CTableModel select) {
        // Overloaded method provided for type compatibility; use setModel(TableModel) instead
    }
}
