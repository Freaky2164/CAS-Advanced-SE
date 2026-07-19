import { ChangeDetectorRef, Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';

import { ApiService, fehlertext, Mitglied } from '../api.service';

/**
 * @author Nils
 *
 * Report-Seite: alle Auswertungen des Backends als Datei-Download
 * (Bußgelder, Spenden, Serienbrief-Adressen) sowie der E-Mail-Verteiler.
 */
@Component({
  selector: 'app-reports',
  imports: [FormsModule],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css',
})
export class ReportsComponent {
  readonly vereine = ['Frauenhaus', 'Förderverein'];

  // Bußgeld-Übersicht und -Detail
  von = `${new Date().getFullYear()}-01-01`;
  bis = new Date().toISOString().slice(0, 10);
  verein = this.vereine[0];

  // Einzel-Dokumente
  bussgeldId: number | null = null;
  spendeId: number | null = null;
  jahr = new Date().getFullYear();

  // Verteiler
  stichworte = '';
  emails: string[] | null = null;
  serienbriefVerein = this.vereine[0];
  emailBetreff = '';
  emailText = '';
  emailVersandLaeuft = false;
  emailVersandFehler = '';
  emailVersandMeldung = '';

  // Stichwortsuche
  sucheStichworte = '';
  sucheFoerderverein = false;
  sucheFrauenhaus = false;
  sucheErgebnis: Mitglied[] | null = null;

  fehler = '';

  constructor(
    private readonly api: ApiService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  bussgeldUebersicht(): void {
    this.starte(
      this.api.download('/api/reports/bussgeld-uebersicht', { von: this.von, bis: this.bis }),
    );
  }

  bussgeldDetail(): void {
    this.starte(
      this.api.download('/api/reports/bussgeld-detail', {
        von: this.von,
        bis: this.bis,
        verein: this.verein,
      }),
    );
  }

  bussgeldBestaetigung(): void {
    this.starte(this.api.download(`/api/reports/bussgeld-bestaetigung/${this.bussgeldId}`, {}));
  }

  spendenUebersicht(): void {
    this.starte(this.api.download('/api/reports/spenden-uebersicht', { jahr: String(this.jahr) }));
  }

  spendenquittung(): void {
    this.starte(this.api.download(`/api/reports/spendenquittung/${this.spendeId}`, {}));
  }

  spendenquittungDocx(): void {
    this.starte(this.api.download(`/api/reports/spendenquittung-docx/${this.spendeId}`, {}));
  }

  serienbriefAdressen(): void {
    this.starte(
      this.api.download('/api/reports/serienbrief-adressen', { stichworte: this.stichwortListe() }),
    );
  }

  serienbrief(): void {
    this.starte(
      this.api.download('/api/reports/serienbrief', {
        stichworte: this.stichwortListe(),
        verein: this.serienbriefVerein,
      }),
    );
  }

  emailsAnzeigen(): void {
    this.fehler = '';
    this.emails = null;
    this.emailVersandFehler = '';
    this.emailVersandMeldung = '';
    this.api.verteilerEmails(this.stichwortListe()).subscribe({
      next: (emails) => {
        this.emails = emails;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }

  verteilerEmailSenden(): void {
    this.fehler = '';
    this.emailVersandFehler = '';
    this.emailVersandMeldung = '';
    this.emailVersandLaeuft = true;
    this.api
      .verteilerVersenden({
        stichworte: this.stichwortListe(),
        traeger: this.serienbriefVerein,
        betreff: this.emailBetreff.trim(),
        text: this.emailText.trim(),
      })
      .subscribe({
        next: (ergebnis) => {
          this.emailVersandLaeuft = false;
          this.emailVersandMeldung = `E-Mail an ${ergebnis.empfaengerAnzahl} Empfänger gesendet.`;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.emailVersandLaeuft = false;
          this.emailVersandFehler = fehlertext(err);
          this.cdr.markForCheck();
        },
      });
  }

  kannVerteilerSenden(): boolean {
    return (
      !this.emailVersandLaeuft &&
      (this.emails?.length ?? 0) > 0 &&
      this.stichwortListe().length > 0 &&
      this.emailBetreff.trim().length > 0 &&
      this.emailText.trim().length > 0
    );
  }

  stichwortListe(): string[] {
    return this.stichworte
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
  }

  sucheStichwortListe(): string[] {
    return this.sucheStichworte
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
  }

  stichwortsucheAnzeigen(): void {
    this.fehler = '';
    this.sucheErgebnis = null;
    this.api
      .stichwortsuche(this.sucheStichwortListe(), this.sucheFoerderverein, this.sucheFrauenhaus)
      .subscribe({
        next: (ergebnis) => {
          this.sucheErgebnis = ergebnis;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.fehler = fehlertext(err);
          this.cdr.markForCheck();
        },
      });
  }

  stichwortsucheHerunterladen(): void {
    this.starte(
      this.api.download('/api/reports/stichwortsuche.xlsx', {
        stichworte: this.sucheStichwortListe(),
        foerderverein: String(this.sucheFoerderverein),
        frauenhaus: String(this.sucheFrauenhaus),
      }),
    );
  }

  private starte(download: Observable<void>): void {
    this.fehler = '';
    download.subscribe({
      error: (err) => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }
}
