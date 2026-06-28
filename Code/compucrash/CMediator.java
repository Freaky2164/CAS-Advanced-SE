package compucrash;

import java.util.HashMap;

public abstract class CMediator {
    // TODO implementieren?
    private final HashMap<String, CColleague> colleagues = HashMap.newHashMap(10);

    public void register(CColleague colleage) {
        colleagues.put(colleage.toString(), colleage);
    }

    public void unregister(CColleague colleage) {
        colleagues.remove(colleage.toString());
    }

    abstract void colleagueChanged(CColleague coleague);
}
