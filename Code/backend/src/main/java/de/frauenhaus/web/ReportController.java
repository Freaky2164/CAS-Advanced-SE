package de.frauenhaus.web;

import de.frauenhaus.service.BussgeldReportService;
import de.frauenhaus.service.SpendenService;
import de.frauenhaus.service.StichwortsucheService;
import de.frauenhaus.service.VerteilerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Nils
 *
 * REST-Endpunkte für die Report-Generierung (Bußgeld-, Spenden- und
 * Verteiler-Reports als xlsx/docx-Download sowie den SMTP-E-Mail-Versand.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final BussgeldReportService bussgeldReports;
    private final SpendenService spendenService;
    private final VerteilerService verteilerService;
    private final StichwortsucheService stichwortsucheService;

    public ReportController(BussgeldReportService bussgeldReports,
                            SpendenService spendenService,
                            VerteilerService verteilerService,
                            StichwortsucheService stichwortsucheService) {
        this.bussgeldReports = bussgeldReports;
        this.spendenService = spendenService;
        this.verteilerService = verteilerService;
        this.stichwortsucheService = stichwortsucheService;
    }

    /** Liefert die Bußgeld-Übersicht (Summen je Gericht und Träger) im gegebenen Zeitraum als xlsx. */
    @GetMapping("/bussgeld-uebersicht")
    public ResponseEntity<byte[]> bussgeldUebersicht(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate von,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bis) {
        return download(bussgeldReports.uebersicht(von, bis), "bussgeld-uebersicht.xlsx", XLSX);
    }

    /** Liefert die Bußgeld-Detailliste mit Zahlungseingängen im Zeitraum für einen Träger als xlsx. */
    @GetMapping("/bussgeld-detail")
    public ResponseEntity<byte[]> bussgeldDetail(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate von,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bis,
            @RequestParam String verein) {
        return download(bussgeldReports.detail(von, bis, verein), "bussgeld-detail.xlsx", XLSX);
    }

    /** Liefert die Zahlungsbestätigung an das Gericht für ein Bußgeld als docx. */
    @GetMapping("/bussgeld-bestaetigung/{bussgeldId}")
    public ResponseEntity<byte[]> bussgeldBestaetigung(@PathVariable Long bussgeldId) {
        return download(bussgeldReports.bestaetigung(bussgeldId), "bestaetigung-" + bussgeldId + ".docx", DOCX);
    }

    /** Liefert alle Spenden eines Jahres, gruppiert nach Träger/Spendentyp/-art, als xlsx. */
    @GetMapping("/spenden-uebersicht")
    public ResponseEntity<byte[]> spendenUebersicht(@RequestParam int jahr) {
        return download(spendenService.uebersicht(jahr), "spenden-uebersicht-" + jahr + ".xlsx", XLSX);
    }

    /** Liefert die Quittungsdaten zu einer Spende (inkl. Betrag in Worten) als xlsx. */
    @GetMapping("/spendenquittung/{spendeId}")
    public ResponseEntity<byte[]> spendenquittung(@PathVariable Long spendeId) {
        return download(spendenService.quittung(spendeId), "spendenquittung-" + spendeId + ".xlsx", XLSX);
    }

    /** Liefert die Spendenquittung als formatiertes Zuwendungsbestätigungs-Dokument (docx). */
    @GetMapping("/spendenquittung-docx/{spendeId}")
    public ResponseEntity<byte[]> spendenquittungDocx(@PathVariable Long spendeId) {
        return download(spendenService.quittungDocx(spendeId), "spendenquittung-" + spendeId + ".docx", DOCX);
    }

    /** Liefert die E-Mail-Adressen aller Mitglieder mit den gegebenen Verteiler-Stichworten. */
    @GetMapping("/verteiler-emails")
    public List<String> verteilerEmails(@RequestParam List<String> stichworte) {
        return verteilerService.emails(stichworte);
    }

    /** Versendet eine Sammel-E-Mail an alle Verteiler-Empfänger per BCC. */
    @PostMapping("/verteiler/versenden")
    public VerteilerService.VersandErgebnis verteilerVersenden(@Valid @RequestBody VerteilerVersandRequest request) {
        return verteilerService.versenden(request.stichworte(), request.traeger(), request.betreff(), request.text());
    }

    /** Liefert die Serienbrief-Adressliste zu den gegebenen Verteiler-Stichworten als xlsx. */
    @GetMapping("/serienbrief-adressen")
    public ResponseEntity<byte[]> serienbriefAdressen(@RequestParam List<String> stichworte) {
        return download(verteilerService.adressen(stichworte), "serienbrief-adressen.xlsx", XLSX);
    }

    /** Liefert die generierten Serienbriefe (ein Anschreiben je Empfänger) als docx. */
    @GetMapping("/serienbrief")
    public ResponseEntity<byte[]> serienbrief(@RequestParam List<String> stichworte, @RequestParam String verein) {
        return download(verteilerService.serienbrief(stichworte, verein), "serienbrief.docx", DOCX);
    }

    /** Liefert die Mitglieder, die mindestens eines der gegebenen Stichworte tragen, als Vorschau (alt: CReportStichwortSuche). */
    @GetMapping("/stichwortsuche")
    public List<de.frauenhaus.service.MitgliedService.MitgliedResponse> stichwortsuche(
            @RequestParam List<String> stichworte,
            @RequestParam(defaultValue = "false") boolean foerderverein,
            @RequestParam(defaultValue = "false") boolean frauenhaus) {
        return stichwortsucheService.suchenAlsResponses(stichworte, foerderverein, frauenhaus);
    }

    /** Liefert die Mitgliedersuche über Stichworte als xlsx-Download (alt: CReportStichwortSuche). */
    @GetMapping("/stichwortsuche.xlsx")
    public ResponseEntity<byte[]> stichwortsucheExcel(
            @RequestParam List<String> stichworte,
            @RequestParam(defaultValue = "false") boolean foerderverein,
            @RequestParam(defaultValue = "false") boolean frauenhaus) {
        return download(stichwortsucheService.suchenAlsExcel(stichworte, foerderverein, frauenhaus),
                "stichwortsuche.xlsx", XLSX);
    }

    /** Baut die Download-Antwort mit Dateinamen und Content-Type für den gegebenen Report-Inhalt. */
    private static ResponseEntity<byte[]> download(byte[] inhalt, String dateiname, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dateiname + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(inhalt);
    }

    public record VerteilerVersandRequest(
            @NotEmpty List<String> stichworte,
            @NotBlank String traeger,
            @NotBlank String betreff,
            @NotBlank String text) { }
}
