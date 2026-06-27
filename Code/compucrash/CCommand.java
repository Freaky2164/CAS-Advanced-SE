package compucrash;

public abstract class CCommand {

    private Object owner;

    protected CCommand() {

    }

    public Object getOwner() {
        return owner;
    }

    public void setOwner(Object o) {
        this.owner = o;
    }

    public abstract Object execute(Object parameters);
}
