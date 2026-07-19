import { DecimalPipe } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, Subject, takeUntil } from 'rxjs';

import {
  ApiService,
  Spende,
  SpendeRequest,
  Spendenart,
  VerlaufEintrag,
  Verein,
  fehlertext,
} from '../../api.service';
import { DokumentePanelComponent } from '../../dokumente-panel/dokumente-panel.component';
import { VerlaufPanelComponent } from '../../verlauf-panel/verlauf-panel.component';

const LEER: SpendeRequest = {
  mitgliedId: 0,
  spendenart: '',
  verein: '',
  datum: '',
  betrag: 0,
  bemerkung: null,
};

/**
 * @author Nils
 *
 * Pflege der Spenden: paginierte Liste, Anlegen/Bearbeiten/Löschen je Mitglied
 * (alt: generisches CInfoFrame/CListFrame auf frauenhaus.spende).
 */
@Component({
  selector: 'app-spenden',
  imports: [FormsModule, DecimalPipe, VerlaufPanelComponent, DokumentePanelComponent],
  templateUrl: './spenden.component.html',
  styleUrl: './spenden.component.css',
})
export class SpendenComponent implements OnInit, OnDestroy {
  spenden: Spende[] = [];
  seite = 0;
  totalPages = 0;
  totalElements = 0;
  suche = '';

  spendenarten: Spendenart[] = [];
  vereine: Verein[] = [];

  bearbeitetId: number | null = null;
  formular: SpendeRequest = { ...LEER };
  verlaufId: number | null = null;
  verlaufTitel = '';
  verlaufEintraege: VerlaufEintrag[] = [];
  verlaufLaedt = false;
  verlaufFehler = '';
  dokumenteId: number | null = null;
  dokumenteTitel = '';

  fehler = '';
  private readonly suchEingaben = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly api: ApiService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.suchEingaben
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.seite = 0;
        this.cdr.markForCheck();
        this.laden(0);
      });
    this.api.spendenarten().subscribe({
      next: (s) => {
        this.spendenarten = s;
        this.cdr.markForCheck();
      },
    });
    this.api.vereine().subscribe({
      next: (v) => {
        this.vereine = v;
        this.cdr.markForCheck();
      },
    });
    this.laden();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.suchEingaben.complete();
  }

  laden(seite = this.seite): void {
    this.fehler = '';
    this.api.spenden(seite, 20, this.suche).subscribe({
      next: (s) => {
        this.spenden = s.content;
        this.seite = s.number;
        this.totalPages = s.totalPages;
        this.totalElements = s.totalElements;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }

  sucheGeaendert(): void {
    this.suchEingaben.next(this.suche);
  }

  neu(): void {
    this.bearbeitetId = 0;
    this.formular = { ...LEER };
  }

  bearbeiten(s: Spende): void {
    this.bearbeitetId = s.id;
    this.formular = {
      mitgliedId: s.mitgliedId,
      spendenart: s.spendenart,
      verein: s.verein,
      datum: s.datum,
      betrag: s.betrag,
      bemerkung: s.bemerkung,
    };
  }

  abbrechen(): void {
    this.bearbeitetId = null;
  }

  speichern(): void {
    const aufruf = this.bearbeitetId
      ? this.api.spendeAendern(this.bearbeitetId, this.formular)
      : this.api.spendeAnlegen(this.formular);
    aufruf.subscribe({
      next: () => {
        this.bearbeitetId = null;
        this.laden();
      },
      error: (err) => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }

  loeschen(s: Spende): void {
    if (!confirm(`Spende von „${s.mitgliedName}“ über ${s.betrag} € wirklich löschen?`)) return;
    this.api.spendeLoeschen(s.id).subscribe({
      next: () => this.laden(),
      error: (err) => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }

  verlaufAnzeigen(s: Spende): void {
    if (this.verlaufId === s.id) {
      this.verlaufId = null;
      return;
    }
    this.verlaufId = s.id;
    this.verlaufTitel = `Verlauf für Spende ${s.id} (${s.mitgliedName})`;
    this.verlaufEintraege = [];
    this.verlaufFehler = '';
    this.verlaufLaedt = true;
    this.api.spendeVerlauf(s.id).subscribe({
      next: (v) => {
        this.verlaufEintraege = v;
        this.verlaufLaedt = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.verlaufFehler = fehlertext(err);
        this.verlaufLaedt = false;
        this.cdr.markForCheck();
      },
    });
  }

  dokumenteAnzeigen(s: Spende): void {
    if (this.dokumenteId === s.id) {
      this.dokumenteId = null;
      return;
    }
    this.dokumenteId = s.id;
    this.dokumenteTitel = `Dokumente für Spende ${s.id} (${s.mitgliedName})`;
  }
}
