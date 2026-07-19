package de.frauenhaus.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Wandelt Geldbeträge in Worte um, wie es Spendenquittungen als Pflichtangabe
 * verlangen.
 *
 * @author Nils
 */
public final class BetragInWorten {

    private static final String[] EINER = {"", "ein", "zwei", "drei", "vier", "fünf",
            "sechs", "sieben", "acht", "neun", "zehn", "elf", "zwölf", "dreizehn",
            "vierzehn", "fünfzehn", "sechzehn", "siebzehn", "achtzehn", "neunzehn"};
    private static final String[] ZEHNER = {"", "", "zwanzig", "dreißig", "vierzig",
            "fünfzig", "sechzig", "siebzig", "achtzig", "neunzig"};

    /** Verhindert Instanziierung der Utility-Klasse. */
    private BetragInWorten() { }

    /**
     * Liefert das Zahlwort ohne Währungsangabe – für Vorlagen, die das Wort
     * "Euro" bereits enthalten, z.B. {@code 50.05} ->
     * {@code "*fünfzig Komma null fünf*"}.
     *
     * @param betrag der umzuwandelnde Betrag
     * @return das Zahlwort in Sternchen eingefasst
     */
    public static String ohneWaehrung(BigDecimal betrag) {
        BigDecimal gerundet = betrag.setScale(2, RoundingMode.HALF_UP);
        long euro = gerundet.longValue();
        int cent = gerundet.remainder(BigDecimal.ONE).movePointRight(2).intValue();

        StringBuilder sb = new StringBuilder("*");
        sb.append(euro == 0 ? "null" : zahlInWorten(euro));
        if (cent > 0) {
            sb.append(" Komma ");
            if (cent < 10) {
                sb.append("null ");
            }
            sb.append(zahlInWorten(cent));
        }
        return sb.append('*').toString();
    }

    /**
     * Wandelt einen Geldbetrag in die für Spendenquittungen übliche Textform
     * um, z.B. {@code 50.05} -> {@code "*fünfzig Euro und fünf Cent*"}.
     *
     * @param betrag der umzuwandelnde Betrag
     * @return der Betrag in Worten, in Sternchen eingefasst
     */
    public static String von(BigDecimal betrag) {
        BigDecimal gerundet = betrag.setScale(2, RoundingMode.HALF_UP);
        long euro = gerundet.longValue();
        int cent = gerundet.remainder(BigDecimal.ONE).movePointRight(2).intValue();

        StringBuilder sb = new StringBuilder("*");
        sb.append(euro == 0 ? "null" : zahlInWorten(euro));
        sb.append(" Euro");
        if (cent > 0) {
            sb.append(" und ").append(zahlInWorten(cent)).append(" Cent");
        }
        return sb.append('*').toString();
    }

    /**
     * Wandelt eine nichtnegative Ganzzahl rekursiv in deutsche Zahlwörter um.
     */
    private static String zahlInWorten(long n) {
        if (n < 20) {
            return n != 1 ? EINER[(int) n] : "eins";
        }
        if (n < 100) {
            int e = (int) (n % 10);
            String prefix = e > 0 ? EINER[e] + "und" : "";
            return prefix + ZEHNER[(int) (n / 10)];
        }
        if (n < 1_000) {
            long rest = n % 100;
            return EINER[(int) (n / 100)] + "hundert" + (rest > 0 ? zahlInWorten(rest) : "");
        }
        if (n < 1_000_000) {
            long rest = n % 1_000;
            return alsPrefix(zahlInWorten(n / 1_000)) + "tausend" + (rest > 0 ? zahlInWorten(rest) : "");
        }
        long rest = n % 1_000_000;
        long mio = n / 1_000_000;
        String mioText = mio == 1 ? "eine Million " : alsPrefix(zahlInWorten(mio)) + " Millionen ";
        return (mioText + (rest > 0 ? zahlInWorten(rest) : "")).trim();
    }

    /**
     * Wandelt ein alleinstehendes "eins" in den Multiplikator "ein" um
     * (eintausend, einhundert).
     */
    private static String alsPrefix(String zahlwort) {
        return zahlwort.endsWith("eins")
                ? zahlwort.substring(0, zahlwort.length() - 1)
                : zahlwort;
    }
}
