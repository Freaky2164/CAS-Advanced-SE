import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, Subject, takeUntil } from 'rxjs';

import {
  Anrede,
  ApiService,
  Bussgeldstatus,
  Gericht,
  Spendenart,
  Spendentyp,
  VerlaufEintrag,
  Verein,
  fehlertext,
} from '../../api.service';
import { DokumentePanelComponent } from '../../dokumente-panel/dokumente-panel.component';
import { VerlaufPanelComponent } from '../../verlauf-panel/verlauf-panel.component';

/**
 * @author Nils
 *
 * Pflege der übrigen Stammdaten-Listen: Vereine (Träger), Gerichte, Spendenarten
 * sowie die Lookup-Werte Anrede/Spendentyp/Bußgeldstatus (alt: je ein generisches
 * CInfoFrame/CListFrame pro Datenobjekt).
 */
@Component({
  selector: 'app-verwaltung',
  imports: [FormsModule, VerlaufPanelComponent, DokumentePanelComponent],
  templateUrl: './verwaltung.component.html',
  styleUrl: './verwaltung.component.css',
})
export class VerwaltungComponent implements OnInit, OnDestroy {
  vereine: Verein[] = [];
  gerichte: Gericht[] = [];
  spendenarten: Spendenart[] = [];
  anreden: Anrede[] = [];
  spendentypen: Spendentyp[] = [];
  bussgeldstati: Bussgeldstatus[] = [];
  vereinSuche = '';
  gerichtSuche = '';

  neuerVerein: Verein = { name: '', bezeichnung: '' };
  neuesGericht: Omit<Gericht, 'id'> = { bezeichnung: '', strasse: '', plz: '', ort: '' };
  neueSpendenart: Spendenart = { name: '', spendentyp: '' };
  neueAnrede = '';
  neuerSpendentyp = '';
  neuerBussgeldstatus = '';
  vereinVerlaufName: string | null = null;
  vereinVerlaufTitel = '';
  vereinVerlaufEintraege: VerlaufEintrag[] = [];
  vereinVerlaufLaedt = false;
  vereinVerlaufFehler = '';
  vereinDokumenteName: string | null = null;
  vereinDokumenteTitel = '';
  gerichtVerlaufId: number | null = null;
  gerichtVerlaufTitel = '';
  gerichtVerlaufEintraege: VerlaufEintrag[] = [];
  gerichtVerlaufLaedt = false;
  gerichtVerlaufFehler = '';
  gerichtDokumenteId: number | null = null;
  gerichtDokumenteTitel = '';

