import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, Subject, takeUntil } from 'rxjs';

import { Anrede, ApiService, Mitglied, MitgliedRequest, VerlaufEintrag, Verein, fehlertext } from '../../api.service';
import { DokumentePanelComponent } from '../../dokumente-panel/dokumente-panel.component';
import { VerlaufPanelComponent } from '../../verlauf-panel/verlauf-panel.component';

const LEER: MitgliedRequest = {
  anrede: null,
  vorname: null,
  name: '',
  name2: null,
  name3: null,
  briefanrede: null,
  strasse: null,
  plz: null,
  ort: null,
  email: null,
  tel1: null,
  tel2: null,
  fax: null,
  foerderverein: false,
  frauenhaus: false,
  bemerkung: null,
  stichworte: [],
  vereine: [],
};

/**
 * @author Nils
 *
 * Pflege der Mitglieder/Adressen: paginierte Liste, Anlegen/Bearbeiten/Löschen
 * sowie Duplizieren (alt: generisches CInfoFrame/CListFrame auf frauenhaus.mitglied
 * mit New/Edit/Copy/Delete).
 */
@Component({
  selector: 'app-mitglieder',
  imports: [FormsModule, VerlaufPanelComponent, DokumentePanelComponent],
  templateUrl: './mitglieder.component.html',
  styleUrl: './mitglieder.component.css',
})
export class MitgliederComponent implements OnInit, OnDestroy {
  mitglieder: Mitglied[] = [];
  seite = 0;
  totalPages = 0;
  totalElements = 0;
  suche = '';

  anreden: Anrede[] = [];
  vereine: Verein[] = [];

  bearbeitetId: number | null = null;
  formular: MitgliedRequest = { ...LEER };
  stichworteText = '';
  vereineAuswahl: Record<string, boolean> = {};
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
    this.api.anreden().subscribe({
      next: a => {
        this.anreden = a;
        this.cdr.markForCheck();
      },
    });
    this.api.vereine().subscribe({
      next: v => {
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
    this.api.mitglieder(seite, 20, this.suche).subscribe({
      next: s => {
        this.mitglieder = s.content;
        this.seite = s.number;
        this.totalPages = s.totalPages;
        this.totalElements = s.totalElements;
        this.cdr.markForCheck();
      },
      error: err => {
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
    this.stichworteText = '';
    this.vereineAuswahl = {};
  }

  bearbeiten(m: Mitglied): void {
    this.bearbeitetId = m.id;
    this.formular = {
      anrede: m.anrede, vorname: m.vorname, name: m.name, name2: m.name2, name3: m.name3,
      briefanrede: m.briefanrede, strasse: m.strasse, plz: m.plz, ort: m.ort, email: m.email,
      tel1: m.tel1, tel2: m.tel2, fax: m.fax, foerderverein: m.foerderverein, frauenhaus: m.frauenhaus,
      bemerkung: m.bemerkung, stichworte: [...m.stichworte], vereine: [...m.vereine],
    };
    this.stichworteText = m.stichworte.join(', ');
    this.vereineAuswahl = Object.fromEntries(this.vereine.map(v => [v.name, m.vereine.includes(v.name)]));
  }

  abbrechen(): void {
    this.bearbeitetId = null;
  }

  speichern(): void {
    const anfrage: MitgliedRequest = {
      ...this.formular,
      stichworte: this.stichworteText.split(',').map(s => s.trim()).filter(s => s.length > 0),
      vereine: Object.entries(this.vereineAuswahl).filter(([, gewaehlt]) => gewaehlt).map(([name]) => name),
    };
    const aufruf = this.bearbeitetId ? this.api.mitgliedAendern(this.bearbeitetId, anfrage) : this.api.mitgliedAnlegen(anfrage);
    aufruf.subscribe({
      next: () => {
        this.bearbeitetId = null;
        this.laden();
      },
      error: err => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }

  duplizieren(id: number): void {
    this.api.mitgliedDuplizieren(id).subscribe({
      next: () => this.laden(),
      error: err => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }

  loeschen(m: Mitglied): void {
    if (!confirm(`Mitglied „${m.vorname ?? ''} ${m.name}“ wirklich löschen?`)) return;
    this.api.mitgliedLoeschen(m.id).subscribe({
      next: () => this.laden(),
      error: err => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }

  verlaufAnzeigen(m: Mitglied): void {
    if (this.verlaufId === m.id) {
      this.verlaufId = null;
      return;
    }
    this.verlaufId = m.id;
    this.verlaufTitel = `Verlauf für ${this.mitgliedLabel(m)}`;
    this.verlaufEintraege = [];
    this.verlaufFehler = '';
    this.verlaufLaedt = true;
    this.api.mitgliedVerlauf(m.id).subscribe({
      next: v => {
        this.verlaufEintraege = v;
        this.verlaufLaedt = false;
        this.cdr.markForCheck();
      },
      error: err => {
        this.verlaufFehler = fehlertext(err);
        this.verlaufLaedt = false;
        this.cdr.markForCheck();
      },
    });
  }

  dokumenteAnzeigen(m: Mitglied): void {
    if (this.dokumenteId === m.id) {
      this.dokumenteId = null;
      return;
    }
    this.dokumenteId = m.id;
    this.dokumenteTitel = `Dokumente für ${this.mitgliedLabel(m)}`;
  }

  private mitgliedLabel(m: Mitglied): string {
    return [m.anrede, m.vorname, m.name].filter(Boolean).join(' ');
  }
}
