package de.frauenhaus.service;

import de.frauenhaus.domain.Dokument;
import de.frauenhaus.repository.BussgeldRepository;
import de.frauenhaus.repository.DokumentRepository;
import de.frauenhaus.repository.GerichtRepository;
import de.frauenhaus.repository.MitgliedRepository;
import de.frauenhaus.repository.SpendeRepository;
import de.frauenhaus.repository.VereinRepository;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.LongPredicate;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

/**
 * Verwaltung der Dokument-Anhänge zu Stammdaten inklusive Upload, Download
 * und Validierung der erlaubten Ziel-Entitäten.
 *
 * @author Paul
 */
@Service
@Transactional
public class DokumentService {

    /** Maximal erlaubte Dateigröße in Bytes (10 MB). */
    static final long MAX_DATEIGROESSE = 10L * 1024 * 1024;

    /**
     * Metadaten eines Dokuments ohne den Dateiinhalt.
     */
    public record DokumentMetadaten(
            Long id, Dokument.EntityTyp entityTyp, String entityId, String dateiname, String contentType,
            long groesse, OffsetDateTime hochgeladenAm, String hochgeladenVon) {

        /** Bildet ein {@link Dokument} auf die Metadaten-Darstellung ab. */
        static DokumentMetadaten of(Dokument d) {
            return new DokumentMetadaten(d.getId(), d.getEntityTyp(), d.getEntityId(), d.getDateiname(),
                    d.getContentType(), d.getGroesse(), d.getHochgeladenAm(), d.getHochgeladenVon());
        }

        /** Bildet eine Metadaten-Projektion auf die Metadaten-Darstellung ab. */
        static DokumentMetadaten of(DokumentRepository.DokumentMetadatenProjection d) {
            return new DokumentMetadaten(d.getId(), d.getEntityTyp(), d.getEntityId(), d.getDateiname(),
                    d.getContentType(), d.getGroesse(), d.getHochgeladenAm(), d.getHochgeladenVon());
        }
    }

    /**
     * Download-Darstellung eines Dokuments mit Dateiname, MIME-Typ und Inhalt.
     */
    public record DokumentDownload(String dateiname, String contentType, byte[] inhalt) {

        /** Vergleicht auch den Inhalt des Byte-Arrays. */
        @Override
        public boolean equals(Object o) {
            return o instanceof DokumentDownload other
                    && Objects.equals(dateiname, other.dateiname)
                    && Objects.equals(contentType, other.contentType)
                    && Arrays.equals(inhalt, other.inhalt);
        }

        /** Bezieht den Inhalt des Byte-Arrays in den Hashwert ein. */
        @Override
        public int hashCode() {
            return Objects.hash(dateiname, contentType, Arrays.hashCode(inhalt));
        }

        /** Gibt statt des Array-Inhalts nur die Dateigröße aus. */
        @Override
        public String toString() {
            return "DokumentDownload[dateiname=" + dateiname + ", contentType=" + contentType
                    + ", groesse=" + (inhalt == null ? 0 : inhalt.length) + "]";
        }
    }

    private final DokumentRepository dokumente;
    private final MitgliedRepository mitglieder;
    private final VereinRepository vereine;
    private final BussgeldRepository bussgelder;
    private final SpendeRepository spenden;
    private final GerichtRepository gerichte;

    /**
     * Erzeugt den Service mit den Repositories aller Entitäten, an die
     * Dokumente angehängt werden können.
     *
     * @param dokumente das Dokument-Repository
     * @param mitglieder das Mitglieder-Repository
     * @param vereine das Verein-Repository
     * @param bussgelder das Bußgeld-Repository
     * @param spenden das Spenden-Repository
     * @param gerichte das Gericht-Repository
     */
    public DokumentService(DokumentRepository dokumente, MitgliedRepository mitglieder,
                           VereinRepository vereine, BussgeldRepository bussgelder,
                           SpendeRepository spenden, GerichtRepository gerichte) {
        this.dokumente = dokumente;
        this.mitglieder = mitglieder;
        this.vereine = vereine;
        this.bussgelder = bussgelder;
        this.spenden = spenden;
        this.gerichte = gerichte;
    }

