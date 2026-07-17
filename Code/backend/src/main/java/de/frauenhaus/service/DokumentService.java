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
import java.util.List;
import java.util.Locale;
import java.util.function.LongPredicate;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

/**
 * @author Nils
 *
 * Verwaltung der Dokument-Anhänge zu Stammdaten inkl. Upload/Download und
 * Validierung der erlaubten Ziel-Entitäten.
 */
@Service
@Transactional
public class DokumentService {

    static final long MAX_DATEIGROESSE = 10L * 1024 * 1024;

    public record DokumentMetadaten(
            Long id, Dokument.EntityTyp entityTyp, String entityId, String dateiname, String contentType,
            long groesse, OffsetDateTime hochgeladenAm, String hochgeladenVon) {

        static DokumentMetadaten of(Dokument d) {
            return new DokumentMetadaten(d.getId(), d.getEntityTyp(), d.getEntityId(), d.getDateiname(),
                    d.getContentType(), d.getGroesse(), d.getHochgeladenAm(), d.getHochgeladenVon());
        }

        static DokumentMetadaten of(DokumentRepository.DokumentMetadatenProjection d) {
            return new DokumentMetadaten(d.getId(), d.getEntityTyp(), d.getEntityId(), d.getDateiname(),
                    d.getContentType(), d.getGroesse(), d.getHochgeladenAm(), d.getHochgeladenVon());
        }
    }

    public record DokumentDownload(String dateiname, String contentType, byte[] inhalt) { }

    private final DokumentRepository dokumente;
    private final MitgliedRepository mitglieder;
    private final VereinRepository vereine;
    private final BussgeldRepository bussgelder;
    private final SpendeRepository spenden;
    private final GerichtRepository gerichte;

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

    public DokumentMetadaten hochladen(String entityTyp, String entityId, MultipartFile datei) {
        pruefeDatei(datei);
        try {
            return hochladen(entityTyp, entityId, dateiname(datei), contentType(datei), datei.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Datei konnte nicht gelesen werden", e);
        }
    }

    /**
     * @author Nils
     *
     * Upload-Variante für das Vaadin-UI, das die Datei bereits als Byte-Array vorliegen hat.
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
        dokument.setHochgeladenAm(OffsetDateTime.now());
        dokument.setHochgeladenVon(aktuellerBenutzer());
        dokument.setInhalt(inhalt);
        return DokumentMetadaten.of(dokumente.save(dokument));
    }

    @Transactional(readOnly = true)
    public List<DokumentMetadaten> liste(String entityTyp, String entityId) {
        Dokument.EntityTyp typ = pruefeEntityTyp(entityTyp);
        String normalisierteEntityId = pruefeEntityId(typ, entityId);
        return dokumente.findMetadatenByEntityTypAndEntityIdOrderByHochgeladenAmDesc(typ, normalisierteEntityId).stream()
                .map(DokumentMetadaten::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public DokumentDownload herunterladen(Long id) {
        Dokument dokument = holen(id);
        return new DokumentDownload(dokument.getDateiname(), dokument.getContentType(), dokument.getInhalt());
    }

    public void loeschen(Long id) {
        if (!dokumente.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Dokument " + id + " nicht gefunden");
        }
        dokumente.deleteById(id);
    }

    public void loescheFuerEntity(Dokument.EntityTyp entityTyp, String entityId) {
        dokumente.deleteByEntityTypAndEntityId(entityTyp, entityId);
    }

    private Dokument holen(Long id) {
        return dokumente.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Dokument " + id + " nicht gefunden"));
    }

    private static void pruefeDatei(MultipartFile datei) {
        if (datei == null || datei.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Bitte eine Datei auswählen");
        }
        if (datei.getSize() > MAX_DATEIGROESSE) {
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "Datei ist zu groß – maximal 10 MB");
        }
    }

    private Dokument.EntityTyp pruefeEntityTyp(String entityTyp) {
        if (!StringUtils.hasText(entityTyp)) {
            throw new ResponseStatusException(BAD_REQUEST, "entityTyp darf nicht leer sein");
        }
        try {
            return Dokument.EntityTyp.valueOf(entityTyp.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Ungültiger entityTyp '" + entityTyp + "'. Erlaubt: MITGLIED, VEREIN, BUSSGELD, SPENDE, GERICHT");
        }
    }

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

    private String pruefeLongId(String typ, String entityId, LongPredicate exists) {
        long id;
        try {
            id = Long.parseLong(entityId.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(BAD_REQUEST, typ + "-ID '" + entityId + "' ist ungültig");
        }
        if (!exists.test(id)) {
            throw new ResponseStatusException(NOT_FOUND, typ + " " + id + " nicht gefunden");
        }
        return Long.toString(id);
    }

    private String pruefeVereinId(String entityId) {
        String name = entityId.trim();
        if (!vereine.existsById(name)) {
            throw new ResponseStatusException(NOT_FOUND, "Verein '" + name + "' nicht gefunden");
        }
        return name;
    }

    private static String dateiname(MultipartFile datei) {
        String dateiname = StringUtils.cleanPath(datei.getOriginalFilename() != null ? datei.getOriginalFilename() : "");
        dateiname = dateiname.replace('\\', '/');
        if (dateiname.contains("/")) {
            dateiname = dateiname.substring(dateiname.lastIndexOf('/') + 1);
        }
        if (!StringUtils.hasText(dateiname)) {
            throw new ResponseStatusException(BAD_REQUEST, "Dateiname fehlt");
        }
        return dateiname;
    }

    private static String contentType(MultipartFile datei) {
        return StringUtils.hasText(datei.getContentType()) ? datei.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private static String aktuellerBenutzer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !auth.getName().isBlank() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName()
                : "system";
    }
}
