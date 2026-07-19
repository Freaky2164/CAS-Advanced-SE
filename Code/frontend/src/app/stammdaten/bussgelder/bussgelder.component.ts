import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, Subject, takeUntil } from 'rxjs';

import {
  ApiService,
  Bussgeld,
  BussgeldRequest,
  Bussgeldstatus,
  Gericht,
  VerlaufEintrag,
  Verein,
  fehlertext,
} from '../../api.service';
import { DokumentePanelComponent } from '../../dokumente-panel/dokumente-panel.component';
import { VerlaufPanelComponent } from '../../verlauf-panel/verlauf-panel.component';

const LEER: BussgeldRequest = {
  gerichtId: 0,
  verein: '',
  status: null,
  name: null,
  vorname: null,
  aktenzeichen: null,
  datum: '',
  zieldatum: null,
  betrag: 0,
  bezahlt: false,
  bemerkung: null,
};

/**
 * @author Nils
 *
 * Pflege der Bußgeldverfahren inkl. Zahlungseingängen (alt: generisches
 * CInfoFrame auf frauenhaus.bussgeld mit Sub-Tabelle eingang).
 */
@Component({
  selector: 'app-bussgelder',
  imports: [FormsModule, VerlaufPanelComponent, DokumentePanelComponent],
  templateUrl: './bussgelder.component.html',
  styleUrl: './bussgelder.component.css',
})
export class BussgelderComponent implements OnInit, OnDestroy {
  bussgelder: Bussgeld[] = [];
  seite = 0;
  totalPages = 0;
  totalElements = 0;
  suche = '';

  gerichte: Gericht[] = [];
  vereine: Verein[] = [];
  stati: Bussgeldstatus[] = [];

  bearbeitetId: number | null = null;
  formular: BussgeldRequest = { ...LEER };
  verlaufId: number | null = null;
  verlaufTitel = '';
  verlaufEintraege: VerlaufEintrag[] = [];
  verlaufLaedt = false;
  verlaufFehler = '';
  dokumenteId: number | null = null;
  dokumenteTitel = '';

  eingangOffenFuer: number | null = null;
  neuerEingang: { datum: string; betrag: number; bemerkung: string | null } = {
    datum: '',
    betrag: 0,
    bemerkung: null,
  };

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
    this.api.gerichte().subscribe({
      next: (g) => {
        this.gerichte = g;
        this.cdr.markForCheck();
      },
    });
    this.api.vereine().subscribe({
      next: (v) => {
        this.vereine = v;
        this.cdr.markForCheck();
      },
    });
    this.api.bussgeldstati().subscribe({
      next: (s) => {
        this.stati = s;
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
    this.api.bussgelder(seite, 20, this.suche).subscribe({
      next: (s) => {
        this.bussgelder = s.content;
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

  bearbeiten(b: Bussgeld): void {
    this.bearbeitetId = b.id;
    this.formular = {
      gerichtId: b.gerichtId,
      verein: b.verein,
      status: b.status,
      name: b.name,
      vorname: b.vorname,
      aktenzeichen: b.aktenzeichen,
      datum: b.datum,
      zieldatum: b.zieldatum,
      betrag: b.betrag,
      bezahlt: b.bezahlt,
      bemerkung: b.bemerkung,
    };
  }

  abbrechen(): void {
    this.bearbeitetId = null;
  }

  speichern(): void {
    const aufruf = this.bearbeitetId
      ? this.api.bussgeldAendern(this.bearbeitetId, this.formular)
      : this.api.bussgeldAnlegen(this.formular);
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

  loeschen(b: Bussgeld): void {
    if (!confirm(`Bußgeldverfahren „${b.aktenzeichen ?? b.id}“ wirklich löschen?`)) return;
    this.api.bussgeldLoeschen(b.id).subscribe({
      next: () => this.laden(),
      error: (err) => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }

  eingangFormularOeffnen(b: Bussgeld): void {
    this.eingangOffenFuer = b.id;
    this.neuerEingang = { datum: '', betrag: 0, bemerkung: null };
  }

  eingangAbbrechen(): void {
    this.eingangOffenFuer = null;
  }

  eingangHinzufuegen(b: Bussgeld): void {
    this.api.eingangHinzufuegen(b.id, this.neuerEingang).subscribe({
      next: (aktualisiert) => {
        Object.assign(b, aktualisiert);
        this.eingangOffenFuer = null;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }

  eingangEntfernen(b: Bussgeld, eingangId: number): void {
    if (!confirm('Zahlungseingang wirklich löschen?')) return;
    this.api.eingangEntfernen(b.id, eingangId).subscribe({
      next: (aktualisiert) => {
        Object.assign(b, aktualisiert);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }

  verlaufAnzeigen(b: Bussgeld): void {
    if (this.verlaufId === b.id) {
      this.verlaufId = null;
      return;
    }
    this.verlaufId = b.id;
    this.verlaufTitel = `Verlauf für Bußgeld ${b.aktenzeichen ?? b.id}`;
    this.verlaufEintraege = [];
    this.verlaufFehler = '';
    this.verlaufLaedt = true;
    this.api.bussgeldVerlauf(b.id).subscribe({
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

  dokumenteAnzeigen(b: Bussgeld): void {
    if (this.dokumenteId === b.id) {
      this.dokumenteId = null;
      return;
    }
    this.dokumenteId = b.id;
    this.dokumenteTitel = `Dokumente für Bußgeld ${b.aktenzeichen ?? b.id}`;
  }
}
