package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CReportFrame extends CFrame {
    private static final String SOURCE = "source";
    private final CProperties p;
    private final Map<String, Component> fields = new HashMap<>();
    private final Map<String, JTextField> files = new HashMap<>();
    GridBagConstraints c = new GridBagConstraints();
    private CSelectDialog selectDialog;


    public CReportFrame(CProperties p) {
        super(null);
        this.p = p;
        if (p.get("title") != null) setTitle(p.get("title").toString());
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        getMainPaneTop().setLayout(new GridBagLayout());
        for (int i = 1; i <= p.size(); i++) {
            CProperties pA = (CProperties) p.get(Integer.toString(i));
            if (pA == null || pA.get("label") == null) continue;
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(0, 1));
            panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                    .createEtchedBorder(), pA.get("label").toString()));
            addEqualsPanelIfNeeded(panel, pA, i);
            addBetweenPanelIfNeeded(panel, pA);
            addLikePanelIfNeeded(panel, pA);
            addCheckPanelIfNeeded(panel, pA);
            addFilePanelIfNeeded(panel, pA, i);
            addTextPanelIfNeeded(panel, pA);
            addMultiplePanelIfNeeded(panel, pA);
            panel.setPreferredSize(new Dimension(800, panel.getPreferredSize().height));
            getMainPaneTop().add(panel, c);
        }
        CButton bDisplay = CButtonFactory.getButton("display");
        bDisplay.addActionListener(_ -> ok());
        CButton bCancel = CButtonFactory.getButton("cancel");
        bCancel.addActionListener(_ -> dispose());
        getButtonPaneLeft().add(bDisplay);
        getButtonPaneRight().add(bCancel);

        pack();
        setVisible(true);
    }

    protected void bSource(ActionEvent e) {
        Component o = fields.get(e.getActionCommand());
        if (selectDialog != null) {
            selectDialog.dispose();
        }
        selectDialog = new CSelectDialog(o, (CSelectParent) o, CDataObjectFactory.getCListDataObject(((CProperties) (p.get(e.getActionCommand()))).get(SOURCE).toString()));
        selectDialog.setBounds(0, 0, selectDialog.getWidth(), selectDialog.getHeight());
        while (o != null && o.getClass() != CReportFrame.class) {
            o = o.getParent();
        }
        if (o != null) {
            o.setSize(Math.max(o.getWidth(), selectDialog.getWidth()), Math.max(o.getHeight(), selectDialog.getHeight() + 21));
            o.doLayout();
            ((CReportFrame) o).getDesktopPane().add(selectDialog, JLayeredPane.MODAL_LAYER);
        }
    }

    protected void bFile(ActionEvent e) {
        JTextField o = files.get(e.getActionCommand());
        JFileChooser chooser = new JFileChooser();
        Object importdir = CPropertyManager.getInstance().getProperties().get("importdir");
        if (importdir != null) {
            chooser.setCurrentDirectory(new File(importdir.toString()));
        }
        int state = chooser.showOpenDialog(null);
        File fin = chooser.getSelectedFile();
        if (fin != null && state == JFileChooser.APPROVE_OPTION) {
            o.setText(fin.getPath());
        }

    }

    protected void ok() {
        for (int i = 1; i <= p.size(); i++) {
            CProperties pA = (CProperties) p.get(Integer.toString(i));
            if (pA != null) collectFieldValue(pA);
        }
        ((CReport) p.get("this")).set(p);
        ((CReport) p.get("this")).go();
    }

    private void collectFieldValue(CProperties pA) {
        if (pA.get("equals") != null) pA.put("equalsValue", ((JTextField) pA.get("equalsTextfield")).getText());
        if (pA.get("between") != null) collectBetweenValues(pA);
        if (pA.get("like") != null) pA.put("likeValue", ((JTextField) pA.get("likeTextfield")).getText());
        if (pA.get("check") != null)
            pA.put("checkValue", ((JCheckBox) pA.get("checkbox")).isSelected());
        if (pA.get("file") != null) pA.put("fileValue", ((JTextField) pA.get("fileTextfield")).getText());
        if (pA.get("text") != null) pA.put("textValue", ((JTextArea) pA.get("textTextfield")).getText());
        if (pA.get("multiple") != null) pA.put("multipleValue", pA.get("multipleTable"));
    }

    private void collectBetweenValues(CProperties pA) {
        String from = ((JTextField) pA.get("fromTextfield")).getText();
        if (from != null) pA.put("fromValue", from);
        else pA.remove("fromValue");
        String to = ((JTextField) pA.get("toTextfield")).getText();
        if (to != null) pA.put("toValue", to);
        else pA.remove("toValue");
    }

    private void addEqualsPanelIfNeeded(JPanel panel, CProperties pA, int index) {
        if (pA.get("equals") == null) return;
        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(p1);
        JLabel equalsLabel = new JLabel("ist gleich");
        equalsLabel.setPreferredSize(new Dimension(100, equalsLabel.getPreferredSize().height));
        p1.add(equalsLabel);
        CExtendedTextField equalsText = new CExtendedTextField(20);
        fields.put(Integer.toString(index), equalsText);
        pA.put("equalsTextfield", equalsText);
        if (pA.get(SOURCE) != null) {
            CButton bSource = CButtonFactory.getButton("dropdown");
            bSource.setActionCommand(Integer.toString(index));
            p1.add(bSource);
            bSource.addActionListener(this::bSource);
        }
        if (pA.get("init") != null) equalsText.setText(pA.get("init").toString());
        p1.add(equalsText);
    }

    private void addBetweenPanelIfNeeded(JPanel panel, CProperties pA) {
        if (pA.get("between") == null) return;
        JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(p2);
        JLabel fromLabel = new JLabel("von");
        fromLabel.setPreferredSize(new Dimension(100, fromLabel.getPreferredSize().height));
        p2.add(fromLabel);
        JTextField fromText = new JTextField(20);
        p2.add(fromText);
        pA.put("fromTextfield", fromText);
        p2.add(new JLabel("  bis  "));
        JTextField toText = new JTextField(20);
        p2.add(toText);
        pA.put("toTextfield", toText);
    }

    private void addLikePanelIfNeeded(JPanel panel, CProperties pA) {
        if (pA.get("like") == null) return;
        JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(p3);
        JLabel likeLabel = new JLabel("enthält");
        likeLabel.setPreferredSize(new Dimension(100, likeLabel.getPreferredSize().height));
        p3.add(likeLabel);
        JTextField likeText = new JTextField(20);
        p3.add(likeText);
        pA.put("likeTextfield", likeText);
    }

    private void addCheckPanelIfNeeded(JPanel panel, CProperties pA) {
        if (pA.get("check") == null) return;
        JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(p4);
        JLabel checkLabel = new JLabel("Auswahl");
        checkLabel.setPreferredSize(new Dimension(100, checkLabel.getPreferredSize().height));
        p4.add(checkLabel);
        JCheckBox checkBox = new JCheckBox();
        p4.add(checkBox);
        pA.put("checkbox", checkBox);
    }

    private void addFilePanelIfNeeded(JPanel panel, CProperties pA, int index) {
        if (pA.get("file") == null) return;
        JPanel p5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(p5);
        JLabel fileLabel = new JLabel("Datei");
        fileLabel.setPreferredSize(new Dimension(100, fileLabel.getPreferredSize().height));
        p5.add(fileLabel);
        JTextField fileText = new JTextField(20);
        p5.add(fileText);
        files.put(Integer.toString(index), fileText);
        pA.put("fileTextfield", fileText);
        CButton bFileBtn = CButtonFactory.getButton("dropdown");
        bFileBtn.setActionCommand(Integer.toString(index));
        p5.add(bFileBtn);
        bFileBtn.addActionListener(this::bFile);
    }

    private void addTextPanelIfNeeded(JPanel panel, CProperties pA) {
        if (pA.get("text") == null) return;
        JPanel p6 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(p6);
        JLabel textLabel = new JLabel("Text:");
        textLabel.setPreferredSize(new Dimension(100, textLabel.getPreferredSize().height));
        p6.add(textLabel);
        JTextArea textText = new JTextArea(10, 60);
        p6.add(new JScrollPane(textText));
        pA.put("textTextfield", textText);
    }

    private void addMultiplePanelIfNeeded(JPanel panel, CProperties pA) {
        if (pA.get("multiple") == null) return;
        JPanel p7 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(p7);
        JLabel multipleLabel = new JLabel("Auswahl:");
        multipleLabel.setPreferredSize(new Dimension(100, multipleLabel.getPreferredSize().height));
        p7.add(multipleLabel);
        CTable multipleTable = new CTable();
        CListDataObject ldo = CDataObjectFactory.getCListDataObject((String) pA.get(SOURCE));
        CProperties pLdo = ldo.getCProperties();
        multipleTable.setCListDataObject(ldo);
        multipleTable.setModel(pLdo.get("order") != null ? ldo.select(pLdo) : ldo.select(1));
        multipleTable.setWidth(ldo.getCProperties());
        multipleTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane sp = new JScrollPane(multipleTable);
        sp.setPreferredSize(new Dimension(600, Math.min(Math.max(multipleTable.getRowCount(), 3) * 21, 200)));
        if (pA.get("height") != null)
            sp.setPreferredSize(new Dimension(600, Integer.parseInt(pA.get("height").toString())));
        p7.add(sp);
        pA.put("multipleTable", multipleTable);
        if (pA.get("columns") != null) multipleTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }
}
