import { Routes } from '@angular/router';

import { adminGuard } from './admin.guard';
import { authGuard } from './auth.guard';
import { BenutzerComponent } from './benutzer/benutzer.component';
import { LoginComponent } from './login/login.component';
import { ReportsComponent } from './reports/reports.component';
import { BussgelderComponent } from './stammdaten/bussgelder/bussgelder.component';
import { MitgliederComponent } from './stammdaten/mitglieder/mitglieder.component';
import { SpendenComponent } from './stammdaten/spenden/spenden.component';
import { StammdatenComponent } from './stammdaten/stammdaten.component';
import { VerwaltungComponent } from './stammdaten/verwaltung/verwaltung.component';
import { StichworteComponent } from './stichworte/stichworte.component';

/**
 * @author Nils
 *
 * Routen der Anwendung; alles außer dem Login ist durch den authGuard geschützt.
 */
export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'reports', component: ReportsComponent, canActivate: [authGuard] },
  { path: 'stichworte', component: StichworteComponent, canActivate: [authGuard] },
  { path: 'benutzer', component: BenutzerComponent, canActivate: [authGuard, adminGuard] },
  {
    path: 'stammdaten',
    component: StammdatenComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'mitglieder' },
      { path: 'mitglieder', component: MitgliederComponent },
      { path: 'spenden', component: SpendenComponent },
      { path: 'bussgelder', component: BussgelderComponent },
      { path: 'verwaltung', component: VerwaltungComponent },
    ],
  },
  { path: '', pathMatch: 'full', redirectTo: 'reports' },
  { path: '**', redirectTo: 'reports' },
];
