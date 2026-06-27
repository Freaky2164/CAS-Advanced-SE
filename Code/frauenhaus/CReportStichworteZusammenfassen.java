package frauenhaus;

import java.awt.Toolkit;
import java.sql.SQLException;
import java.text.NumberFormat;

import javax.swing.JOptionPane;

import compucrash.CCommand;
import compucrash.CDataManager;
import compucrash.CProperties;
import compucrash.CPropertyManager;
import compucrash.CReport;
import compucrash.CReportFrame;
import compucrash.CTable;

public class CReportStichworteZusammenfassen extends CCommand implements CReport {

	private CProperties p;
	private NumberFormat nf = NumberFormat.getInstance();
	private String reports;
	private String vorlagen;
	private String excel;
		
	public CReportStichworteZusammenfassen() {
		this.reports = CPropertyManager.getInstance().getProperty("reports");
		this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
		this.excel = CPropertyManager.getInstance().getProperty("excel");
	}

	public Object execute(Object parameters) {
		
		p = new CProperties();
		p.put("this",this);
		CProperties pA = new CProperties();
		p.put(Integer.toString(1),pA);
		pA.put("label", "Stichworte");
		pA.put("height", "150");
		pA.put("multiple", "1");
		pA.put("source","stichwort");
		pA = new CProperties();
		p.put(Integer.toString(2),pA);
		pA.put("label", "Zusammenfassen in Stichwort");
		pA.put("equals", "1");
		pA.put("source","stichwort");
		new CReportFrame(p);

		return null;
	}

	public void go() {
		String stichworteAlt = "";
		String stichwortNeu = "";
		
		if (((CProperties)p.get("1")).get("multipleValue") != null) {
			CTable tab = ((CTable)((CProperties)p.get("1")).get("multipleValue"));
			int[] rows = tab.getSelectedRows();
			for (int i = 0; i < rows.length; i++) {
			    stichworteAlt += "'" + tab.getValueAt(rows[i],0).toString().trim() + "',";
			}
			stichworteAlt = stichworteAlt.substring(0, stichworteAlt.length()-1);
		} else {
		    Toolkit.getDefaultToolkit().beep();
		    return;
		}
		if (((CProperties)p.get("2")).get("equalsValue") != null) {
			stichwortNeu = ((CProperties)p.get("2")).get("equalsValue").toString().trim();
		} else {
		    Toolkit.getDefaultToolkit().beep();
		    return;
		}
		
		try {
		    // Neues Stichwort zuordnen
			String SQLString = "INSERT INTO frauenhaus.stichwort_person " +
					"SELECT DISTINCT mitglied, '" + stichwortNeu + "' " +
					"FROM frauenhaus.mitglied m " +
					"WHERE m.mitglied IN (SELECT mitglied " +
					"FROM frauenhaus.stichwort_person " +
					"WHERE stichwort IN (" + stichworteAlt + ")) " +
					"AND m.mitglied NOT IN (SELECT mitglied " +
					"FROM frauenhaus.stichwort_person " +
					"WHERE stichwort = '" + stichwortNeu + "') ";
			System.out.println(SQLString);
			CDataManager.getInstance().getStatement().execute(SQLString);
			// Alte Stichwortzuordnung löschen
			SQLString = "DELETE FROM frauenhaus.stichwort_person " +
					"WHERE stichwort IN (" + stichworteAlt + ") " +
					"AND stichwort != '" + stichwortNeu + "' ";
			System.out.println(SQLString);
			CDataManager.getInstance().getStatement().execute(SQLString);
			// Alte Stichwörter löschen
			SQLString = "DELETE FROM frauenhaus.stichwort " +
					"WHERE stichwort IN (" + stichworteAlt + ") " +
					"AND stichwort != '" + stichwortNeu + "' ";
			System.out.println(SQLString);
			CDataManager.getInstance().getStatement().execute(SQLString);			
			CDataManager.getInstance().getConnection().commit();
			// Erfolgsmeldung einbauen
			JOptionPane.showMessageDialog(null, 
			        "Stichworte erfolgreich zusammengefasst", 
			        "Stichworte Zusammenfassen - Info", 
			        JOptionPane.INFORMATION_MESSAGE);
		} catch (SQLException e) {
			e.printStackTrace();
			Toolkit.getDefaultToolkit().beep();
			try {
				CDataManager.getInstance().getConnection().rollback();			    
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			return;
		}
	}

	public void set(CProperties p) {
		this.p = p;
	}

}
