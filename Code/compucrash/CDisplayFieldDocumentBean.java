package compucrash;

import com.adobe.acrobat.Viewer;
import com.adobe.acrobat.ViewerCommand;

import javax.swing.*;
import java.awt.*;
import java.nio.*;
import java.io.*;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CDisplayFieldDocumentBean extends CDisplayFieldBean {

    private static final Logger LOGGER = Logger.getLogger(CDisplayFieldDocumentBean.class.getName());
    // \u00c4nderungen bei Dokumenten werden nicht registriert
    private final JLabel label = new JLabel();
    private final JPanel px = new JPanel();
    protected CButton bNew;
    protected CButton bDelete;
    protected CButton bPrint;
    private transient Viewer viewer;
    private transient InputStream stream = null;
    private transient Blob blob = null;

    public CDisplayFieldDocumentBean(CProperties p, CInfoFrame frame) {
        super(p, frame);
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(label);
        add(px);
        px.setLayout(new BorderLayout());
        try {
            viewer = new Viewer();
            viewer.activate();
            px.add(viewer.getFrame(), BorderLayout.NORTH);
        } catch (Exception e1) {
            LOGGER.log(Level.SEVERE, "Failed to activate PDF viewer", e1);
        }

        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                (String) p.get("label"))
        );

        if (p.get("tooltip") != null) {
            setToolTipText((String) p.get("tooltip"));
        }
        viewer.setPreferredSize(new Dimension(Integer.parseInt((String) p.get("data_scale")) * 15, 45));
        if (Integer.parseInt((String) p.get("data_height")) != 0) {
            viewer.setPreferredSize(new Dimension(Integer.parseInt((String) p.get("data_scale")) * 15,
                    Integer.parseInt((String) p.get("data_height")) * 21));
        }
        bNew = CButtonFactory.getButton("new");
        bNew.addActionListener(e -> onNew());
        bDelete = CButtonFactory.getButton("delete");
        bDelete.addActionListener(e -> onDelete());
        bPrint = CButtonFactory.getButton("print");
        bPrint.addActionListener(e -> onPrint());
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

    @Override
    public void setEditedColor() {
        // TODO document why this method is empty
    }

    @Override
    public void resetEditedColor() {
        // TODO document why this method is empty
    }

    protected void onPrint() {
        if (stream == null) {
            return;
        }
        try {
            byte[] b = new byte[stream.available()];
            int k = stream.read(b);
            if (k>=0) {
                File fout = new File("out.pdf");

                try (FileOutputStream foutstr = new FileOutputStream(fout)) {
                    foutstr.write(b);
                }
                String acrobatPath = "";
                if (CPropertyManager.getInstance().getProperty("acrobat") != null) {
                    acrobatPath = CPropertyManager.getInstance().getProperty("acrobat");
                }
            }
            Process p;
            if (acrobatPath.isEmpty()) {
                p = new ProcessBuilder("acrord32", fout.getAbsolutePath()).start();
            } else {
                p = new ProcessBuilder(acrobatPath + "acrord32", fout.getAbsolutePath()).start();
            }
            p.waitFor();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to print document", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Print process interrupted", e);
        }
        boolean deleted;
        deleted = new File("out.pdf").delete();
        if (deleted) {
            /*Shouldn't do anything if the deletion worked*/
        } else {
            LOGGER.log(Level.WARNING, "Failed to delete temporary PDF file");
        }
    }

    protected void onDelete() {
        stream = null;
        try {
            FileInputStream fin = new FileInputStream("../images/nada_nix.pdf");
            viewer.setDocumentInputStream(fin);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to reset document viewer", e);
        }
    }

    protected void onNew() {
        JFileChooser chooser = new JFileChooser();
        chooser.addChoosableFileFilter(new CPdfFileFilter());
        Object importdir = CPropertyManager.getInstance().getProperties().get("importdir");
        if (importdir != null) {
            chooser.setCurrentDirectory(new File(importdir.toString()));
        }
        int state = chooser.showOpenDialog(null);
        File fin = chooser.getSelectedFile();
        if (fin != null && state == JFileChooser.APPROVE_OPTION) {
            try {
                stream = new FileInputStream(fin);
                viewer.setDocumentInputStream(new FileInputStream(fin));
                viewer.execMenuItem(ViewerCommand.FitWidth_K);
                viewer.execMenuItem(ViewerCommand.OneColumn_K);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to open document", e);
            }
        }
    }

    public void setEditable(int i) {
        // Editability is managed by the document viewer buttons (bNew, bDelete, bPrint)
    }

    public Object getValue() {
        try {
            if (stream == null) {
                return null;
            }
            byte[] b = new byte[stream.available()];
            int k = stream.read(b);
            if (k>=0) {
                if (blob == null) {
                    return b;
                } else {
                    blob.setBinaryStream(1).write(b);
                    return blob;
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read document stream", e);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to write to blob", e);
        }
        return null;
    }

    public void setValue(Object o) {
        switch (o) {
            case Blob blob1 -> {
                try {
                    blob = blob1;
                    stream = blob.getBinaryStream();
                    viewer.setDocumentInputStream(blob.getBinaryStream());
                    viewer.execMenuItem(ViewerCommand.FitWidth_K);
                    viewer.execMenuItem(ViewerCommand.OneColumn_K);
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to read blob stream", e);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to set document from blob", e);
                }
            }
            case null, default -> {
                try {
                    FileInputStream fin = new FileInputStream("../images/nada_nix.pdf");
                    viewer.setDocumentInputStream(fin);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load default document", e);
                }
            }
        }
    }
}
