import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/**
 * @author Nils
 *
 * Rahmen für die Stammdaten-Verwaltung mit Unternavigation zu Mitgliedern,
 * Spenden, Bußgeldern und den übrigen Stammdaten-Listen (alt: CMainFrame-Menü
 * mit den einzelnen CInfoFrame/CListFrame-Masken je Datenobjekt).
 */
@Component({
  selector: 'app-stammdaten',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './stammdaten.component.html',
  styleUrl: './stammdaten.component.css',
})
export class StammdatenComponent {}