  fehler = '';
  private readonly vereinSuchEingaben = new Subject<string>();
  private readonly gerichtSuchEingaben = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly api: ApiService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.vereinSuchEingaben
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.vereineLaden());
    this.gerichtSuchEingaben
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.gerichteLaden());
    this.neuLaden();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.vereinSuchEingaben.complete();
    this.gerichtSuchEingaben.complete();
  }

  neuLaden(): void {
    this.fehler = '';
    this.vereineLaden();
    this.gerichteLaden();
    this.api.spendenarten().subscribe({
      next: (s) => this.setzen(() => (this.spendenarten = s)),
      error: (err) => this.fehlerAnzeigen(err),
    });
    this.api
      .anreden()
      .subscribe({
        next: (a) => this.setzen(() => (this.anreden = a)),
        error: (err) => this.fehlerAnzeigen(err),
      });
    this.api.spendentypen().subscribe({
      next: (s) => this.setzen(() => (this.spendentypen = s)),
      error: (err) => this.fehlerAnzeigen(err),
    });
    this.api.bussgeldstati().subscribe({
      next: (s) => this.setzen(() => (this.bussgeldstati = s)),
      error: (err) => this.fehlerAnzeigen(err),
    });
  }

  private setzen(mutation: () => void): void {
    mutation();
    this.cdr.markForCheck();
  }

  private fehlerAnzeigen(err: HttpErrorResponse): void {
    this.fehler = fehlertext(err);
    this.cdr.markForCheck();
  }

  private vereineLaden(): void {
    this.api.vereine(this.vereinSuche).subscribe({
      next: (v) => this.setzen(() => (this.vereine = v)),
      error: (err) => this.fehlerAnzeigen(err),
    });
  }

  private gerichteLaden(): void {
    this.api.gerichte(this.gerichtSuche).subscribe({
      next: (g) => this.setzen(() => (this.gerichte = g)),
      error: (err) => this.fehlerAnzeigen(err),
    });
  }

  vereinSucheGeaendert(): void {
    this.vereinSuchEingaben.next(this.vereinSuche);
  }

  gerichtSucheGeaendert(): void {
    this.gerichtSuchEingaben.next(this.gerichtSuche);
  }

  // --- Vereine ---
  vereinAnlegen(): void {
    this.api.vereinAnlegen(this.neuerVerein).subscribe({
      next: () => {
        this.neuerVerein = { name: '', bezeichnung: '' };
        this.neuLaden();
      },
      error: (err) => this.fehlerAnzeigen(err),
    });
  }

  vereinBezeichnungAendern(v: Verein): void {
    this.api
      .vereinAendern(v.name, v.bezeichnung)
      .subscribe({ error: (err) => this.fehlerAnzeigen(err) });
  }

  vereinLoeschen(name: string): void {
    if (!confirm(`Verein „${name}“ wirklich löschen?`)) return;
    this.api
      .vereinLoeschen(name)
      .subscribe({ next: () => this.neuLaden(), error: (err) => this.fehlerAnzeigen(err) });
  }

  vereinVerlaufAnzeigen(v: Verein): void {
    if (this.vereinVerlaufName === v.name) {
      this.vereinVerlaufName = null;
      return;
    }
    this.vereinVerlaufName = v.name;
    this.vereinVerlaufTitel = `Verlauf für Verein ${v.name}`;
    this.vereinVerlaufEintraege = [];
    this.vereinVerlaufFehler = '';
    this.vereinVerlaufLaedt = true;
    this.api.vereinVerlauf(v.name).subscribe({
      next: (eintraege) => {
        this.vereinVerlaufEintraege = eintraege;
        this.vereinVerlaufLaedt = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.vereinVerlaufFehler = fehlertext(err);
        this.vereinVerlaufLaedt = false;
        this.cdr.markForCheck();
      },
    });
  }

  vereinDokumenteAnzeigen(v: Verein): void {
    if (this.vereinDokumenteName === v.name) {
      this.vereinDokumenteName = null;
      return;
    }
    this.vereinDokumenteName = v.name;
    this.vereinDokumenteTitel = `Dokumente für Verein ${v.name}`;
  }

  // --- Gerichte ---
  gerichtAnlegen(): void {
    this.api.gerichtAnlegen(this.neuesGericht).subscribe({
      next: () => {
        this.neuesGericht = { bezeichnung: '', strasse: '', plz: '', ort: '' };
        this.neuLaden();
      },
      error: (err) => this.fehlerAnzeigen(err),
    });
  }

  gerichtAendern(g: Gericht): void {
    this.api.gerichtAendern(g.id, g).subscribe({ error: (err) => this.fehlerAnzeigen(err) });
  }

  gerichtLoeschen(id: number): void {
    if (!confirm('Gericht wirklich löschen?')) return;
    this.api
      .gerichtLoeschen(id)
      .subscribe({ next: () => this.neuLaden(), error: (err) => this.fehlerAnzeigen(err) });
  }

  gerichtVerlaufAnzeigen(g: Gericht): void {
    if (this.gerichtVerlaufId === g.id) {
      this.gerichtVerlaufId = null;
      return;
    }
    this.gerichtVerlaufId = g.id;
    this.gerichtVerlaufTitel = `Verlauf für Gericht ${g.bezeichnung}`;
    this.gerichtVerlaufEintraege = [];
    this.gerichtVerlaufFehler = '';
    this.gerichtVerlaufLaedt = true;
    this.api.gerichtVerlauf(g.id).subscribe({
      next: (eintraege) => {
        this.gerichtVerlaufEintraege = eintraege;
        this.gerichtVerlaufLaedt = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.gerichtVerlaufFehler = fehlertext(err);
        this.gerichtVerlaufLaedt = false;
        this.cdr.markForCheck();
      },
    });
  }

  gerichtDokumenteAnzeigen(g: Gericht): void {
    if (this.gerichtDokumenteId === g.id) {
      this.gerichtDokumenteId = null;
      return;
    }
    this.gerichtDokumenteId = g.id;
    this.gerichtDokumenteTitel = `Dokumente für Gericht ${g.bezeichnung}`;
  }

  // --- Spendenarten ---
  spendenartAnlegen(): void {
    this.api.spendenartAnlegen(this.neueSpendenart).subscribe({
      next: () => {
        this.neueSpendenart = { name: '', spendentyp: '' };
        this.neuLaden();
      },
      error: (err) => this.fehlerAnzeigen(err),
    });
  }

  spendenartAendern(sa: Spendenart): void {
    this.api
      .spendenartAendern(sa.name, sa.spendentyp)
      .subscribe({ error: (err) => this.fehlerAnzeigen(err) });
  }

  spendenartLoeschen(name: string): void {
    if (!confirm(`Spendenart „${name}“ wirklich löschen?`)) return;
    this.api
      .spendenartLoeschen(name)
      .subscribe({ next: () => this.neuLaden(), error: (err) => this.fehlerAnzeigen(err) });
  }

  // --- Anreden ---
  anredeAnlegen(): void {
    if (!this.neueAnrede.trim()) return;
    this.api.anredeAnlegen(this.neueAnrede.trim()).subscribe({
      next: () => {
        this.neueAnrede = '';
        this.neuLaden();
      },
      error: (err) => this.fehlerAnzeigen(err),
    });
  }

  anredeLoeschen(name: string): void {
    this.api
      .anredeLoeschen(name)
      .subscribe({ next: () => this.neuLaden(), error: (err) => this.fehlerAnzeigen(err) });
  }

  // --- Spendentypen ---
  spendentypAnlegen(): void {
    if (!this.neuerSpendentyp.trim()) return;
    this.api.spendentypAnlegen(this.neuerSpendentyp.trim()).subscribe({
      next: () => {
        this.neuerSpendentyp = '';
        this.neuLaden();
      },
      error: (err) => this.fehlerAnzeigen(err),
    });
  }

  spendentypLoeschen(name: string): void {
    this.api
      .spendentypLoeschen(name)
      .subscribe({ next: () => this.neuLaden(), error: (err) => this.fehlerAnzeigen(err) });
  }

  // --- Bußgeldstatus ---
  bussgeldstatusAnlegen(): void {
    if (!this.neuerBussgeldstatus.trim()) return;
    this.api.bussgeldstatusAnlegen(this.neuerBussgeldstatus.trim()).subscribe({
      next: () => {
        this.neuerBussgeldstatus = '';
        this.neuLaden();
      },
      error: (err) => this.fehlerAnzeigen(err),
    });
  }

  bussgeldstatusLoeschen(name: string): void {
    this.api
      .bussgeldstatusLoeschen(name)
      .subscribe({ next: () => this.neuLaden(), error: (err) => this.fehlerAnzeigen(err) });
  }
}
