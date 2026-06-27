package compucrash;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

public class CReportFrame extends CFrame {
	
	private CProperties p;
	private CButton bPrint;
	private CButton bDisplay;
	private CButton bCancel;
	private CButton bSource;
	private static final int DISPLAY = 0;
	private static final int PRINT = 0;
	private CSelectDialog selectDialog;
	private Hashtable fields = new Hashtable();
	private Hashtable files = new Hashtable();
	GridBagConstraints c = new GridBagConstraints();
	

	public CReportFrame(CProperties p) {
		super(null);
		this.p = p;
		if (p.get("title") != null)	setTitle(p.get("title").toString());
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		getMainPaneTop().setLayout(new GridBagLayout());
		for (int i = 1; i <= p.size(); i++) {
			CProperties pA = (CProperties) p.get(Integer.toString(i));
			if (pA == null) continue;
			if (pA.get("label") != null ) {
				JPanel panel = new JPanel();
				panel.setLayout(new GridLayout(0,1));
				panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
						.createEtchedBorder(), pA.get("label").toString()));
				if (pA.get("equals") != null) {
					JPanel p1 = new JPanel();
					p1.setLayout(new FlowLayout(FlowLayout.LEFT));
					panel.add(p1);
					JLabel equalsLabel = new JLabel("ist gleich");
					equalsLabel.setPreferredSize(
							new Dimension(100,equalsLabel.getPreferredSize().height));
					p1.add(equalsLabel);
					CExtendedTextField equalsText = new CExtendedTextField(20);
					fields.put(Integer.toString(i),equalsText);
					pA.put("equalsTextfield", equalsText);
					if (pA.get("source") != null) {
						bSource = CButtonFactory.getButton("dropdown");
						bSource.setActionCommand(Integer.toString(i));
						p1.add(bSource);
						bSource.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								System.out.println(e);
								bSource(e);
							}			
						});
					}
					if (pA.get("init") != null) {
						equalsText.setText(pA.get("init").toString());
					}
					p1.add(equalsText);
				}
				if (pA.get("between") != null) {
					JPanel p2 = new JPanel();
					p2.setLayout(new FlowLayout(FlowLayout.LEFT));
					panel.add(p2);
					JLabel fromLabel = new JLabel("von");
					fromLabel.setPreferredSize(
							new Dimension(100,fromLabel.getPreferredSize().height));
					p2.add(fromLabel);
					JTextField fromText = new JTextField(20);
					p2.add(fromText);
					pA.put("fromTextfield", fromText);
					JLabel toLabel = new JLabel("  bis  ");
					p2.add(toLabel);
					JTextField toText = new JTextField(20);
					p2.add(toText);
					pA.put("toTextfield", toText);
				}
				if (pA.get("like") != null) {
					JPanel p3 = new JPanel();
					p3.setLayout(new FlowLayout(FlowLayout.LEFT));
					panel.add(p3);
					JLabel likeLabel = new JLabel("enthält");
					likeLabel.setPreferredSize(
							new Dimension(100,likeLabel.getPreferredSize().height));
					p3.add(likeLabel);
					JTextField likeText = new JTextField(20);
					p3.add(likeText);
					pA.put("likeTextfield", likeText);
				}
				if (pA.get("check") != null) {
					JPanel p4 = new JPanel();
					p4.setLayout(new FlowLayout(FlowLayout.LEFT));
					panel.add(p4);
					JLabel checkLabel = new JLabel("Auswahl");
					checkLabel.setPreferredSize(
							new Dimension(100,checkLabel.getPreferredSize().height));
					p4.add(checkLabel);
					JCheckBox checkBox = new JCheckBox();
					p4.add(checkBox);
					pA.put("checkbox", checkBox);
					
				}
				if (pA.get("file") != null) {
					JPanel p5 = new JPanel();
					p5.setLayout(new FlowLayout(FlowLayout.LEFT));
					panel.add(p5);
					JLabel fileLabel = new JLabel("Datei");
					fileLabel.setPreferredSize(
							new Dimension(100,fileLabel.getPreferredSize().height));
					p5.add(fileLabel);
					JTextField fileText = new JTextField(20);
					p5.add(fileText);
					files.put(Integer.toString(i),fileText);
					pA.put("fileTextfield", fileText);
					CButton bSource = CButtonFactory.getButton("dropdown");
					bSource.setActionCommand(Integer.toString(i));
					p5.add(bSource);
					bSource.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
//							System.out.println(e);
							bFile(e);
						}			
					});

				}
				if (pA.get("text") != null) {
					JPanel p6 = new JPanel();
					p6.setLayout(new FlowLayout(FlowLayout.LEFT));
					panel.add(p6);
					JLabel textLabel = new JLabel("Text:");
					textLabel.setPreferredSize(
							new Dimension(100,textLabel.getPreferredSize().height));
					p6.add(textLabel);
					JTextArea textText = new JTextArea(10,60);
					p6.add(new JScrollPane(textText));
					pA.put("textTextfield", textText);
				}
				if (pA.get("multiple") != null) {
					JPanel p7 = new JPanel();
					p7.setLayout(new FlowLayout(FlowLayout.LEFT));
					panel.add(p7);
					JLabel multipleLabel = new JLabel("Auswahl:");
					multipleLabel.setPreferredSize(
							new Dimension(100,multipleLabel.getPreferredSize().height));
					p7.add(multipleLabel);
					CTable multipleTable = new CTable();
					CListDataObject ldo = CDataObjectFactory.getCListDataObject((String)pA.get("source"));
					CProperties p_ldo = ldo.getCProperties();
					multipleTable.setCListDataObject(ldo);
					if (p_ldo.get("order") != null) {
					    multipleTable.setModel(ldo.select(p_ldo));
					}else {
					    multipleTable.setModel(ldo.select(1));
					}
					multipleTable.setWidth(ldo.getCProperties());

//					multipleTable.setModel(CDataObjectFactory.getCListDataObject((String)pA.get("source")).select(1));
					multipleTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
					JScrollPane sp = new JScrollPane(multipleTable);
					sp.setPreferredSize(new Dimension(600,Math.min(Math.max(multipleTable.getRowCount(),3)* 21,200)));
					if (pA.get("height") != null) {
						sp.setPreferredSize(new Dimension(600, Integer.parseInt(pA.get("height").toString())));			    
					}
					p7.add(sp);
					pA.put("multipleTable", multipleTable);			
					if (pA.get("columns") != null) {
						multipleTable.setAutoResizeMode(CTable.AUTO_RESIZE_ALL_COLUMNS);
					}
				}
				panel.setPreferredSize(new Dimension(800,panel.getPreferredSize().height));
				getMainPaneTop().add(panel,c);				
			}	
		}
		bDisplay = CButtonFactory.getButton("display");
		bDisplay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});
