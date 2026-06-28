package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CListFrame extends CFrame implements CInfoParent, CListParent {

    private static final Logger LOGGER = Logger.getLogger(CListFrame.class.getName());
    private static final String BDISPLAYCONST = "bdisplay";
    private static final String BEDITCONST = "bedit";
    private static final String BDELETECONST = "bdelete";
    private static final String BCOPYCONST = "bcopy";
    private static final String BNEWCONST = "bnew";
    private static final String DELETECONST = "delete";
    private static final String DISPLAYCONST = "display";
    private static final String DISABLEDCONST = "DISABLED";
    private final transient CListDataObject obj;
    private final String objectName;
    protected CButton bNew;
    protected CButton bEdit;
    protected CButton bDelete;
    protected CButton bCopy;
    protected CButton bDisplay;
    protected CButton bCancel;
    protected CTable tab = new CTable();
    private final JScrollPane sp = new JScrollPane(tab);
    protected CProperties p;
    private CAddonTableBean searchBean = null;

    public CListFrame(String objDesc, CFrame parent) {
        super(parent);
        this.objectName = objDesc;
        setName(objDesc + ".list");
        obj = CDataObjectFactory.getCListDataObject(objDesc);
        p = obj.getCProperties();
        bNew = CButtonFactory.getButton("new");
        bNew.addActionListener(e -> onNew());
        bEdit = CButtonFactory.getButton("edit");
        bEdit.addActionListener(e -> onEdit());
        bDelete = CButtonFactory.getButton(DELETECONST);
        bDelete.addActionListener(e -> onDelete());
        bCopy = CButtonFactory.getButton("copy");
        bCopy.addActionListener(e -> onCopy());
        bDisplay = CButtonFactory.getButton(DISPLAYCONST);
        bDisplay.addActionListener(e -> onDisplay());
        bCancel = CButtonFactory.getButton("cancel");
        bCancel.addActionListener(e -> onCancel());

        addButton(bNew, p.get(BNEWCONST));
        addButton(bEdit, p.get(BEDITCONST));
        addButton(bDelete, p.get(BDELETECONST));
        addButton(bCopy, p.get(BCOPYCONST));
        addButton(bDisplay, p.get(BDISPLAYCONST));

        getButtonPaneRight().add(bCancel);
        tab.setCListDataObject(obj);
        if (p.get("order") != null) {
            tab.setModel(obj.select(p));
        } else {
            tab.setModel(obj.select(1));
        }
        setStatusLine(tab.getRowCount() + " Eintr�ge");
        tab.setListParent();
        tab.setWidth(obj.getCProperties());
        tab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickCount = e.getClickCount();
                if (clickCount >= 2) {
                    Object o = obj.getCProperties().get("default_button");
                    if (o != null && o.toString().equalsIgnoreCase("bEdit")) {
                        onEdit();
                    } else {
                        onDisplay();
                    }
                }
            }
        });
        searchBean = new CSearchTableBean(tab, this);
        getMainPaneTopLeft().add(searchBean);

        getMainPane().add(sp);

        setTitle(obj.getObjectLabel() + " - �bersicht");

        if (p.get("color") != null) {
            String color = p.get("color").toString();
            int r = Integer.parseInt(color.substring(0, 3));
            int g = Integer.parseInt(color.substring(3, 6));
            int b = Integer.parseInt(color.substring(6));
            setColor(new Color(r, g, b));
        }
        addCustButtons();
        setFrameSize();
        CPropertyManager.getInstance().setDialog(getName(), this);
        setVisible(true);
    }

    public CAddonTableBean getSearchBean() {
        return searchBean;
    }

    protected void addCustButtons() {
        CProperties cbs = (CProperties) p.get("cust_buttons");
        for (int i = 1; i <= cbs.size(); i++) {
            CProperties cb = (CProperties) cbs.get(Integer.toString(i));
            CButton button = CButtonFactory.getButton(cb.get("bez").toString());
            getCustomButtonPane().add(button);
            button.getCommand().setOwner(this);
            button.addActionListener(this::custButton);
        }
    }

    public void custButton(ActionEvent e) {
        ((CButton) e.getSource()).getCommand().execute(null);
    }

    @Override
    public void setColor(Color c) {
        super.setColor(c);
        if (searchBean != null) searchBean.setColor(c);
    }

    private void addButton(CButton button, Object option) {
        if (option == null || option.toString().isEmpty()) {
            getButtonPaneLeft().add(button);
            return;
        }
        if (option.toString().equalsIgnoreCase(DISABLEDCONST)) {
            getButtonPaneLeft().add(button);
            button.setEnabled(false);
            return;
        }
        if (option.toString().equalsIgnoreCase("NONE")) {
            return;
        }
        getButtonPaneLeft().add(button);
    }

    public void refresh() {
        if (p.get("order") != null) {
            tab.setModel(obj.select(p));
            tab.setWidth(obj.getCProperties());
        } else {
            tab.setModel(obj.select(1));
            tab.setWidth(obj.getCProperties());
        }
    }

    protected void onCancel() {
        dispose();
    }

    protected void onDisplay() {
        if (p.get(BDISPLAYCONST) == null || p.get(BDISPLAYCONST).toString().equalsIgnoreCase("")) {
            new CInfoFrame(CInfoFrame.DISPLAY, getSelectedElement(), this);
        } else if (!p.get(BDISPLAYCONST).toString().equalsIgnoreCase("NONE")
                && !p.get(BDISPLAYCONST).toString().equalsIgnoreCase(DISABLEDCONST)) {
            CCommand command;
            try {
                command = newCommand(p.get(BDISPLAYCONST).toString());
                command.setOwner(this);
                command.execute(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to execute bDisplay command", e);
            }
        }
    }

    private CProperties getSelectedElement() {
        CProperties selectedProperties = new CProperties();
        selectedProperties.put("objectName", objectName);
        selectedProperties.put("keys", tab.getKeys());
        return selectedProperties;
    }

    protected void onCopy() {
        if (p.get(BCOPYCONST) == null || p.get(BCOPYCONST).toString().equalsIgnoreCase("")) {
            new CInfoFrame(CInfoFrame.COPY, getSelectedElement(), this);
        } else if (!p.get(BCOPYCONST).toString().equalsIgnoreCase("NONE")
                && !p.get(BCOPYCONST).toString().equalsIgnoreCase(DISABLEDCONST)) {
            CCommand command;
            try {
                command = newCommand(p.get(BCOPYCONST).toString());
                command.setOwner(this);
                command.execute(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to execute bCopy command", e);
            }
        }
    }

    protected void onDelete() {
        if (p.get(BDELETECONST) == null || p.get(BDELETECONST).toString().equalsIgnoreCase("")) {
            new CInfoFrame(CInfoFrame.DELETE, getSelectedElement(), this);
        } else if (!p.get(BDELETECONST).toString().equalsIgnoreCase("NONE")
                && !p.get(BDELETECONST).toString().equalsIgnoreCase(DISABLEDCONST)) {
            CCommand command;
            try {
                command = newCommand(p.get(BDELETECONST).toString());
                command.setOwner(this);
                command.execute(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to execute bDelete command", e);
            }
        }
    }

    public void onEdit() {
        if (p.get(BEDITCONST) == null || p.get(BEDITCONST).toString().equalsIgnoreCase("")) {
            new CInfoFrame(CInfoFrame.EDITALL, getSelectedElement(), this);
        } else if (!p.get(BEDITCONST).toString().equalsIgnoreCase("NONE")
                && !p.get(BEDITCONST).toString().equalsIgnoreCase(DISABLEDCONST)) {
            CCommand command;
            try {
                command = newCommand(p.get(BEDITCONST).toString());
                command.setOwner(this);
                command.execute(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to execute bEdit command", e);
            }
        }
    }

    protected void onNew() {
        if (p.get("bnew") == null || p.get("bnew").toString().equalsIgnoreCase("")) {
            new CInfoFrame(CInfoFrame.NEW, getNewElement(), this);
        } else if (!p.get("bnew").toString().equalsIgnoreCase("NONE")
                && !p.get("bnew").toString().equalsIgnoreCase(DISABLEDCONST)) {
            CCommand command;
            try {
                command = newCommand(p.get("bnew").toString());
                command.setOwner(this);
                command.execute(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to execute bNew command", e);
            }
        }
    }

    protected CProperties getNewElement() {
        CProperties properties = new CProperties();
        properties.put("objectName", objectName);
        return properties;
    }

    @Override
    public void dispose() {
        try {
            Statement statement = ((CTableModel) tab.getModel()).getStatement();
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException _) {
            /*soll halt excepten ey kp */
        }
        CPropertyManager.getInstance().setDialog(getName(), null);
        super.dispose();
    }

    private CCommand newCommand(String className) throws ReflectiveOperationException {
        return (CCommand) Class.forName(className).getDeclaredConstructor().newInstance();
    }
}
