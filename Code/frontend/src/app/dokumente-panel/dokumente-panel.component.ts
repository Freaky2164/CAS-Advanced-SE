import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';

import { ApiService, DokumentEntityTyp, DokumentMetadaten, fehlertext } from '../api.service';

/**
 * @author Nils
 *
 * Wiederverwendbare Anzeige und Pflege von Dokument-Anhängen zu Stammdaten.
 */
@Component({
  selector: 'app-dokumente-panel',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './dokumente-panel.component.html',
  styleUrl: './dokumente-panel.component.css',
})
export class DokumentePanelComponent implements OnChanges {
  @Input({ required: true }) titel = 'Dokumente';
  @Input({ required: true }) entityTyp!: DokumentEntityTyp;
  @Input({ required: true }) entityId!: string | number;

  dokumente: DokumentMetadaten[] = [];
  laedt = false;
  hochladenLaeuft = false;
  fehler = '';
  datei: File | null = null;

  constructor(
    private readonly api: ApiService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['entityTyp'] || changes['entityId']) && this.entityTyp && this.entityId !== null && this.entityId !== undefined) {
      this.laden();
    }
  }

  dateiGewaehlt(input: HTMLInputElement): void {
    this.datei = input.files?.item(0) ?? null;
    this.fehler = '';
    this.cdr.markForCheck();
  }

  hochladen(input: HTMLInputElement): void {
    if (!this.datei) return;
    this.hochladenLaeuft = true;
    this.fehler = '';
    const datei = this.datei;
    this.api.dokumentHochladen(this.entityTyp, this.entityId, datei).subscribe({
      next: dokument => {
        this.dokumente = [dokument, ...this.dokumente];
        this.datei = null;
        input.value = '';
        this.hochladenLaeuft = false;
        this.cdr.markForCheck();
      },
      error: err => {
        this.fehler = fehlertext(err);
        this.hochladenLaeuft = false;
        this.cdr.markForCheck();
      },
    });
  }

  herunterladen(dokument: DokumentMetadaten): void {
    this.api.dokumentHerunterladen(dokument.id).subscribe({
      error: err => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }

  loeschen(dokument: DokumentMetadaten): void {
    if (!confirm(`Dokument „${dokument.dateiname}“ wirklich löschen?`)) return;
    this.api.dokumentLoeschen(dokument.id).subscribe({
      next: () => {
        this.dokumente = this.dokumente.filter(d => d.id !== dokument.id);
        this.cdr.markForCheck();
      },
      error: err => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }

  groessenText(groesse: number): string {
    if (groesse >= 1024 * 1024) return `${(groesse / (1024 * 1024)).toFixed(1)} MB`;
    if (groesse >= 1024) return `${(groesse / 1024).toFixed(1)} KB`;
    return `${groesse} B`;
  }

  private laden(): void {
    this.fehler = '';
    this.laedt = true;
    this.dokumente = [];
    this.api.dokumente(this.entityTyp, this.entityId).subscribe({
      next: dokumente => {
        this.dokumente = dokumente;
        this.laedt = false;
        this.cdr.markForCheck();
      },
      error: err => {
        this.fehler = fehlertext(err);
        this.laedt = false;
        this.cdr.markForCheck();
      },
    });
  }
}
