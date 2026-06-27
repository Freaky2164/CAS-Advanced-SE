package compucrash;

import javax.swing.*;

public class CButton extends JButton {

    public static final int RIGHT = 0;
    public static final int LEFT = 1;
    public static final int TOP = 2;
    public static final int BOTTOM = 3;

    private final String bez;

    private transient CCommand command;

    public CButton(CProperties p) {
        super((String) p.get("label"), (Icon) p.get("icon"));
        switch ((Integer) p.get("position")) {
            case LEFT:
                this.setHorizontalTextPosition(SwingConstants.LEFT);
                break;
            case TOP:
                this.setHorizontalTextPosition(SwingConstants.CENTER);
                this.setVerticalTextPosition(SwingConstants.TOP);
                break;
            case BOTTOM:
                this.setHorizontalTextPosition(SwingConstants.CENTER);
                this.setVerticalTextPosition(SwingConstants.BOTTOM);
                break;
            case RIGHT:
            default:
                this.setHorizontalTextPosition(SwingConstants.RIGHT);
        }
        this.setToolTipText((String) p.get("tooltip"));
        this.bez = (String) p.get("bez");
        if (p.get("mnemonic") != null) {
            setMnemonic(Integer.parseInt(p.get("mnemonic").toString()));
        }
        command = null;
        if (p.get("command") != null) {
            try {
                Class<? extends CCommand> c = Class.forName(p.get("command").toString()).asSubclass(CCommand.class);
                command = c.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                CMessage.print(e);
            }
        }
        addActionListener(_ -> {
            if (command != null) command.execute(null);
        });
    }

    public void setCCommand(CCommand command) {
        this.command = command;
    }


    public CCommand getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return bez;
    }


}
