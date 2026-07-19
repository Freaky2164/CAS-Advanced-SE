import { DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ApiService, AppUser, AppUserRole, fehlertext } from '../api.service';
import { AuthService } from '../auth.service';

/**
 * @author Nils
 *
 * Admin-only Benutzerverwaltung (alt: Benutzer wurden nur direkt in
 * compucrash.user_def gepflegt, es gab keine UI dafür). Anlegen, Rolle/
 * Aktiv-Status ändern, Passwort zurücksetzen.
 */
@Component({
  selector: 'app-benutzer',
  imports: [FormsModule, DatePipe],
  templateUrl: './benutzer.component.html',
  styleUrl: './benutzer.component.css',
})
export class BenutzerComponent implements OnInit {
  benutzer: AppUser[] = [];
  fehler = '';
  erfolg = '';

  neuerBenutzername = '';
  neuesPasswort = '';
  neueRolle: AppUserRole = 'SACHBEARBEITUNG';

  passwortEingabe: Record<number, string> = {};

  constructor(
    private readonly api: ApiService,
    private readonly cdr: ChangeDetectorRef,
    readonly auth: AuthService,
  ) {}

  ngOnInit(): void {
    this.neuLaden();
  }

  neuLaden(): void {
    this.fehler = '';
    this.api.benutzer().subscribe({
      next: (b) => {
        this.benutzer = b;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }

  istEigenerBenutzer(u: AppUser): boolean {
    return u.username === this.auth.user()?.username;
  }

  anlegen(): void {
    this.fehler = '';
    this.erfolg = '';
    this.api
      .benutzerAnlegen(this.neuerBenutzername.trim(), this.neuesPasswort, this.neueRolle)
      .subscribe({
        next: () => {
          this.neuerBenutzername = '';
          this.neuesPasswort = '';
          this.neueRolle = 'SACHBEARBEITUNG';
          this.erfolg = 'Benutzer angelegt.';
          this.neuLaden();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.fehler = fehlertext(err);
          this.cdr.markForCheck();
        },
      });
  }

  aendern(u: AppUser): void {
    this.fehler = '';
    this.erfolg = '';
    this.api.benutzerAendern(u.id, u.role, u.enabled).subscribe({
      next: () => {
        this.erfolg = `Benutzer „${u.username}“ aktualisiert.`;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }

  passwortZuruecksetzen(u: AppUser): void {
    const neuesPasswort = this.passwortEingabe[u.id];
    if (!neuesPasswort || neuesPasswort.length < 8) {
      this.fehler = 'Neues Passwort muss mindestens 8 Zeichen lang sein.';
      return;
    }
    this.fehler = '';
    this.erfolg = '';
    this.api.benutzerPasswortZuruecksetzen(u.id, neuesPasswort).subscribe({
      next: () => {
        this.passwortEingabe[u.id] = '';
        this.erfolg = `Passwort für „${u.username}“ zurückgesetzt.`;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.fehler = fehlertext(err);
        this.cdr.markForCheck();
      },
    });
  }
}
