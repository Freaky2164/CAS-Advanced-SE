package de.frauenhaus.service;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.PAPX;
import org.apache.poi.hwpf.sprm.SprmBuffer;
import org.apache.poi.hwpf.usermodel.Bookmark;
import org.apache.poi.hwpf.usermodel.Bookmarks;
import org.apache.poi.hwpf.usermodel.Range;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Befüllt die Word-Vorlagen (.dot) aus dem Vorlagen-Verzeichnis über ihre
 * Lesezeichen mit POI-HWPF.
 *
 * @author Nils
 */
final class DocumentCreationHelpers {

    /** Verhindert Instanziierung der Utility-Klasse. */
    private DocumentCreationHelpers() { }

    /**
     * Öffnet die Vorlage, setzt an jedem Lesezeichen den zugehörigen Wert aus
     * {@code werte} ein (Lesezeichen ohne Eintrag bleiben unverändert) und
     * liefert das Ergebnis als Word-Dokument (.doc). Eingesetzt wird von hinten
     * nach vorne, damit die Offsets der noch nicht befüllten Lesezeichen durch
     * das Einfügen gültig bleiben.
     *
     * <p>Zeilenumbrüche im Wert ({@code \r}) erzeugen neue Absätze, Tabs bleiben
     * erhalten – so lassen sich Listen wie "Datum &lt;Tab&gt; Betrag" einsetzen.
     * Die neuen Absätze übernehmen die Absatzformatierung des Lesezeichen-Absatzes,
     * Aufzählungen (Bulletpoints/Nummerierungen) werden also je Zeile weitergeführt.</p>
     *
     * @param vorlage der Pfad zur .dot-Vorlage
     * @param werte die Werte je Lesezeichen-Name
     * @return das befüllte Dokument als .doc-Bytes
     */
    static byte[] fuelleVorlage(Path vorlage, Map<String, String> werte) {
        try (InputStream in = Files.newInputStream(vorlage)) {
            HWPFDocument doc = new HWPFDocument(in);

            List<int[]> mehrzeilig = new ArrayList<>();
            for (Bookmark bookmark : rueckwaertsSortiert(doc.getBookmarks())) {
                String wert = werte.get(bookmark.getName());
                if (wert == null || wert.isEmpty()) {
                    continue;
                }
                Range range = new Range(bookmark.getStart(), bookmark.getEnd(), doc);
                if (bookmark.getStart() < bookmark.getEnd()) {
                    range.replaceText(wert, false);
                } else {
                    range.insertBefore(wert);
                }
                for (int[] bereich : mehrzeilig) {
                    bereich[0] += wert.length();
                    bereich[1] += wert.length();
                }
                if (wert.indexOf('\r') >= 0) {
                    mehrzeilig.add(new int[]{bookmark.getStart(), bookmark.getStart() + wert.length()});
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            if (mehrzeilig.isEmpty()) {
                return out.toByteArray();
            }
            return absatzformateWeiterfuehren(out.toByteArray(), mehrzeilig);
        } catch (IOException e) {
            throw new UncheckedIOException("Vorlage " + vorlage.getFileName() + " konnte nicht befüllt werden", e);
        }
    }

    /**
     * Überträgt die Absatzformatierung des Lesezeichen-Absatzes auf alle durch
     * mehrzeiliges Einfügen entstandenen Absätze. Im Word-Binärformat hängen die
     * Absatz-Eigenschaften (PAPX) an der Absatzmarke; neu eingefügte Absatzmarken
     * haben keine und fielen sonst auf das Standardformat zurück – Aufzählungen
     * würden nicht weitergeführt. Beim erneuten Einlesen legt POI für diese
     * Absätze Default-Einträge an, die hier durch Klone des Originals ersetzt
     * werden (die Original-Formatierung deckt die letzte eingefügte Zeile ab).
     *
     * @param dokument das serialisierte Dokument
     * @param mehrzeilig die Start-/End-Offsets der mehrzeilig befüllten Bereiche
     * @return das Dokument mit weitergeführten Absatzformaten
     */
    private static byte[] absatzformateWeiterfuehren(byte[] dokument, List<int[]> mehrzeilig) throws IOException {
        HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(dokument));
        List<PAPX> absatzformate = doc.getParagraphTable().getParagraphs();

        for (int[] bereich : mehrzeilig) {
            SprmBuffer original = null;
            for (PAPX papx : absatzformate) {
                if (papx.getStart() <= bereich[1] && bereich[1] < papx.getEnd()) {
                    original = papx.getSprmBuf();
                    break;
                }
            }
            if (original == null) {
                continue;
            }
            for (int i = 0; i < absatzformate.size(); i++) {
                PAPX papx = absatzformate.get(i);
                if (papx.getStart() >= bereich[0] && papx.getEnd() <= bereich[1]) {
                    absatzformate.set(i, new PAPX(papx.getStart(), papx.getEnd(), new SprmBuffer(original)));
                }
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        return out.toByteArray();
    }

    /**
     * Liefert alle Lesezeichen des Dokuments, absteigend nach Startposition.
     *
     * @param bookmarks die Lesezeichen des Dokuments
     * @return die Lesezeichen absteigend sortiert
     */
    private static List<Bookmark> rueckwaertsSortiert(Bookmarks bookmarks) {
        List<Bookmark> alle = new ArrayList<>();
        for (int i = 0; i < bookmarks.getBookmarksCount(); i++) {
            alle.add(bookmarks.getBookmark(i));
        }
        alle.sort(Comparator.comparingInt(Bookmark::getStart).reversed());
        return alle;
    }
}
