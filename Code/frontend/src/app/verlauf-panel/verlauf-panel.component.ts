import { DatePipe } from '@angular/common';
import { Component, Input } from '@angular/core';

import { VerlaufEintrag } from '../api.service';

/**
 * @author Nils
 *
 * Wiederverwendbare Anzeige der Änderungshistorie (Zeitpunkt, Benutzer und
 * Feld-Diffs) für auditierte Stammdaten.
 */
@Component({
  selector: 'app-verlauf-panel',
  imports: [DatePipe],
  templateUrl: './verlauf-panel.component.html',
  styleUrl: './verlauf-panel.component.css',
})
export class VerlaufPanelComponent {
  @Input({ required: true }) titel = 'Verlauf';
  @Input() eintraege: VerlaufEintrag[] = [];
  @Input() laedt = false;
  @Input() fehler = '';

  wertText(wert: string | number | boolean | null): string {
    if (wert === null || wert === '') return '–';
    if (wert === true) return 'Ja';
    if (wert === false) return 'Nein';
    return String(wert);
  }
}
