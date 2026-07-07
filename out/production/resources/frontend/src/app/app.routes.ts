import { Routes } from '@angular/router';

import { authGuard } from './auth.guard';
import { LoginComponent } from './login/login.component';
import { ReportsComponent } from './reports/reports.component';
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
  { path: '', pathMatch: 'full', redirectTo: 'reports' },
  { path: '**', redirectTo: 'reports' },
];
