import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../auth.service';

/**
 * @author Nils
 *
 * Login-Maske: prüft Benutzername/Passwort per Basic Auth gegen /api/me.
 */
@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <div class="login-wrapper">
      <form class="card login-card" (ngSubmit)="anmelden()">
        <h1>Frauenhaus-Verwaltung</h1>
        <p class="hinweis">Bitte mit den Zugangsdaten des Backends anmelden.</p>

        <label for="username">Benutzername</label>
        <input id="username" name="username" [(ngModel)]="username" autocomplete="username" required />

        <label for="password">Passwort</label>
        <input id="password" name="password" type="password" [(ngModel)]="passwort"
               autocomplete="current-password" required />

        @if (fehler) {
          <p class="fehler">{{ fehler }}</p>
        }

        <button type="submit" [disabled]="laedt || !username || !passwort">
          {{ laedt ? 'Anmelden…' : 'Anmelden' }}
        </button>
      </form>
    </div>
  `,
  styles: `
    .login-wrapper { display: flex; justify-content: center; padding-top: 10vh; }
    .login-card { width: 22rem; display: flex; flex-direction: column; gap: 0.5rem; }
    .login-card h1 { font-size: 1.3rem; margin: 0 0 0.25rem; }
    .hinweis { color: var(--text-schwach); margin: 0 0 0.75rem; }
    button { margin-top: 0.75rem; }
  `,
})
export class LoginComponent {
  username = '';
  passwort = '';
  fehler = '';
  laedt = false;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {}

  anmelden(): void {
    this.laedt = true;
    this.fehler = '';
    this.auth.login(this.username, this.passwort).subscribe({
      next: () => void this.router.navigate(['/reports']),
      error: err => {
        this.laedt = false;
        this.fehler = err.status === 401 ? 'Benutzername oder Passwort falsch.' : 'Backend nicht erreichbar.';
      },
    });
  }
}
