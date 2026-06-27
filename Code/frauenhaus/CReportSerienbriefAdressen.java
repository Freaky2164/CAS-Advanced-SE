package frauenhaus;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import ch.kova.connector.exception.ComponentObjectModelException;
import ch.kova.connector.exception.ItemNotFoundException;
import ch.kova.connector.exception.LibraryNotFoundException;
import ch.kova.connector.ms.outlook.Outlook;
import ch.kova.connector.ms.outlook.folder.FolderType;
import ch.kova.connector.ms.outlook.folder.OutlookFolder;
import ch.kova.connector.ms.outlook.item.ItemType;
import ch.kova.connector.ms.outlook.mail.OutlookMail;

import compucrash.CCommand;
import compucrash.CDataManager;
import compucrash.CProperties;
import compucrash.CPropertyManager;
import compucrash.CReport;
import compucrash.CReportFrame;
import compucrash.CTable;

public class CReportSerienbriefAdressen extends CCommand implements CReport {

	private CProperties p;
	private NumberFormat nf = NumberFormat.getInstance();
	private String reports;
	private String vorlagen;
	private String excel;
	
	public CReportSerienbriefAdressen() {
		this.reports = CPropertyManager.getInstance().getProperty("reports");
		this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
		this.excel = CPropertyManager.getInstance().getProperty("excel");
	}

	public Object execute(Object parameters) {
		
		p = new CProperties();
		p.put("this",this);
		CProperties pA = new CProperties();
		p.put(Integer.toString(1),pA);
		pA.put("label", "Adressen");
		pA.put("height", "150");
		pA.put("multiple", "1");
		pA.put("source","mitglied");
		pA = new CProperties();
		p.put(Integer.toString(2),pA);
		pA.put("label", "Email-Text");
		pA.put("text", "1");
		new CReportFrame(p);

		return null;
	}

	public void go() {
		// Datei öffnen
		POIFSFileSystem fsin;
		HSSFWorkbook wb;
		HSSFSheet sheet;
		String verteiler = "";
		String ohne = "";
		String betreff = "";
		String text = "";
		String anhang = "";
		String bcc = "";
		String kontaktdatumvon = "";
		String kontaktdatumbis = "";
		
		if (((CProperties)p.get("1")).get("multipleValue") != null) {
			CTable tab = ((CTable)((CProperties)p.get("1")).get("multipleValue"));
			int[] rows = tab.getSelectedRows();
			for (int i = 0; i < rows.length; i++) {
				verteiler += "'" + tab.getValueAt(rows[i],0).toString().trim() + "',";
			}
			verteiler = verteiler.substring(0, verteiler.length()-1);
		}
		if (((CProperties)p.get("2")).get("textValue") != null) {
			text = ((CProperties)p.get("2")).get("textValue").toString().trim();
		}
		
		try {
			fsin = new POIFSFileSystem(new FileInputStream(vorlagen + "/Verteiler.xls"));
			wb = new HSSFWorkbook(fsin);
			String SQLString = "SELECT DISTINCT k.anrede, k.vorname, k.name, k.strasse, k.plz, k.ort, k.email, ' ' " +
					"FROM frauenhaus.mitglied k " +
					"WHERE k.mitglied IN (" + verteiler + ") ";
			SQLString += " ORDER BY k.email";
			ResultSet rset = CDataManager.getInstance().getStatement().executeQuery(SQLString);
			sheet = wb.getSheetAt(0);
			int line = 1;
			
			while (rset.next()) {
				String anrede = rset.getString(1);
				String vorname = rset.getString(2);
				String nachname = rset.getString(3);
				String strasse = rset.getString(4);
				String plz = rset.getString(5);
				String ort = rset.getString(6);
				String emailAdresse = rset.getString(7);
//				String firma = rset.getString(8);
				if (emailAdresse == null) {
					HSSFRow row = sheet.createRow(line++);
					HSSFCell cell0 = row.createCell((short)0);
					cell0.setCellValue(anrede);
					HSSFCell cell1 = row.createCell((short)1);
					cell1.setCellValue(vorname);
					HSSFCell cell2 = row.createCell((short)2);
					cell2.setCellValue(nachname);
					HSSFCell cell3 = row.createCell((short)3);
					cell3.setCellValue(strasse);
					HSSFCell cell4 = row.createCell((short)4);
					cell4.setCellValue(plz);
					HSSFCell cell5 = row.createCell((short)5);
					cell5.setCellValue(ort);
//					HSSFCell cell6 = row.createCell((short)6);
//					cell6.setCellValue(firma);
				} else {
					bcc += emailAdresse + ";";
				}			
			}
		      try
		      {
		         // Outlook application
		         Outlook outlookApplication = new Outlook();

		         // Get the Outbox folder
		         OutlookFolder outbox = outlookApplication.getDefaultFolder(FolderType.OUTBOX);

		         // Create a new mail in the outbox folder
		         OutlookMail mail = (OutlookMail) outbox.createItem(ItemType.MAIL);

		         // Set the subject, destination and contents of the mail
		         mail.setSubject(betreff);
		         mail.setTo(CPropertyManager.getInstance().getProperty("email"));
		         mail.setBCC(bcc);
		         mail.setBody(text);
		         File file = new File(anhang);
		         if (file.isFile()) mail.getAttachments().addFile(file);
		         // Send the mail
//		         mail.send();
		         mail.display();
		         // Dispose the library
		         outlookApplication.dispose();
		      }
		      catch (ItemNotFoundException ex)
		      {
		         System.out.println("The outbox folder hasn't been found: ");
		         ex.printStackTrace();
		      }
		      catch(ComponentObjectModelException ex)
		      {
		         System.out.println("COM error has occured: ");
		         ex.printStackTrace();
		      }
		      catch(LibraryNotFoundException ex)
		      {
		         // If this error occurs, verify the file 'joutlookconnector.dll' is present
		         // in java.library.path
		         System.out.println("The Java Outlook Library has not been found.");
		         ex.printStackTrace();
		      }

			FileOutputStream fileOut = new FileOutputStream(reports + "/Verteiler.xls");
		    wb.write(fileOut);
		    fileOut.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Toolkit.getDefaultToolkit().beep();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			Toolkit.getDefaultToolkit().beep();
			return;
		} catch (SQLException e) {
			e.printStackTrace();
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection selection = new StringSelection(text);
		clip.setContents(selection, null);
		File file = new File(vorlagen + "/Verteiler.dot");
		String path = file.getAbsolutePath();
		Runtime r = Runtime.getRuntime();
		Process p = null;
		try {
			p = r.exec(excel + "/winword.exe -t\"" + path + "\"");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public void set(CProperties p) {
		this.p = p;
	}

}
