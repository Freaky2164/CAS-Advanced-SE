package compucrash;
public abstract class CCommand {

	public Object owner;
	
	public CCommand() {
		
	}
	
/*	public Object getOwner() {
		return owner;
	}*/
	
	public void setOwner(Object o) {
		this.owner = o;
	}
	abstract public Object execute(Object parameters);
}
