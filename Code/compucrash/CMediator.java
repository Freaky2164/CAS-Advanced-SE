package compucrash;
import java.util.Hashtable;

public abstract class CMediator {
    // TODO implementieren?
	private Hashtable colleagues = new Hashtable(10);

	public void register(CColleague colleage) {
		colleagues.put(colleage.toString(), colleage);
	}
	
	public void unregister (CColleague colleage) {
		colleagues.remove(colleage.toString());
	}

	abstract void colleagueChanged( CColleague coleague );
}
