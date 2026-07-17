package de.frauenhaus.ui.support;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * @author Nils
 *
 * Kleine UI-Helfer: Erfolgs-/Fehlermeldungen, Download-Links auf Service-Ergebnisse
 * und Formatierung der in den Grids angezeigten Werte.
 */
public final class UiUtil {

    public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String DOC = "application/msword";
    public static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter ZEITPUNKT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    private UiUtil() {
    }

    /**
     * @author Nils
     *
     * Datei-Inhalt eines Downloads: Name, Content-Type und Bytes.
     */
    public record Datei(String name, String contentType, byte[] inhalt) {
    }

    public static void erfolg(String text) {
        Notification n = Notification.show(text, 4000, Notification.Position.BOTTOM_START);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    /**
     * @author Nils
     *
     * Zeigt die Fehlermeldung einer fehlgeschlagenen Service-Operation an;
     * bei {@link ResponseStatusException} wird der deutsche Grund angezeigt.
     */
    public static void fehler(Exception e) {
        String text = e instanceof ResponseStatusException rse && rse.getReason() != null
                ? rse.getReason()
                : "Unerwarteter Fehler: " + e.getMessage();
        Notification n = Notification.show(text, 6000, Notification.Position.BOTTOM_START);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    /**
     * @author Nils
     *
     * Download-Link, der die Datei erst beim Klick über den gegebenen Lieferanten
     * (typisch: ein Report-Service) erzeugt.
     */
    public static Anchor downloadLink(String beschriftung, SerializableSupplier<Datei> lieferant) {
        DownloadHandler handler = DownloadHandler.fromInputStream(event -> {
            try {
                Datei datei = lieferant.get();
                return new DownloadResponse(new ByteArrayInputStream(datei.inhalt()),
                        datei.name(), datei.contentType(), datei.inhalt().length);
            } catch (ResponseStatusException e) {
                return DownloadResponse.error(e.getStatusCode().value());
            }
        });
        Anchor anchor = new Anchor(handler, beschriftung);
        anchor.getElement().setAttribute("download", true);
        return anchor;
    }

    /**
     * @author Nils
     *
     * Zerlegt eine Komma-getrennte Eingabe (z.B. Stichworte) in eine bereinigte Liste.
     */
    public static List<String> kommaListe(String eingabe) {
        if (eingabe == null || eingabe.isBlank()) {
            return List.of();
        }
        return Arrays.stream(eingabe.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static String datum(LocalDate datum) {
        return datum == null ? "" : DATUM.format(datum);
    }

    public static String zeitpunkt(Instant instant) {
        return instant == null ? "" : ZEITPUNKT.format(instant.atZone(ZONE));
    }

    public static String zeitpunkt(OffsetDateTime zeitpunkt) {
        return zeitpunkt == null ? "" : ZEITPUNKT.format(zeitpunkt.atZoneSameInstant(ZONE));
    }

    public static String betrag(BigDecimal betrag) {
        return betrag == null ? "" : String.format("%,.2f €", betrag);
    }

    public static String groesse(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
