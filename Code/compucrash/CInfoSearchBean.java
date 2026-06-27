package compucrash;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CInfoSearchBean extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(CInfoSearchBean.class.getName());
    private final CInfoFrame owner;
    private final JComboBox<String> modes = new JComboBox<>();
    protected CButton bNew;
    protected CButton bEdit;
    protected CButton bDelete;
    protected CButton bCopy;
    protected CButton bDisplay;
    protected CProperties p;
    private static final String BDISPLAYCONST = "bdisplay";
    private static final String BEDITCONST = "bedit";
    private static final String BDELETECONST = "bdelete";
    private static final String BCOPYCONST = "bcopy";
    private static final String BNEWCONST = "bnew";

    public CInfoSearchBean(CInfoFrame parent) {
        super();
        this.owner = parent;
        p = CDataObjectFactory.getCListDataObject(parent.objectName).getCProperties();
        JPanel p1 = new JPanel();
        p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Suchen"));
        JPanel p2 = new JPanel();
        p2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Modus"));
        setLayout(new BorderLayout());
        JTextField search = new JTextField(60);
        p1.add(search, BorderLayout.CENTER);
        p2.setPreferredSize(new Dimension(200, p2.getHeight()));
        search.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED) return;
                action(e);
            }
        });
        p2.add(modes, BorderLayout.EAST);
        add(p1, BorderLayout.CENTER);
        add(p2, BorderLayout.EAST);
        bNew = CButtonFactory.getButton("new");
        bNew.addActionListener(_ -> bDo("bnew"));
        bEdit = CButtonFactory.getButton("edit");
        bEdit.addActionListener(_ -> bDo(BEDITCONST));
        bDelete = CButtonFactory.getButton("delete");
        bDelete.addActionListener(_ -> bDo(BDELETECONST));
        bCopy = CButtonFactory.getButton("copy");
        bCopy.addActionListener(_ -> bDo(BCOPYCONST));
        bDisplay = CButtonFactory.getButton("display");
        bDisplay.addActionListener(_ -> bDo(BDISPLAYCONST));

        modes.addItem("");
        addButton(bNew, p.get("bnew"));
        addButton(bEdit, p.get(BEDITCONST));
        addButton(bDelete, p.get(BDELETECONST));
        addButton(bCopy, p.get(BCOPYCONST));
        addButton(bDisplay, p.get(BDISPLAYCONST));

        modes.addActionListener(_ -> bDo(Objects.requireNonNull(modes.getSelectedItem()).toString()));
    }

    protected void bDo(String whatis) {
        String what = "";
        // Umbenennung von Labeltext in internen Key
        if (bNew.getText().equalsIgnoreCase(whatis)) what = BNEWCONST;
        if (bEdit.getText().equalsIgnoreCase(whatis)) what = BEDITCONST;
        if (bDelete.getText().equalsIgnoreCase(whatis)) what = BDELETECONST;
        if (bCopy.getText().equalsIgnoreCase(whatis)) what = BCOPYCONST;
        if (bDisplay.getText().equalsIgnoreCase(whatis)) what = BDISPLAYCONST;
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
            owner.getAttributeValues().clear();
            for (CDisplayField f : owner.getcFields()) {
                owner.getAttributeValues().put(f.getName(), "init");
            }
            owner.p = new CProperties();
            owner.p.put("objectName", owner.objectName);
            owner.p.put("key", "");
            owner.setStatus(new CInfoFrameStatusNew(owner));
        } else if (what.equalsIgnoreCase(BEDITCONST)) {
            owner.setStatus(new CInfoFrameStatusEdit(owner));
        } else if (what.equalsIgnoreCase(BDELETECONST)) {
            owner.setStatus(new CInfoFrameStatusDelete(owner));
        } else if (what.equalsIgnoreCase(BCOPYCONST)) {
            owner.setStatus(new CInfoFrameStatusCopy(owner));
        } else if (what.equalsIgnoreCase(BDISPLAYCONST)) {
            owner.setStatus(new CInfoFrameStatusDisplay(owner));
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
            return;
        }
        if (option.toString().equalsIgnoreCase("NONE")) {
            return;
        }
        modes.addItem(button.getText());
    }

    protected void action(KeyEvent e) {
        String list = owner.objectName + ".list";
        CListFrame f = (CListFrame) CPropertyManager.getInstance().getDialog(list);
        if (f == null) {
            f = new CListFrame(owner.objectName, null);
        } else {
            f.toFront();
            f.setVisible(true);
        }
        f.getSearchBean().getTextField().setText(String.valueOf(e.getKeyChar()));
        f.getSearchBean().getTextField().requestFocus();
    }

}
