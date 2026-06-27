package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CInfoSearchBean extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(CInfoSearchBean.class.getName());
    private final JTextField search = new JTextField(60);
    private final JLabel searchLabel = new JLabel("Suchen   ");
    private final CInfoFrame owner;
    private final JComboBox<String> modes = new JComboBox<>();
    private final JPanel p1 = new JPanel();
    private final JPanel p2 = new JPanel();
    protected CButton bNew;
    protected CButton bEdit;
    protected CButton bDelete;
    protected CButton bCopy;
    protected CButton bDisplay;
    protected CProperties p;

    public CInfoSearchBean(CInfoFrame parent) {
        super();
        this.owner = parent;
        p = CDataObjectFactory.getCListDataObject(parent.object_name).getCProperties();
        p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Suchen"));
        p2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Modus"));
//		setLayout(new FlowLayout(FlowLayout.LEFT));
        setLayout(new BorderLayout());
        p1.add(search, BorderLayout.CENTER);
        p2.setPreferredSize(new Dimension(200, p2.getHeight()));
        search.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED) return;
                action(e);
            }
        });
        p2.add(modes, BorderLayout.EAST);
        add(p1, BorderLayout.CENTER);
        add(p2, BorderLayout.EAST);
        bNew = CButtonFactory.getButton("new");
        bNew.addActionListener(e -> bDo("bnew"));
        bEdit = CButtonFactory.getButton("edit");
        bEdit.addActionListener(e -> bDo("bedit"));
        bDelete = CButtonFactory.getButton("delete");
        bDelete.addActionListener(e -> bDo("bdelete"));
        bCopy = CButtonFactory.getButton("copy");
        bCopy.addActionListener(e -> bDo("bcopy"));
        bDisplay = CButtonFactory.getButton("display");
        bDisplay.addActionListener(e -> bDo("bdisplay"));

        modes.addItem("");
        addButton(bNew, p.get("bnew"));
        addButton(bEdit, p.get("bedit"));
        addButton(bDelete, p.get("bdelete"));
        addButton(bCopy, p.get("bcopy"));
        addButton(bDisplay, p.get("bdisplay"));

        modes.addActionListener(e -> bDo(modes.getSelectedItem().toString()));
    }

    protected void bDo(String whatis) {
        String what = "";
        // Umbenennung von Labeltext in internen Key
        if (bNew.getText().equalsIgnoreCase(whatis)) what = "bnew";
        if (bEdit.getText().equalsIgnoreCase(whatis)) what = "bedit";
        if (bDelete.getText().equalsIgnoreCase(whatis)) what = "bdelete";
        if (bCopy.getText().equalsIgnoreCase(whatis)) what = "bcopy";
        if (bDisplay.getText().equalsIgnoreCase(whatis)) what = "bdisplay";
        if (p.get(what) == null || p.get(what).toString().equalsIgnoreCase("")) {
            executeDefaultAction(what);
        } else if (!p.get(what).toString().equalsIgnoreCase("NONE")
                && !p.get(what).toString().equalsIgnoreCase("DISABLED")) {
            try {
                CCommand command = newCommand(p.get(what).toString());
                command.setOwner(this);
                command.execute(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to execute command", e);
            }
        }
    }

    private void executeDefaultAction(String what) {
        if (what.equalsIgnoreCase("bnew")) {
            owner.attributeValues.clear();
            for (CDisplayField f : owner.cFields) {
                owner.attributeValues.put(f.getName(), "init");
            }
            owner.p = new CProperties();
            owner.p.put("object_name", owner.object_name);
            owner.p.put("key", "");
            owner.status = new CInfoFrameStatusNew(owner);
        } else if (what.equalsIgnoreCase("bedit")) {
            owner.status = new CInfoFrameStatusEdit(owner);
        } else if (what.equalsIgnoreCase("bdelete")) {
            owner.status = new CInfoFrameStatusDelete(owner);
        } else if (what.equalsIgnoreCase("bcopy")) {
            owner.status = new CInfoFrameStatusCopy(owner);
        } else if (what.equalsIgnoreCase("bdisplay")) {
            owner.status = new CInfoFrameStatusDisplay(owner);
        }
    }

    private CCommand newCommand(String className) throws ReflectiveOperationException {
        return (CCommand) Class.forName(className).getDeclaredConstructor().newInstance();
    }

    private void addButton(CButton button, Object option) {
        if (option == null || option.toString().isEmpty()) {
            modes.addItem(button.getText());
            return;
        }
        if (option.toString().equalsIgnoreCase("DISABLED")) {
            // Inaktive Schaltfl�chen werden auch ausgeblendet
//			modes.addItem(button.getText());
            return;
        }
        if (option.toString().equalsIgnoreCase("NONE")) {
            return;
        }
        modes.addItem(button.getText());
    }

    protected void action(KeyEvent e) {
        String list = owner.object_name + ".list";
        CListFrame f = (CListFrame) CPropertyManager.getInstance().getDialog(list);
        if (f == null) {
            f = new CListFrame(owner.object_name, null);
        } else {
            f.toFront();
            f.setVisible(true);
        }
        f.getSearchBean().getTextField().setText(String.valueOf(e.getKeyChar()));
        f.getSearchBean().getTextField().requestFocus();
    }

}
