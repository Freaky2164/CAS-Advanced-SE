package de.frauenhaus.web;

import de.frauenhaus.service.BussgeldReportService;
import de.frauenhaus.service.DocumentCreationService;
import de.frauenhaus.service.MitgliedService;
import de.frauenhaus.service.SpendenService;
import de.frauenhaus.service.StichwortsucheService;
import de.frauenhaus.service.VerteilerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * REST-Endpunkte für die Report-Generierung: Übersichten als xlsx,
 * Bußgeldbestätigungen und Spendenbescheinigungen als Word-Dokumente
 * aus den Vorlagen in vorlagen/, Serienbriefe und Stichwortsuche
 * sowie der SMTP-Verteiler-Versand.
 *
 * @author Robin
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String DOC = "application/msword";
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final BussgeldReportService bussgeldReports;
    private final SpendenService spendenService;
    private final VerteilerService verteilerService;
    private final DocumentCreationService documentCreationService;
    private final StichwortsucheService stichwortsucheService;

    /**
     * Erzeugt den Controller mit den benötigten Services.
     */
    public ReportController(BussgeldReportService bussgeldReports,
                            SpendenService spendenService,
                            VerteilerService verteilerService,
                            DocumentCreationService documentCreationService,
                            StichwortsucheService stichwortsucheService) {
        this.bussgeldReports = bussgeldReports;
        this.spendenService = spendenService;
        this.verteilerService = verteilerService;
        this.documentCreationService = documentCreationService;
        this.stichwortsucheService = stichwortsucheService;
    }

    /**
     * Liefert die Bußgeld-Übersicht (Summen je Gericht und Träger) im gegebenen Zeitraum als xlsx.
     */
    @GetMapping("/bussgeld-uebersicht")
    public ResponseEntity<byte[]> bussgeldUebersicht(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate von,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bis) {
        return download(bussgeldReports.uebersicht(von, bis), "bussgeld-uebersicht.xlsx", XLSX);
    }

    /**
     * Liefert die Bußgeld-Detailliste mit Zahlungseingängen im Zeitraum für einen Träger als xlsx.
     */
    @GetMapping("/bussgeld-detail")
    public ResponseEntity<byte[]> bussgeldDetail(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate von,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bis,
            @RequestParam String verein) {
        return download(bussgeldReports.detail(von, bis, verein), "bussgeld-detail.xlsx", XLSX);
    }

    /**
     * Liefert die Zahlungsbestätigung an das Gericht (Vorlage FHBG.dot/FVBG.dot) als Word-Dokument.
     */
    @GetMapping("/bussgeld-bestaetigung/{bussgeldId}")
    public ResponseEntity<byte[]> bussgeldBestaetigung(@PathVariable Long bussgeldId) {
        return download(documentCreationService.bussgeldBestaetigung(bussgeldId),
                "bestaetigung-" + bussgeldId + ".doc", DOC);
    }

    /**
     * Liefert alle Spenden eines Jahres, gruppiert nach Träger/Spendentyp/-art, als xlsx.
     */
    @GetMapping("/spenden-uebersicht")
    public ResponseEntity<byte[]> spendenUebersicht(@RequestParam int jahr) {
        return download(spendenService.uebersicht(jahr), "spenden-uebersicht-" + jahr + ".xlsx", XLSX);
    }

    /**
     * Liefert die Spendenbescheinigung (Vorlagen FHSB*.dot/FVSB*.dot je Träger und Spendentyp) als Word-Dokument.
     */
    @GetMapping("/spendenquittung/{spendeId}")
    public ResponseEntity<byte[]> spendenquittung(@PathVariable Long spendeId) {
        return download(documentCreationService.spendenBescheinigung(spendeId),
                "spendenbescheinigung-" + spendeId + ".doc", DOC);
    }

    /**
     * Liefert die E-Mail-Adressen aller Mitglieder mit den gegebenen Verteiler-Stichworten.
     */
    @GetMapping("/verteiler-emails")
    public List<String> verteilerEmails(@RequestParam List<String> stichworte) {
        return verteilerService.emails(stichworte);
    }

    /**
     * Versendet eine Sammel-E-Mail an alle Verteiler-Empfänger per BCC.
     */
    @PostMapping("/verteiler/versenden")
    public VerteilerService.VersandErgebnis verteilerVersenden(@Valid @RequestBody VerteilerVersandRequest request) {
        return verteilerService.versenden(request.stichworte(), request.betreff(), request.text());
    }

    /**
     * Liefert die Serienbrief-Adressliste zu den gegebenen Verteiler-Stichworten als xlsx.
     */
    @GetMapping("/serienbrief-adressen")
    public ResponseEntity<byte[]> serienbriefAdressen(@RequestParam List<String> stichworte) {
        return download(verteilerService.adressen(stichworte), "serienbrief-adressen.xlsx", XLSX);
    }

    /**
     * Liefert die generierten Serienbriefe (ein Anschreiben je Empfänger) als docx;
     * {@code text} ist der optionale Brieftext (ohne ihn bleibt Platz zum Ergänzen in Word).
     */
    @GetMapping("/serienbrief")
    public ResponseEntity<byte[]> serienbrief(@RequestParam List<String> stichworte,
                                              @RequestParam String verein,
                                              @RequestParam(required = false) String text) {
        return download(verteilerService.serienbrief(stichworte, verein, text), "serienbrief.docx", DOCX);
    }

    /**
     * Liefert die Mitglieder, die mindestens eines der gegebenen Stichworte tragen, als Vorschau.
     */
    @GetMapping("/stichwortsuche")
    public List<MitgliedService.MitgliedResponse> stichwortsuche(
            @RequestParam List<String> stichworte,
            @RequestParam(defaultValue = "false") boolean foerderverein,
            @RequestParam(defaultValue = "false") boolean frauenhaus) {
        return stichwortsucheService.suchenAlsResponses(stichworte, foerderverein, frauenhaus);
    }

    /**
     * Liefert die Mitgliedersuche über Stichworte als xlsx-Download.
     */
    @GetMapping("/stichwortsuche.xlsx")
    public ResponseEntity<byte[]> stichwortsucheExcel(
            @RequestParam List<String> stichworte,
            @RequestParam(defaultValue = "false") boolean foerderverein,
            @RequestParam(defaultValue = "false") boolean frauenhaus) {
        return download(stichwortsucheService.suchenAlsExcel(stichworte, foerderverein, frauenhaus),
                "stichwortsuche.xlsx", XLSX);
    }

    /**
     * Baut die Download-Antwort mit Dateinamen und Content-Type für den gegebenen Report-Inhalt.
     * Der Dateiname wird RFC-6266-konform kodiert, damit Umlaute erhalten bleiben.
     */
    private static ResponseEntity<byte[]> download(byte[] inhalt, String dateiname, String contentType) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(dateiname, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(inhalt);
    }

    /**
     * Datenobjekt der REST-Schnittstelle.
     */
    public record VerteilerVersandRequest(
            @NotEmpty List<String> stichworte,
            @NotBlank String betreff,
            @NotBlank String text) { }
}
