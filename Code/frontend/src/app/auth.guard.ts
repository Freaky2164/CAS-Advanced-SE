import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/**
 * @author Nils
 *
 * Lässt Routen nur mit hinterlegten Zugangsdaten zu, sonst Umleitung zum Login.
 */
export const authGuard: CanActivateFn = () =>
  inject(AuthService).angemeldet ? true : inject(Router).createUrlTree(['/login']);