/*		bPrint = CButtonFactory.getButton("print");
		bPrint.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});*/
		bCancel = CButtonFactory.getButton("cancel");
		bCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		getButtonPaneLeft().add(bDisplay);
//		getButtonPaneLeft().add(bPrint);
		getButtonPaneRight().add(bCancel);
		
		pack();
		setVisible(true);
	}

	protected void bSource(ActionEvent e) {
		Component o = (Component)fields.get(e.getActionCommand());
		if (selectDialog instanceof CSelectDialog) {
			selectDialog.dispose();
		}
		selectDialog = new CSelectDialog(o,(CSelectParent)o,CDataObjectFactory.getCListDataObject(((CProperties)(p.get(e.getActionCommand()))).get("source").toString()));
	    selectDialog.setBounds(0,0,selectDialog.getWidth(),selectDialog.getHeight());
		while (o != null && o.getClass() != CReportFrame.class) {
			o = o.getParent();
		}
	    try {
	        o.setSize(Math.max(o.getWidth(),selectDialog.getWidth()),Math.max(o.getHeight(), selectDialog.getHeight() + 21));
			o.doLayout();
	        ((CReportFrame)o).getDesktopPane().add(selectDialog,JDesktopPane.MODAL_LAYER);
		    } catch (Exception ex) {
		    }
	}

	protected void bFile(ActionEvent e) {
		JTextField o = (JTextField)files.get(e.getActionCommand());
		JFileChooser chooser = new JFileChooser();
		Object importdir = CPropertyManager.getInstance().getProperties().get("importdir");
   		if ( importdir != null) {
   			chooser.setCurrentDirectory(new File(importdir.toString()));
   		}
		int state = chooser.showOpenDialog(null);
		File fin = chooser.getSelectedFile();
		if (fin != null && state == JFileChooser.APPROVE_OPTION) {
			o.setText(fin.getPath());
		}

	}
	protected void ok() {
		
		// Hier aus p auslesen, welche Elemente es gibt. und übergeben
		for (int i = 1; i <= p.size(); i++) {
			CProperties pA = (CProperties) p.get(Integer.toString(i));
			if (pA == null) continue;
			if (pA.get("equals") != null ) {
				pA.put("equalsValue", ((JTextField)pA.get("equalsTextfield")).getText());
			}
			if (pA.get("between") != null ) {
				if (((JTextField)pA.get("fromTextfield")).getText() != null) {
					pA.put("fromValue", ((JTextField)pA.get("fromTextfield")).getText());
				} else {
					pA.remove("fromValue");
				}
				if (((JTextField)pA.get("toTextfield")).getText() != null) {
					pA.put("toValue", ((JTextField)pA.get("toTextfield")).getText());
				} else {
					pA.remove("toValue");
				}
			}
			if (pA.get("like") != null ) {
				pA.put("likeValue", ((JTextField)pA.get("likeTextfield")).getText());
			}
			if (pA.get("check") != null ) {
				pA.put("checkValue", new Boolean(((JCheckBox)pA.get("checkbox")).isSelected()));
			}
			if (pA.get("file") != null ) {
				pA.put("fileValue", ((JTextField)pA.get("fileTextfield")).getText());
			}
			if (pA.get("text") != null ) {
				pA.put("textValue", ((JTextArea)pA.get("textTextfield")).getText());
			}
			if (pA.get("multiple") != null ) {
				pA.put("multipleValue", ((CTable)pA.get("multipleTable")));
			}
		}
		((CReport)p.get("this")).set(p);			
		((CReport)p.get("this")).go();	
	}
}
