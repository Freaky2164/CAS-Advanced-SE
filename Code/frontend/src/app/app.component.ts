import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './auth.service';

/**
 * @author Nils
 *
 * App-Rahmen mit Kopfzeile, Navigation und Abmelden-Knopf.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  constructor(
    protected auth: AuthService,
    private readonly router: Router,
  ) {}

  abmelden(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
