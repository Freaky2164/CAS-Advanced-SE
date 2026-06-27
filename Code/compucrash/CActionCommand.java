/*
 * Created on 06.01.2006
 */
package compucrash;

public abstract class CActionCommand extends CCommand {

    protected CActionCommand() {
        super();
    }

    public abstract Object execute(Object parameters);

    public abstract void executeChange(Object parameters);

}
