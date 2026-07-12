package de.frauenhaus.service;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Bookmark;
import org.apache.poi.hwpf.usermodel.Bookmarks;
import org.apache.poi.hwpf.usermodel.Range;

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
 * @author Nils
 *
 * Befüllt die Word-Vorlagen (.dot) aus dem Vorlagen-Verzeichnis über ihre
 * Lesezeichen (ersetzt WordProcessing.typeTextAtBookmark aus dem Altsystem,
 * das Word per COM fernsteuerte – hier stattdessen POI-HWPF, läuft damit
 * auch headless im Container).
 */
final class DocumentCreationHelpers {

    private DocumentCreationHelpers() { }

    /**
     * Öffnet die Vorlage, setzt an jedem Lesezeichen den zugehörigen Wert aus
     * {@code werte} ein (Lesezeichen ohne Eintrag bleiben unverändert) und
     * liefert das Ergebnis als Word-Dokument (.doc).
     *
     * <p>Zeilenumbrüche im Wert ({@code \r}) erzeugen neue Absätze, Tabs bleiben
     * erhalten – so lassen sich Listen wie "Datum &lt;Tab&gt; Betrag" einsetzen.</p>
     */
    static byte[] fuelleVorlage(Path vorlage, Map<String, String> werte) {
        try (InputStream in = Files.newInputStream(vorlage)) {
            HWPFDocument doc = new HWPFDocument(in);

            // Von hinten nach vorne einsetzen, damit die Offsets der noch
            // nicht befüllten Lesezeichen durch das Einfügen gültig bleiben.
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
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Vorlage " + vorlage.getFileName() + " konnte nicht befüllt werden", e);
        }
    }

    /** Alle Lesezeichen des Dokuments, absteigend nach Startposition. */
    private static List<Bookmark> rueckwaertsSortiert(Bookmarks bookmarks) {
        List<Bookmark> alle = new ArrayList<>();
        for (int i = 0; i < bookmarks.getBookmarksCount(); i++) {
            alle.add(bookmarks.getBookmark(i));
        }
        alle.sort(Comparator.comparingInt(Bookmark::getStart).reversed());
        return alle;
    }
}