    /**
     * Lädt eine Multipart-Datei als Dokument-Anhang hoch.
     *
     * @param entityTyp der Typ des Ziel-Stammdatensatzes
     * @param entityId die ID des Ziel-Stammdatensatzes
     * @param datei die hochgeladene Datei
     * @return die Metadaten des gespeicherten Dokuments
     */
    public DokumentMetadaten hochladen(String entityTyp, String entityId, MultipartFile datei) {
        pruefeDatei(datei);
        try {
            return hochladen(entityTyp, entityId, dateiname(datei), contentType(datei), datei.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Datei konnte nicht gelesen werden", e);
        }
    }

    /**
     * Upload-Variante für das Vaadin-UI, das die Datei bereits als Byte-Array
     * vorliegen hat.
     *
     * @param entityTyp der Typ des Ziel-Stammdatensatzes
     * @param entityId die ID des Ziel-Stammdatensatzes
     * @param dateiname der Dateiname
     * @param contentType der MIME-Typ des Inhalts
     * @param inhalt der Dateiinhalt
     * @return die Metadaten des gespeicherten Dokuments
     */
    public DokumentMetadaten hochladen(String entityTyp, String entityId,
                                       String dateiname, String contentType, byte[] inhalt) {
        Dokument.EntityTyp typ = pruefeEntityTyp(entityTyp);
        String normalisierteEntityId = pruefeEntityId(typ, entityId);
        if (inhalt == null || inhalt.length == 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Bitte eine Datei auswählen");
        }
        if (inhalt.length > MAX_DATEIGROESSE) {
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "Datei ist zu groß – maximal 10 MB");
        }
        if (!StringUtils.hasText(dateiname)) {
            throw new ResponseStatusException(BAD_REQUEST, "Dateiname fehlt");
        }

        Dokument dokument = new Dokument();
        dokument.setEntityTyp(typ);
        dokument.setEntityId(normalisierteEntityId);
        dokument.setDateiname(dateiname);
        dokument.setContentType(StringUtils.hasText(contentType) ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        dokument.setGroesse(inhalt.length);
        dokument.setHochgeladenAm(OffsetDateTime.now(ZoneId.systemDefault()));
        dokument.setHochgeladenVon(aktuellerBenutzer());
        dokument.setInhalt(inhalt);
        return DokumentMetadaten.of(dokumente.save(dokument));
    }

    /**
     * Liefert die Metadaten aller Dokumente eines Stammdatensatzes, neueste
     * zuerst.
     *
     * @param entityTyp der Typ des Stammdatensatzes
     * @param entityId die ID des Stammdatensatzes
     * @return die Dokument-Metadaten
     */
    @Transactional(readOnly = true)
    public List<DokumentMetadaten> liste(String entityTyp, String entityId) {
        Dokument.EntityTyp typ = pruefeEntityTyp(entityTyp);
        String normalisierteEntityId = pruefeEntityId(typ, entityId);
        return dokumente.findMetadatenByEntityTypAndEntityIdOrderByHochgeladenAmDesc(typ, normalisierteEntityId).stream()
                .map(DokumentMetadaten::of)
                .toList();
    }

    /**
     * Lädt ein Dokument mit Inhalt zum Download.
     *
     * @param id die ID des Dokuments
     * @return das Dokument mit Dateiname, MIME-Typ und Inhalt
     */
    @Transactional(readOnly = true)
    public DokumentDownload herunterladen(Long id) {
        Dokument dokument = holen(id);
        return new DokumentDownload(dokument.getDateiname(), dokument.getContentType(), dokument.getInhalt());
    }

    /**
     * Löscht ein Dokument.
     *
     * @param id die ID des Dokuments
     */
    public void loeschen(Long id) {
        if (!dokumente.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Dokument " + id + " nicht gefunden");
        }
        dokumente.deleteById(id);
    }

    /**
     * Löscht alle Dokumente eines Stammdatensatzes.
     *
     * @param entityTyp der Typ des Stammdatensatzes
     * @param entityId die ID des Stammdatensatzes
     */
    public void loescheFuerEntity(Dokument.EntityTyp entityTyp, String entityId) {
        dokumente.deleteByEntityTypAndEntityId(entityTyp, entityId);
    }

    /**
     * Lädt ein Dokument oder wirft 404, wenn es nicht existiert.
     */
    private Dokument holen(Long id) {
        return dokumente.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Dokument " + id + " nicht gefunden"));
    }

    /**
     * Prüft Vorhandensein und Größe der hochgeladenen Datei.
     */
    private static void pruefeDatei(MultipartFile datei) {
        if (datei == null || datei.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Bitte eine Datei auswählen");
        }
        if (datei.getSize() > MAX_DATEIGROESSE) {
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "Datei ist zu groß – maximal 10 MB");
        }
    }

