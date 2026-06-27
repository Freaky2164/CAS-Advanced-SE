package compucrash;

public class CNull {

    // this class is needed to distinguish between no value and null database value
    //TODO testen, ob diese Klasse ersetzt werden kann
    public CNull() {
        // Default constructor intentionally empty; this class serves as a null marker type
    }

    public String toString() {
        return null;
    }
}
