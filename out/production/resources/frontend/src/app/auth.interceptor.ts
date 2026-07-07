import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { HEADER_KEY, USER_KEY } from './auth.service';

/**
 * @author Nils
 *
 * Hängt den Basic-Auth-Header an jede Anfrage. Bei 401 (außer beim Login-Check
 * selbst) werden die Zugangsdaten verworfen und zur Login-Seite umgeleitet.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const header = sessionStorage.getItem(HEADER_KEY);
  const anfrage = header ? req.clone({ setHeaders: { Authorization: header } }) : req;

  return next(anfrage).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !req.url.endsWith('/api/me')) {
        sessionStorage.removeItem(HEADER_KEY);
        sessionStorage.removeItem(USER_KEY);
        void router.navigate(['/login']);
      }
      return throwError(() => err);
    }),
  );
};