    /**
     * Wandelt den Entity-Typ-Parameter in den Enum-Wert um; ungültige Werte
     * führen zu 400.
     */
    private Dokument.EntityTyp pruefeEntityTyp(String entityTyp) {
        if (!StringUtils.hasText(entityTyp)) {
            throw new ResponseStatusException(BAD_REQUEST, "entityTyp darf nicht leer sein");
        }
        try {
            return Dokument.EntityTyp.valueOf(entityTyp.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException _) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Ungültiger entityTyp '" + entityTyp + "'. Erlaubt: MITGLIED, VEREIN, BUSSGELD, SPENDE, GERICHT");
        }
    }

    /**
     * Validiert die Entity-ID gegen den jeweiligen Datenbestand und liefert
     * die normalisierte ID.
     */
    private String pruefeEntityId(Dokument.EntityTyp entityTyp, String entityId) {
        if (!StringUtils.hasText(entityId)) {
            throw new ResponseStatusException(BAD_REQUEST, "entityId darf nicht leer sein");
        }
        return switch (entityTyp) {
            case MITGLIED -> pruefeLongId("Mitglied", entityId, mitglieder::existsById);
            case BUSSGELD -> pruefeLongId("Bußgeld", entityId, bussgelder::existsById);
            case SPENDE -> pruefeLongId("Spende", entityId, spenden::existsById);
            case GERICHT -> pruefeLongId("Gericht", entityId, gerichte::existsById);
            case VEREIN -> pruefeVereinId(entityId);
        };
    }

    /**
     * Prüft eine numerische Entity-ID auf Format und Existenz.
     */
    private String pruefeLongId(String typ, String entityId, LongPredicate exists) {
        long id;
        try {
            id = Long.parseLong(entityId.trim());
        } catch (NumberFormatException _) {
            throw new ResponseStatusException(BAD_REQUEST, typ + "-ID '" + entityId + "' ist ungültig");
        }
        if (!exists.test(id)) {
            throw new ResponseStatusException(NOT_FOUND, typ + " " + id + " nicht gefunden");
        }
        return Long.toString(id);
    }

    /**
     * Prüft den Vereinsnamen auf Existenz.
     */
    private String pruefeVereinId(String entityId) {
        String name = entityId.trim();
        if (!vereine.existsById(name)) {
            throw new ResponseStatusException(NOT_FOUND, "Verein '" + name + "' nicht gefunden");
        }
        return name;
    }

    /**
     * Extrahiert den reinen Dateinamen ohne Pfadanteile aus dem Upload.
     */
    private static String dateiname(MultipartFile datei) {
        String original = datei.getOriginalFilename();
        String dateiname = StringUtils.cleanPath(original == null ? "" : original);
        dateiname = dateiname.replace('\\', '/');
        if (dateiname.contains("/")) {
            dateiname = dateiname.substring(dateiname.lastIndexOf('/') + 1);
        }
        if (!StringUtils.hasText(dateiname)) {
            throw new ResponseStatusException(BAD_REQUEST, "Dateiname fehlt");
        }
        return dateiname;
    }

    /**
     * Liefert den MIME-Typ des Uploads oder application/octet-stream.
     */
    private static String contentType(MultipartFile datei) {
        return StringUtils.hasText(datei.getContentType()) ? datei.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    /**
     * Liefert den Benutzernamen des angemeldeten Benutzers oder "system".
     */
    private static String aktuellerBenutzer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !auth.getName().isBlank() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName()
                : "system";
    }
}
