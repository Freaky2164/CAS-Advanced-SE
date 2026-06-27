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
    private final CListDataObject obj;
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
        this.name = objDesc + ".list";
        obj = CDataObjectFactory.getCListDataObject(objDesc);
        p = obj.getCProperties();
        bNew = CButtonFactory.getButton("new");
        bNew.addActionListener(e -> onNew());
        bEdit = CButtonFactory.getButton("edit");
        bEdit.addActionListener(e -> onEdit());
        bDelete = CButtonFactory.getButton("delete");
        bDelete.addActionListener(e -> onDelete());
        bCopy = CButtonFactory.getButton("copy");
        bCopy.addActionListener(e -> onCopy());
        bDisplay = CButtonFactory.getButton("display");
        bDisplay.addActionListener(e -> onDisplay());
        bCancel = CButtonFactory.getButton("cancel");
        bCancel.addActionListener(e -> onCancel());

        addButton(bNew, p.get("bnew"));
        addButton(bEdit, p.get("bedit"));
        addButton(bDelete, p.get("bdelete"));
        addButton(bCopy, p.get("bcopy"));
        addButton(bDisplay, p.get("bdisplay"));

        getButtonPaneRight().add(bCancel);
        tab.setCListDataObject(obj);
        if (p.get("order") != null) {
            tab.setModel(obj.select(p));
        } else {
            tab.setModel(obj.select(1));
        }
        setStatusLine(tab.getRowCount() + " Eintr�ge");
        tab.setListParent(this);
        tab.setWidth(obj.getCProperties());
        tab.addMouseListener(new MouseAdapter() {
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
//		pack();
        setFrameSize();
        CPropertyManager.getInstance().setDialog(name, this);
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

    public void setColor(Color c) {
        super.setColor(c);
        if (searchBean != null) searchBean.setColor(c);
    }

    private void addButton(CButton button, Object option) {
        if (option == null || option.toString().isEmpty()) {
            getButtonPaneLeft().add(button);
            return;
        }
        if (option.toString().equalsIgnoreCase("DISABLED")) {
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
        if (p.get("bdisplay") == null || p.get("bdisplay").toString().equalsIgnoreCase("")) {
            new CInfoFrame(CInfoFrame.DISPLAY, getSelectedElement(), this);
        } else if (!p.get("bdisplay").toString().equalsIgnoreCase("NONE")
                && !p.get("bdisplay").toString().equalsIgnoreCase("DISABLED")) {
            CCommand command;
            try {
                command = newCommand(p.get("bdisplay").toString());
                command.setOwner(this);
                command.execute(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to execute bDisplay command", e);
            }
        }
    }

    private CProperties getSelectedElement() {
        CProperties p = new CProperties();
        p.put("object_name", objectName);
        p.put("keys", tab.getKeys());
        return p;
    }

    protected void onCopy() {
        if (p.get("bcopy") == null || p.get("bcopy").toString().equalsIgnoreCase("")) {
            new CInfoFrame(CInfoFrame.COPY, getSelectedElement(), this);
        } else if (!p.get("bcopy").toString().equalsIgnoreCase("NONE")
                && !p.get("bcopy").toString().equalsIgnoreCase("DISABLED")) {
            CCommand command;
            try {
                command = newCommand(p.get("bcopy").toString());
                command.setOwner(this);
                command.execute(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to execute bCopy command", e);
            }
        }
    }

    protected void onDelete() {
        if (p.get("bdelete") == null || p.get("bdelete").toString().equalsIgnoreCase("")) {
            new CInfoFrame(CInfoFrame.DELETE, getSelectedElement(), this);
        } else if (!p.get("bdelete").toString().equalsIgnoreCase("NONE")
                && !p.get("bdelete").toString().equalsIgnoreCase("DISABLED")) {
            CCommand command;
            try {
                command = newCommand(p.get("bdelete").toString());
                command.setOwner(this);
                command.execute(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to execute bDelete command", e);
            }
        }
    }

    public void onEdit() {
        if (p.get("bedit") == null || p.get("bedit").toString().equalsIgnoreCase("")) {
            new CInfoFrame(CInfoFrame.EDITALL, getSelectedElement(), this);
        } else if (!p.get("bedit").toString().equalsIgnoreCase("NONE")
                && !p.get("bedit").toString().equalsIgnoreCase("DISABLED")) {
            CCommand command;
            try {
                command = newCommand(p.get("bedit").toString());
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
                && !p.get("bnew").toString().equalsIgnoreCase("DISABLED")) {
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
        CProperties p = new CProperties();
        p.put("object_name", objectName);
//		p.put("keys",null);
        return p;
    }

    public void dispose() {
        try {
            Statement statement = ((CTableModel) tab.getModel()).getStatement();
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
//			e.printStackTrace();
        }
        CPropertyManager.getInstance().setDialog(name, null);
        super.dispose();
    }

    private CCommand newCommand(String className) throws ReflectiveOperationException {
        return (CCommand) Class.forName(className).getDeclaredConstructor().newInstance();
    }
}
