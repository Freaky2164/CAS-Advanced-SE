package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CDisplayFieldHistoryBean extends CDisplayFieldBean implements CInfoParent {

    private static final String PROP_COLUMN_NAME = "column_name";
    private static final String PROP_OWNER = "owner";
    private static final String PROP_TABLE_NAME = "table_name";
    private static final String PROP_ORDER = "order";
    private final CTable tab;
    private final transient CListDataObject objList;
    private final transient CInfoDataObject objInfo;
    private final JPanel p2 = new JPanel();
    private final JLabel label = new JLabel();
    private final CButton bSource;
    private final CProperties paList;
    protected CButton bNew;
    protected CButton bEdit;
    protected CButton bDelete;
    //  Änderungen bei Dokumenten werden nicht registriert
    private JScrollPane sp;
    private CDataObject obj;
    private CProperties pTab;
    private CProperties infoKeys;
    private CProperties pb = null;

    public CDisplayFieldHistoryBean(CProperties p, CInfoFrame frame) {
        super(p, frame);
        this.objInfo = frame.dataObj;
        // source owner und table_name?
        //
        if (frame.p.get("keys") != null) {
            infoKeys = CProperties.copyOf((CProperties) frame.p.get("keys"));
        } else {
            infoKeys = new CProperties();
        }
        objList = CDataObjectFactory.getCListDataObject(p.get("source").toString());
        paList = (CProperties) objList.getCProperties().get("attributes");

        for (int i = 1; i <= infoKeys.size(); i++) {
            CProperties pak = (CProperties) infoKeys.get(Integer.toString(i));
            for (int j = 1; j <= paList.size(); j++) {
                CProperties pal = (CProperties) paList.get(Integer.toString(j));
                if (pal.get(PROP_COLUMN_NAME).toString().equalsIgnoreCase(pak.get(PROP_COLUMN_NAME).toString())) {
                    ((CProperties) infoKeys.get(Integer.toString(i))).put(PROP_OWNER, pal.get(PROP_OWNER));
                    ((CProperties) infoKeys.get(Integer.toString(i))).put(PROP_TABLE_NAME, pal.get(PROP_TABLE_NAME));
                    ((CProperties) infoKeys.get(Integer.toString(i))).put("operator", "=");
                }
            }
        }
        pb = objList.getCProperties();
        setLayout(new FlowLayout(FlowLayout.LEFT));
        String labelString = (String) p.get("label");
        label.setText(labelString);
        label.setPreferredSize(
                new Dimension(
                        Integer.parseInt((String) p.get("label_length")) * 7,
                        label.getPreferredSize().height));
        if (p.get("tooltip") != null) {
            setToolTipText((String) p.get("tooltip"));
        }
        add(label);
        bSource = CButtonFactory.getButton("dropdown");
        add(bSource);
        bSource.addActionListener(e -> bSource());

        tab = new CTable();
        int width = Integer.parseInt((String) p.get("data_scale")) * 15;


        tab.setPreferredSize(new Dimension(width, 16));

        add(tab, BorderLayout.CENTER);
        JPanel p1 = new JPanel();
        add(p1, BorderLayout.EAST);
        p1.setLayout(new BorderLayout());
        p1.add(p2, BorderLayout.NORTH);
        p2.setLayout(new GridLayout(0, 1));
        if (infoKeys.size() == 0) {
            return;
        }
        setPTab();
        lostFocus();
        tab.setWidth(objList.getCProperties());
        tab.exclude(1);
        tab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickCount = e.getClickCount();
                if (clickCount >= 2) {
                    bSource();
                }
            }
        });
    }

    private void setPTab() {
        if (frame.p.get("keys") != null) {
            infoKeys = CProperties.copyOf((CProperties) frame.p.get("keys"));
        } else {
            infoKeys = new CProperties();
        }
        CDisplayFieldTableBean.infoKeyParser(paList, infoKeys, PROP_COLUMN_NAME, PROP_OWNER, PROP_TABLE_NAME);
        pTab = new CProperties();
        CProperties po = new CProperties();
        if (objList.getCProperties().get(PROP_ORDER) == null) {
            pTab.put(PROP_ORDER, po);
            po.put("1", "1");
        } else {
            pTab.put(PROP_ORDER, objList.getCProperties().get(PROP_ORDER));
        }
        pTab.put("filter_and", infoKeys);
        pTab.put("exclude", infoKeys);
    }

    public Component getTextField() {
        return null;
    }

    public void setEditedColor() {
        // No color change applicable for history bean
    }

    public void resetEditedColor() {
        // No color change applicable for history bean
    }

    protected void bSource() {
        new CHistoryDialog(p, frame, this);
    }

    public CProperties getSelectedElement() {
        CProperties p = new CProperties();
        p.put("objectName", objList.getCProperties().get("objectName").toString());
        p.put("keys", tab.getKeys());
        return p;
    }

    public void setEditable(int i) {
        // Editability is not applicable for history bean
    }

    @Override
    public void refresh() {
        setPTab();
        tab.setModel(objList.select(pTab));
        tab.exclude(1);
        lostFocus();
    }

    public Object getValue() {
        return null;
    }

    public void setValue(Object o) {
        // Setting a value is not applicable for history bean
    }

    public CInfoDataObject getObjInfo() {
        return objInfo;
    }

    public JScrollPane getSp() {
        return sp;
    }

    public void setSp(JScrollPane sp) {
        this.sp = sp;
    }

    public CDataObject getObj() {
        return obj;
    }

    public void setObj(CDataObject obj) {
        this.obj = obj;
    }

    public CProperties getPb() {
        return pb;
    }

    public void setPb(CProperties pb) {
        this.pb = pb;
    }
}
