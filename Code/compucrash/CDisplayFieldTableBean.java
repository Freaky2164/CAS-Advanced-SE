package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CDisplayFieldTableBean extends CDisplayFieldBean implements CInfoParent {

    private static final Logger LOGGER = Logger.getLogger(CDisplayFieldTableBean.class.getName());
    private static final String KEY_BEDIT = "bedit";
    private static final String KEY_BDELETE = "bdelete";
    private static final String KEY_BDISPLAY = "bdisplay";
    private static final String KEY_ORDER = "order";
    private static final String KEY_COLUMN_NAME = "column_name";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_TABLE_NAME = "table_name";
    private static final String KEY_OBJECT_NAME = "object_name";
    private static final String OPTION_DISABLED = "DISABLED";
    private final JScrollPane sp;
    private final JPanel p2 = new JPanel();
    private final JPanel p1;
    protected CTable tab;
    protected CButton bNew;
    protected CButton bEdit;
    protected CButton bDelete;
    protected CListDataObject objList;
    protected CInfoDataObject objInfo;
    protected CDataObject obj;
    protected CProperties pTab;
    protected CProperties infoKeys;
    protected CProperties pb = null;

    public CDisplayFieldTableBean(CProperties p, CInfoFrame frame) {
        super(p, frame);
        this.objInfo = frame.dataObj;
        initKeys();
        pb = objList.getCProperties();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), (String) p.get("label")));
        bNew = CButtonFactory.getButton("new");
        bNew.addActionListener(e -> onNew());
        bEdit = CButtonFactory.getButton("edit");
        bEdit.addActionListener(e -> onEdit());
        bDelete = CButtonFactory.getButton("delete");
        bDelete.addActionListener(e -> onDelete());
        tab = new CTable();
        int width = Integer.parseInt((String) p.get("data_scale")) * 15;
        int height = Integer.parseInt((String) p.get("data_height")) * 20;
        sp = new JScrollPane(tab);
        sp.setPreferredSize(new Dimension(width, height));
        add(sp, BorderLayout.CENTER);
        p1 = new JPanel();
        add(p1, BorderLayout.EAST);
        p1.setLayout(new BorderLayout());
        p1.add(p2, BorderLayout.NORTH);
        p2.setLayout(new GridLayout(0, 1));
        addButton(bNew, pb.get("bnew"));
        addButton(bEdit, pb.get(KEY_BEDIT));
        addButton(bDelete, pb.get(KEY_BDELETE));
        if (infoKeys.size() == 0) {
            return;
        }
        pTab = new CProperties();
        CProperties po = new CProperties();
        if (objList.getCProperties().get(KEY_ORDER) == null) {
            pTab.put(KEY_ORDER, po);
            po.put("1", "1");
        } else {
            pTab.put(KEY_ORDER, objList.getCProperties().get(KEY_ORDER));
        }
        CProperties pf = new CProperties();
        pTab.put("filter_and", infoKeys);
        CProperties pe = new CProperties();
        pTab.put("exclude", infoKeys);
        tab.setModel(objList.select(pTab));
        //		tab.setParent(this);
        tab.setWidth(objList.getCProperties());
        tab.exclude(1);
        tab.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int clickCount = e.getClickCount();
                if (clickCount >= 2) {
                    Object o = objList.getCProperties().get("default_button");
                    if (o != null && o.toString().equalsIgnoreCase("bEdit")) {
                        onEdit();
                    } else if (o != null && o.toString().equalsIgnoreCase("bDelete")) {
                        onDelete();
                    } else {
                        onDisplay();
                    }
                }
            }
        });
    }

    public void setEditedColor() {
        // No color change applicable for table bean
    }

    public void resetEditedColor() {
        // No color change applicable for table bean
    }

    private void initKeys() {
        if (frame.p.get("keys") != null) {
            infoKeys = CProperties.copyOf((CProperties) frame.p.get("keys"));
        } else {
            infoKeys = new CProperties();
        }
        objList = CDataObjectFactory.getCListDataObject(p.get("source").toString());
        CProperties paList = (CProperties) objList.getCProperties().get("attributes");

        for (int i = 1; i <= infoKeys.size(); i++) {
            CProperties pak = (CProperties) infoKeys.get(Integer.toString(i));
            for (int j = 1; j <= paList.size(); j++) {
                CProperties pal = (CProperties) paList.get(Integer.toString(j));
                if (pal.get(KEY_COLUMN_NAME).toString().equalsIgnoreCase(pak.get(KEY_COLUMN_NAME).toString())) {
                    ((CProperties) infoKeys.get(Integer.toString(i))).put(KEY_OWNER, pal.get(KEY_OWNER));
                    ((CProperties) infoKeys.get(Integer.toString(i))).put(KEY_TABLE_NAME, pal.get(KEY_TABLE_NAME));
                    ((CProperties) infoKeys.get(Integer.toString(i))).put("operator", "=");
                }
            }
        }

    }

    protected void onDelete() {
        if (pb.get(KEY_BDELETE) == null || pb.get(KEY_BDELETE).toString().equalsIgnoreCase("")) {
            new CInfoFrame(CInfoFrame.DELETE, getSelectedElement(), this);
        } else if (!pb.get(KEY_BDELETE).toString().equalsIgnoreCase("NONE")
                && !pb.get(KEY_BDELETE).toString().equalsIgnoreCase(OPTION_DISABLED)) {
            CCommand command;
            try {
                command = newCommand(pb.get(KEY_BDELETE).toString());
                command.setOwner(this);
                command.execute(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to execute bDelete command", e);
            }
        }
    }

    protected void onEdit() {
        if (pb.get(KEY_BEDIT) == null || pb.get(KEY_BEDIT).toString().equalsIgnoreCase("")) {
            new CInfoFrame(CInfoFrame.EDIT, getSelectedElement(), this);
        } else if (!pb.get(KEY_BEDIT).toString().equalsIgnoreCase("NONE")
                && !pb.get(KEY_BEDIT).toString().equalsIgnoreCase(OPTION_DISABLED)) {
            CCommand command;
            try {
                command = newCommand(pb.get(KEY_BEDIT).toString());
                command.setOwner(this);
                command.execute(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to execute bEdit command", e);
            }
        }
    }

    protected void onNew() {
        if (pb.get("bnew") == null || pb.get("bnew").toString().equalsIgnoreCase("")) {
            new CInfoFrame(CInfoFrame.SUB, getNewElement(), this);
        } else if (!pb.get("bnew").toString().equalsIgnoreCase("NONE")
                && !pb.get("bnew").toString().equalsIgnoreCase(OPTION_DISABLED)) {
            CCommand command;
            try {
                command = newCommand(pb.get("bnew").toString());
                command.setOwner(this);
                command.execute(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to execute bNew command", e);
            }
        }
    }

    protected void onDisplay() {
        if (pb.get(KEY_BDISPLAY) == null || pb.get(KEY_BDISPLAY).toString().equalsIgnoreCase("")) {
            new CInfoFrame(CInfoFrame.DISPLAY, getSelectedElement(), this);
        } else if (!pb.get(KEY_BDISPLAY).toString().equalsIgnoreCase("NONE")
                && !pb.get(KEY_BDISPLAY).toString().equalsIgnoreCase(OPTION_DISABLED)) {
            CCommand command;
            try {
                command = newCommand(pb.get(KEY_BDISPLAY).toString());
                command.setOwner(this);
                command.execute(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to execute bDisplay command", e);
            }
        }
    }

    public CProperties getSelectedElement() {
        CProperties p = new CProperties();
        p.put(KEY_OBJECT_NAME, objList.getCProperties().get(KEY_OBJECT_NAME).toString());
        p.put("keys", tab.getKeys());
        return p;
    }

    public Component getTextField() {
        return null;
    }

    public CProperties getNewElement() {
        CProperties paList = (CProperties) objList.getCProperties().get("attributes");
        CProperties p = new CProperties();
        p.put(KEY_OBJECT_NAME, objList.getCProperties().get(KEY_OBJECT_NAME).toString());
        CProperties ps = new CProperties();
        p.put("sub", ps);
        int k = 0;
        for (int i = 1; i <= infoKeys.size(); i++) {
            CProperties pa = (CProperties) infoKeys.get(Integer.toString(i));
            for (int j = 1; j <= paList.size(); j++) {
                CProperties pal = (CProperties) paList.get(Integer.toString(j));
                if (pa.get(KEY_COLUMN_NAME).toString().equalsIgnoreCase(pal.get(KEY_COLUMN_NAME).toString())) {
                    k++;
                    CProperties psa = new CProperties();
                    ps.put(Integer.toString(k), psa);
                    psa.put(KEY_OWNER, pal.get(KEY_OWNER));
                    psa.put(KEY_TABLE_NAME, pal.get(KEY_TABLE_NAME));
                    psa.put(KEY_COLUMN_NAME, pal.get(KEY_COLUMN_NAME));
                    psa.put("value", pa.get("value"));
                    break;
                }
            }
        }
        return p;
    }

    private CCommand newCommand(String className) throws ReflectiveOperationException {
        return (CCommand) Class.forName(className).getDeclaredConstructor().newInstance();
    }

    public void setColor(Color c) {
        super.setColor(c);
//        if (bNew != null) bNew.setBackground(c);
//        if (bEdit != null) bEdit.setBackground(c);
//        if (bDelete != null) bDelete.setBackground(c);
        p1.setBackground(c);
        p2.setBackground(c);
    }

    public String getText() {
        return null;
    }

    public void setEditable(int i) {
        boolean bool = i == 1;
        initKeys();
        if (infoKeys.size() == 0) bool = false;
        bNew.setEnabled(bool);
        bEdit.setEnabled(bool);
        bDelete.setEnabled(bool);
        if (infoKeys.size() == 0) {
            return;
        }
        pTab = new CProperties();
        CProperties po = new CProperties();
        if (objList.getCProperties().get(KEY_ORDER) == null) {
            pTab.put(KEY_ORDER, po);
            po.put("1", "1");
        } else {
            pTab.put(KEY_ORDER, objList.getCProperties().get(KEY_ORDER));
        }
        CProperties pf = new CProperties();
        pTab.put("filter_and", infoKeys);
        CProperties pe = new CProperties();
        pTab.put("exclude", infoKeys);
        tab.setModel(objList.select(pTab));
        //		tab.setParent(this);
        tab.setWidth(objList.getCProperties());
        tab.exclude(1);
        if (tab.getMouseListeners().length == 3) {
            addDoubleClickListener();
        }

    }

    private void addDoubleClickListener() {
        tab.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    handleDoubleClick();
                }
            }
        });
    }

    private void handleDoubleClick() {
        Object o = objList.getCProperties().get("default_button");
        if (o != null && o.toString().equalsIgnoreCase("bEdit")) {
            onEdit();
        } else if (o != null && o.toString().equalsIgnoreCase("bDelete")) {
            onDelete();
        } else {
            onDisplay();
        }
    }

    public void refresh() {
        if (pTab == null) return;
        tab.setModel(objList.select(pTab));
        tab.setWidth(objList.getCProperties());
        // TODO exclude in Tabelle mit Properties steuern
        tab.exclude(1);
        lostFocus();
    }

    public Object getValue() {
        return null;
    }

    public void setValue(Object o) {
        // Setting a value is not applicable for table bean
    }

    private void addButton(CButton button, Object option) {
        if (option == null || option.toString().isEmpty()) {
            p2.add(button);
            return;
        }
        if (option.toString().equalsIgnoreCase(OPTION_DISABLED)) {
            p2.add(button);
            button.setEnabled(false);
            return;
        }
        if (option.toString().equalsIgnoreCase("NONE")) {
            return;
        }
        p2.add(button);
    }

}
