import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';

import { ApiService, fehlertext } from '../api.service';

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

  fehler = '';

  constructor(private readonly api: ApiService) {}

  bussgeldUebersicht(): void {
    this.starte(this.api.download('/api/reports/bussgeld-uebersicht', { von: this.von, bis: this.bis }));
  }

  bussgeldDetail(): void {
    this.starte(
      this.api.download('/api/reports/bussgeld-detail', { von: this.von, bis: this.bis, verein: this.verein }),
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

  serienbriefAdressen(): void {
    this.starte(this.api.download('/api/reports/serienbrief-adressen', { stichworte: this.stichwortListe() }));
  }

  emailsAnzeigen(): void {
    this.fehler = '';
    this.emails = null;
    this.api.verteilerEmails(this.stichwortListe()).subscribe({
      next: emails => (this.emails = emails),
      error: err => (this.fehler = fehlertext(err)),
    });
  }

  stichwortListe(): string[] {
    return this.stichworte
      .split(',')
      .map(s => s.trim())
      .filter(s => s.length > 0);
  }

  private starte(download: Observable<void>): void {
    this.fehler = '';
    download.subscribe({ error: err => (this.fehler = fehlertext(err)) });
  }
}
