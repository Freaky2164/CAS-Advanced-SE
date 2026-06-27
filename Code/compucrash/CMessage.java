package compucrash;

public class CMessage {

    public static void print(Object o) {
        if (CPropertyManager.DEBUG) System.out.println(o);
    }
}
