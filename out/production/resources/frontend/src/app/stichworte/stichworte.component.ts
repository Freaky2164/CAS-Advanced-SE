import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ApiService, fehlertext } from '../api.service';

/**
 * @author Nils
 *
 * Pflege der Verteiler-Stichworte: Zusammenstellen (alte bleiben erhalten)
 * und Zusammenfassen (alte werden gelöscht).
 */
@Component({
  selector: 'app-stichworte',
  imports: [FormsModule],
  templateUrl: './stichworte.component.html',
  styleUrl: './stichworte.component.css',
})
export class StichworteComponent {
  neu = '';
  alte = '';
  meldung = '';
  fehler = '';
  laedt = false;

  constructor(private readonly api: ApiService) {}

  zusammenstellen(): void {
    this.ausfuehren('zusammenstellen');
  }

  zusammenfassen(): void {
    if (!confirm(`Die Stichworte „${this.alteListe().join('“, „')}“ werden dabei gelöscht. Fortfahren?`)) {
      return;
    }
    this.ausfuehren('zusammenfassen');
  }

  alteListe(): string[] {
    return this.alte
      .split(',')
      .map(s => s.trim())
      .filter(s => s.length > 0);
  }

  private ausfuehren(aktion: 'zusammenstellen' | 'zusammenfassen'): void {
    this.laedt = true;
    this.meldung = '';
    this.fehler = '';
    const aufruf =
      aktion === 'zusammenstellen'
        ? this.api.zusammenstellen(this.neu.trim(), this.alteListe())
        : this.api.zusammenfassen(this.neu.trim(), this.alteListe());
    aufruf.subscribe({
      next: ergebnis => {
        this.laedt = false;
        this.meldung = `Fertig: ${ergebnis.zugeordnet} Mitglieder dem Stichwort „${this.neu.trim()}“ neu zugeordnet.`;
      },
      error: err => {
        this.laedt = false;
        this.fehler = fehlertext(err);
      },
    });
  }
}
