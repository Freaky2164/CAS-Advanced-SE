import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/**
 * @author Nils
 *
 * Lässt Routen nur für angemeldete Benutzer mit Rolle ADMIN zu, sonst
 * Umleitung zu den Reports.
 */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.angemeldet && auth.user()?.roles.includes('ROLE_ADMIN')) return true;
  return inject(Router).createUrlTree(['/reports']);
};
