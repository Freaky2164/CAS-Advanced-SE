package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CSelectDialog extends JInternalFrame implements CInfoParent {

    private static final Logger LOGGER = Logger.getLogger(CSelectDialog.class.getName());
    private final JPanel p1 = new JPanel();
    private final JPanel p2 = new JPanel();
    private final JPanel p3 = new JPanel();
    CProperties p;
    CTable tab = new CTable();
    JScrollPane sp = new JScrollPane(tab);
    CButton bCancel;
    CButton bSelect;
    Component o;
    transient CSelectParent t;
    CProperties pLdo;
    private transient CListDataObject ldo;
    private static final String BDELETE = "bdelete";
    private static final String BEDIT = "bedit";
    private static final String OBJECTNAME = "objectName";

    public CSelectDialog(Component o, CSelectParent t, CProperties p) {
        super("Auswahl", true, true, true);
        this.o = o;
        this.t = t;
        this.p = p;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        CTableModel tabModel = new CTableModel();
        tabModel.addColumn(p.get("label"));
        tabModel.addRows(CDataManager.getInstance().getSelect(p));
        tab.setModel(tabModel);
        tab.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int height = Math.clamp((tab.getRowCount() + 1) * 20L, 100, 600);
        int width = 200;
        sp.setPreferredSize(new Dimension(width, height));
        tab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickCount = e.getClickCount();
                if (clickCount >= 2) {
                    bSelect();
                }
            }
        });
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(sp, BorderLayout.CENTER);
        p1.setLayout(new BorderLayout());
        cp.add(p1, BorderLayout.SOUTH);
        p1.add(p2, BorderLayout.WEST);
        p2.setLayout(new FlowLayout(FlowLayout.LEFT));
        bSelect = CButtonFactory.getButton("ok");
        bSelect.addActionListener(e -> bSelect());
        p2.add(bSelect);
        p1.add(p3, BorderLayout.EAST);
        p3.setLayout(new FlowLayout(FlowLayout.RIGHT));
        bCancel = CButtonFactory.getButton("cancel");
        bCancel.addActionListener(e -> bCancel());
        p3.add(bCancel);

        pack();
        setVisible(true);
    }

    public CSelectDialog(Component o, CSelectParent t, CListDataObject ldo) {
        super("Auswahl", true, true, true);
        CMessage.print("Hallo");
        this.o = o;
        this.t = t;
        this.ldo = ldo;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pLdo = ldo.getCProperties();
        tab.setCListDataObject(ldo);
        if (pLdo.get("order") != null) {
            tab.setModel(ldo.select(pLdo));
        } else {
            tab.setModel(ldo.select(1));
        }
        int height = Math.clamp((tab.getRowCount() + 1) * 20L, 100, 600);
        int width = 200;
        sp.setPreferredSize(new Dimension(width, height));
        tab.setWidth(ldo.getCProperties());
        tab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickCount = e.getClickCount();
                if (clickCount >= 2) {
                    bSelect();
                }
            }
        });
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(sp, BorderLayout.CENTER);
        p1.setLayout(new BorderLayout());
        cp.add(p1, BorderLayout.SOUTH);
        p1.add(p2, BorderLayout.WEST);
        p2.setLayout(new FlowLayout(FlowLayout.LEFT));
        bSelect = CButtonFactory.getButton("ok");
        bSelect.addActionListener(e -> bSelect());
        p2.add(bSelect);
        CButton bNew = CButtonFactory.getButton("new");
        if (pLdo.get("bnew") == null || pLdo.get("bnew").toString().equalsIgnoreCase("")) {
            bNew.addActionListener(e -> bNew());
        }
        CButton bEdit = CButtonFactory.getButton("edit");
        if (pLdo.get(BEDIT) == null || pLdo.get(BEDIT).toString().equalsIgnoreCase("")) {
            bEdit.addActionListener(e -> bEdit());
        }
        CButton bDelete = CButtonFactory.getButton("delete");
        if (pLdo.get(BDELETE) == null || pLdo.get(BDELETE).toString().equalsIgnoreCase("")) {
            bDelete.addActionListener(e -> bDelete());
        }

        p1.add(p3, BorderLayout.EAST);
        p3.setLayout(new FlowLayout(FlowLayout.RIGHT));
        bCancel = CButtonFactory.getButton("cancel");
        bCancel.addActionListener(e -> bCancel());
        addButton(bNew, pLdo.get("bnew"));
        addButton(bEdit, pLdo.get(BEDIT));
        addButton(bDelete, pLdo.get(BDELETE));
        p3.add(bCancel);

        if (pLdo.get("color") != null) {
            String color = pLdo.get("color").toString();
            int r = Integer.parseInt(color.substring(0, 3));
            int g = Integer.parseInt(color.substring(3, 6));
            int b = Integer.parseInt(color.substring(6));
            setColor(new Color(r, g, b));
        }
        pack();
        setVisible(true);
    }

    @Override
    public void dispose() {
        t.resetSelectDialog();
        super.dispose();
    }

    public void setColor(Color c) {
        getContentPane().setBackground(c);
        p1.setBackground(c);
        p2.setBackground(c);
        p3.setBackground(c);
    }

    protected void bCancel() {
        dispose();
    }

    protected void bDelete() {
        new CInfoFrame(CInfoFrame.DELETE, getSelectedElement(), this);
    }

    protected void bEdit() {
        new CInfoFrame(CInfoFrame.EDITALL, getSelectedElement(), this);
    }

    protected void bNew() {
        new CInfoFrame(CInfoFrame.NEW, getNewElement(), this);
    }

    private CProperties getSelectedElement() {
        CProperties newP = new CProperties();
        newP.put(OBJECTNAME, ldo.getCProperties().get(OBJECTNAME).toString());
        newP.put("keys", tab.getKeys());
        return newP;
    }

    protected CProperties getNewElement() {
        CProperties anotherP = new CProperties();
        anotherP.put(OBJECTNAME, ldo.getCProperties().get(OBJECTNAME).toString());
        anotherP.put("key", "");
        return anotherP;
    }

    protected void bSelect() {
        if (t instanceof CDisplayFieldListBean cdisplayfieldlistbean) {
            int[] rows = tab.getSelectedRows();
            Object[] values = new Object[rows.length];
            for (int i = 0; i < rows.length; i++) {
                values[i] = tab.getValueAt(rows[i], 0);
            }
            (cdisplayfieldlistbean).insert(values);
        } else {
            t.setValue(tab.getValueAt(tab.getSelectedRow(), 0));
        }
        dispose();
    }

    public void refresh() {
        if (pLdo.get("order") != null) {
            tab.setModel(ldo.select(pLdo));
        } else {
            tab.setModel(ldo.select(1));
        }
    }


    private void addButton(CButton button, Object option) {
        if (option == null || option.toString().isEmpty()) {
            p2.add(button);
            return;
        }
        if (option.toString().equalsIgnoreCase("DISABLED")) {
            p2.add(button);
            button.setEnabled(false);
            return;
        }
        if (option.toString().equalsIgnoreCase("NONE")) {
            return;
        }
        if (!option.toString().isEmpty()) {
            CCommand command = null;
            try {
                command = (CCommand) Class.forName(option.toString()).getDeclaredConstructor().newInstance();
                command.setOwner(this);
                button.setCCommand(command);
                p2.add(button);
                return;
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to instantiate command for button", e);
            }
        }
        p2.add(button);
        CMessage.print("Invalid option for:");
        CMessage.print(button);
        CMessage.print(option);
    }


}
