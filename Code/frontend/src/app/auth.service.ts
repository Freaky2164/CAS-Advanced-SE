import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

/** Antwort von GET /api/me. */
export interface Me {
  username: string;
  roles: string[];
}

export const HEADER_KEY = 'auth.header';
export const USER_KEY = 'auth.user';

/**
 * @author Nils
 *
 * Verwaltet die Basic-Auth-Anmeldung gegen das Backend. Die Zugangsdaten werden
 * als fertiger Authorization-Header in der sessionStorage gehalten und vom
 * authInterceptor an jede Anfrage angehängt.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly user = signal<Me | null>(gespeicherterUser());

  constructor(private readonly http: HttpClient) {}

  get angemeldet(): boolean {
    return sessionStorage.getItem(HEADER_KEY) !== null;
  }

  /** Hinterlegt die Zugangsdaten und prüft sie gegen /api/me. */
  login(username: string, passwort: string): Observable<Me> {
    sessionStorage.setItem(HEADER_KEY, basicHeader(username, passwort));
    return this.http.get<Me>('/api/me').pipe(
      tap({
        next: me => {
          sessionStorage.setItem(USER_KEY, JSON.stringify(me));
          this.user.set(me);
        },
        error: () => this.logout(),
      }),
    );
  }

  logout(): void {
    sessionStorage.removeItem(HEADER_KEY);
    sessionStorage.removeItem(USER_KEY);
    this.user.set(null);
  }
}

/** Basic-Auth-Header, UTF-8-sicher (btoa allein scheitert an Umlauten). */
function basicHeader(username: string, passwort: string): string {
  const bytes = new TextEncoder().encode(`${username}:${passwort}`);
  return 'Basic ' + btoa(String.fromCodePoint(...bytes));
}

function gespeicherterUser(): Me | null {
  const raw = sessionStorage.getItem(USER_KEY);
  return raw ? (JSON.parse(raw) as Me) : null;
}
