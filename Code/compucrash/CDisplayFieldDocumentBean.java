package compucrash;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.adobe.acrobat.Viewer;
import com.adobe.acrobat.ViewerCommand;

public class CDisplayFieldDocumentBean extends CDisplayFieldBean {
// Änderungen bei Dokumenten werden nicht registriert
	private JLabel label = new JLabel();
	private Viewer viewer;
	protected CButton bNew;
	protected CButton bDelete;
	protected CButton bPrint;
	private InputStream stream = null;
	private Blob blob = null;
	private JPanel px = new JPanel();
	
	public CDisplayFieldDocumentBean(CProperties p, CInfoFrame frame) {
		super(p, frame);
		setLayout(new FlowLayout(FlowLayout.LEFT));
		add(label);
		add(px);
		px.setLayout(new BorderLayout());
		try {
			viewer = new Viewer();
			viewer.activate();
			px.add(viewer, BorderLayout.NORTH);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), 
				(String) p.get("label"))
				);

		if (p.get("tooltip") != null) {
			setToolTipText((String) p.get("tooltip"));
		}
		viewer.setPreferredSize(new Dimension(Integer.parseInt((String) p.get("data_scale"))*15, 45));
		if (Integer.parseInt((String)p.get("data_height")) != 0) {
			viewer.setPreferredSize(new Dimension(Integer.parseInt((String) p.get("data_scale")) * 15, 
					Integer.parseInt((String)p.get("data_height")) * 21));
		}
		bNew = CButtonFactory.getButton("new");
		bNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bNew();
			}
		});
		bDelete = CButtonFactory.getButton("delete");
		bDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bDelete();
			}
		});
		bPrint = CButtonFactory.getButton("print");
		bPrint.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    bPrint();
			}
		});
		JPanel p2 = new JPanel();
		p2.setLayout(new FlowLayout(FlowLayout.LEFT));
		px.add(p2, BorderLayout.SOUTH);
		p2.add(bNew);
		p2.add(bDelete);
		p2.add(bPrint);
	}
	
	public Component getTextField() {
	    return null;
	}

	public void setEditedColor() {
	    // TODO irgendeine Markierung?
//	    text.setForeground(Color.BLUE);
	}

	public void resetEditedColor() {
	    // TODO Irgendeine Markierung wieder entfernen
//	    text.setForeground(Color.BLACK);
	}
	protected void bPrint() {
		if (stream == null) {
			return;
		}
		byte[] b;
        try {
            b = new byte[stream.available()];
            stream.read(b);
            File fout = new File("out.pdf");
            FileOutputStream foutstr = new FileOutputStream(fout);
            foutstr.write(b);
            foutstr.close();
            Runtime r = Runtime.getRuntime();
            Process p = null;
            String acrobatPath = "";
            if (CPropertyManager.getInstance().getProperty("acrobat") != null) {
    			acrobatPath = CPropertyManager.getInstance().getProperty("acrobat") + "/";
    		}
            p = r.exec("\"" + acrobatPath + "acrord32\" " + fout.getAbsolutePath());
      //      p = r.exec("explorer.exe " + fout.getAbsolutePath());
            p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        File fout = new File("out.pdf");
        fout.delete();
/*            String acrobatPath = "";
    		if (CPropertyManager.getInstance().getProperty("acrobat") != null) {
    			acrobatPath = CPropertyManager.getInstance().getProperty("acrobat") + "/";
    		}
            Runtime.getRuntime().exec("\"" + acrobatPath + "acrord32\" /p /h" + fout.getAbsolutePath());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
*/
	}

	protected void bDelete() {
		stream = null;
            try {
            	FileInputStream fin = new FileInputStream("../images/nada_nix.pdf");
            	viewer.setDocumentInputStream(fin);
            } catch (Exception e) {
            	e.printStackTrace();
            }
	}

	protected void bNew() {
		JFileChooser chooser = new JFileChooser();
		chooser.addChoosableFileFilter(new CPdfFileFilter());
		Object importdir = CPropertyManager.getInstance().getProperties().get("importdir");
   		if ( importdir != null) {
   			chooser.setCurrentDirectory(new File(importdir.toString()));
   		}
		int state = chooser.showOpenDialog(null);
		File fin = chooser.getSelectedFile();
		if (fin != null && state == JFileChooser.APPROVE_OPTION) {
			System.out.println(fin.getAbsolutePath());
			try {
				stream = new FileInputStream(fin);
				viewer.setDocumentInputStream(new FileInputStream(fin));
				viewer.execMenuItem(ViewerCommand.FitWidth_K);
				viewer.execMenuItem(ViewerCommand.OneColumn_K);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}

	public void setValue(Object o) {
		if (o instanceof Blob) {
			try {
				blob = (Blob)o;
				stream = blob.getBinaryStream();
				viewer.setDocumentInputStream(blob.getBinaryStream());
				viewer.execMenuItem(ViewerCommand.FitWidth_K);
				viewer.execMenuItem(ViewerCommand.OneColumn_K);
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			FileInputStream fin;
			try {
				fin = new FileInputStream("../images/nada_nix.pdf");
				viewer.setDocumentInputStream(fin);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void setEditable(int i) {		
	}
	
	public Object getValue() {
		try {
			if (stream == null) {
				return null;
			}
			byte[] b = new byte[stream.available()];
			stream.read(b);
			if (blob == null) {
				return b;
			} else {
				blob.setBinaryStream((long) 1).write(b);
				return blob;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}
}
