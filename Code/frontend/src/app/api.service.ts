import { HttpClient, HttpErrorResponse, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

/** Ausschnitt der Spring-Data-Page-Antwort, den das Frontend benötigt. */
export interface Seite<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface Anrede { name: string; }
export interface Spendentyp { name: string; }
export interface Bussgeldstatus { name: string; }
export interface Verein { name: string; bezeichnung: string; }
export interface Gericht { id: number; bezeichnung: string; strasse: string | null; plz: string | null; ort: string | null; }
export interface Spendenart { name: string; spendentyp: string; }
export interface VerlaufAenderung {
  feld: string;
  alt: string | number | boolean | null;
  neu: string | number | boolean | null;
}
export interface VerlaufEintrag {
  revision: number;
  zeitpunkt: string;
  benutzername: string;
  typ: 'INSERT' | 'UPDATE' | 'DELETE';
  aenderungen: VerlaufAenderung[];
}
export type DokumentEntityTyp = 'MITGLIED' | 'VEREIN' | 'BUSSGELD' | 'SPENDE' | 'GERICHT';
export interface DokumentMetadaten {
  id: number;
  entityTyp: DokumentEntityTyp;
  entityId: string;
  dateiname: string;
  contentType: string;
  groesse: number;
  hochgeladenAm: string;
  hochgeladenVon: string;
}

export interface Mitglied {
  id: number;
  anrede: string | null;
  vorname: string | null;
  name: string;
  name2: string | null;
  name3: string | null;
  briefanrede: string | null;
  strasse: string | null;
  plz: string | null;
  ort: string | null;
  email: string | null;
  tel1: string | null;
  tel2: string | null;
  fax: string | null;
  foerderverein: boolean;
  frauenhaus: boolean;
  bemerkung: string | null;
  stichworte: string[];
  vereine: string[];
}
export type MitgliedRequest = Omit<Mitglied, 'id'>;

export interface Spende {
  id: number;
  mitgliedId: number;
  mitgliedName: string;
  spendenart: string;
  verein: string;
  datum: string;
  betrag: number;
  bemerkung: string | null;
}
export type SpendeRequest = Omit<Spende, 'id' | 'mitgliedName'>;

export interface Eingang { id: number; datum: string; betrag: number; bemerkung: string | null; }
export interface Bussgeld {
  id: number;
  gerichtId: number;
  gerichtBezeichnung: string;
  verein: string;
  status: string | null;
  name: string | null;
  vorname: string | null;
  aktenzeichen: string | null;
  datum: string;
  zieldatum: string | null;
  betrag: number;
  bezahlt: boolean;
  bemerkung: string | null;
  eingaenge: Eingang[];
}
export type BussgeldRequest = Omit<Bussgeld, 'id' | 'gerichtBezeichnung' | 'eingaenge'>;
export type EingangRequest = Omit<Eingang, 'id'>;

export type AppUserRole = 'ADMIN' | 'SACHBEARBEITUNG';
export interface AppUser {
  id: number;
  username: string;
  role: AppUserRole;
  enabled: boolean;
  createdAt: string;
}
export interface VerteilerVersandRequest {
  stichworte: string[];
  traeger: string;
  betreff: string;
  text: string;
}
export interface VerteilerVersandErgebnis {
  empfaengerAnzahl: number;
}

/**
 * @author Nils
 *
 * Zugriff auf die Report-, Stichwort- und Stammdaten-CRUD-Endpunkte des Backends.
 * Report-Downloads werden als Blob geladen und direkt als Datei gespeichert.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private readonly http: HttpClient) {}

  // --- Anreden / Spendentypen / Bußgeldstatus (einfache Lookups) ---
  anreden(): Observable<Anrede[]> { return this.http.get<Anrede[]>('/api/anreden'); }
  anredeAnlegen(name: string): Observable<Anrede> { return this.http.post<Anrede>('/api/anreden', { name }); }
  anredeLoeschen(name: string): Observable<void> { return this.http.delete<void>(`/api/anreden/${encodeURIComponent(name)}`); }

  spendentypen(): Observable<Spendentyp[]> { return this.http.get<Spendentyp[]>('/api/spendentypen'); }
  spendentypAnlegen(name: string): Observable<Spendentyp> { return this.http.post<Spendentyp>('/api/spendentypen', { name }); }
  spendentypLoeschen(name: string): Observable<void> { return this.http.delete<void>(`/api/spendentypen/${encodeURIComponent(name)}`); }

  bussgeldstati(): Observable<Bussgeldstatus[]> { return this.http.get<Bussgeldstatus[]>('/api/bussgeldstatus'); }
  bussgeldstatusAnlegen(name: string): Observable<Bussgeldstatus> { return this.http.post<Bussgeldstatus>('/api/bussgeldstatus', { name }); }
  bussgeldstatusLoeschen(name: string): Observable<void> { return this.http.delete<void>(`/api/bussgeldstatus/${encodeURIComponent(name)}`); }

  // --- Vereine ---
  vereine(suche?: string | null): Observable<Verein[]> {
    return this.http.get<Verein[]>('/api/vereine', { params: this.paramsMitSuche({}, suche) });
  }
  vereinAnlegen(verein: Verein): Observable<Verein> { return this.http.post<Verein>('/api/vereine', verein); }
  vereinAendern(name: string, bezeichnung: string): Observable<Verein> {
    return this.http.put<Verein>(`/api/vereine/${encodeURIComponent(name)}`, { bezeichnung });
  }
  vereinVerlauf(name: string): Observable<VerlaufEintrag[]> {
    return this.http.get<VerlaufEintrag[]>(`/api/vereine/${encodeURIComponent(name)}/verlauf`);
  }
  vereinLoeschen(name: string): Observable<void> { return this.http.delete<void>(`/api/vereine/${encodeURIComponent(name)}`); }

  // --- Gerichte ---
  gerichte(suche?: string | null): Observable<Gericht[]> {
    return this.http.get<Gericht[]>('/api/gerichte', { params: this.paramsMitSuche({}, suche) });
  }
  gerichtAnlegen(g: Omit<Gericht, 'id'>): Observable<Gericht> { return this.http.post<Gericht>('/api/gerichte', g); }
  gerichtAendern(id: number, g: Omit<Gericht, 'id'>): Observable<Gericht> { return this.http.put<Gericht>(`/api/gerichte/${id}`, g); }
  gerichtVerlauf(id: number): Observable<VerlaufEintrag[]> { return this.http.get<VerlaufEintrag[]>(`/api/gerichte/${id}/verlauf`); }
  gerichtLoeschen(id: number): Observable<void> { return this.http.delete<void>(`/api/gerichte/${id}`); }

  // --- Spendenarten ---
  spendenarten(): Observable<Spendenart[]> { return this.http.get<Spendenart[]>('/api/spendenarten'); }
  spendenartAnlegen(sa: Spendenart): Observable<Spendenart> { return this.http.post<Spendenart>('/api/spendenarten', sa); }
  spendenartAendern(name: string, spendentyp: string): Observable<Spendenart> {
    return this.http.put<Spendenart>(`/api/spendenarten/${encodeURIComponent(name)}`, { spendentyp });
  }
  spendenartLoeschen(name: string): Observable<void> { return this.http.delete<void>(`/api/spendenarten/${encodeURIComponent(name)}`); }

  // --- Mitglieder ---
  mitglieder(seite: number, groesse = 20, suche?: string | null): Observable<Seite<Mitglied>> {
    return this.http.get<Seite<Mitglied>>('/api/mitglieder', {
      params: this.paramsMitSuche({ page: seite, size: groesse, sort: 'name,vorname' }, suche),
    });
  }
  mitglied(id: number): Observable<Mitglied> { return this.http.get<Mitglied>(`/api/mitglieder/${id}`); }
  mitgliedVerlauf(id: number): Observable<VerlaufEintrag[]> { return this.http.get<VerlaufEintrag[]>(`/api/mitglieder/${id}/verlauf`); }
  mitgliedAnlegen(m: MitgliedRequest): Observable<Mitglied> { return this.http.post<Mitglied>('/api/mitglieder', m); }
  mitgliedAendern(id: number, m: MitgliedRequest): Observable<Mitglied> { return this.http.put<Mitglied>(`/api/mitglieder/${id}`, m); }
  mitgliedDuplizieren(id: number): Observable<Mitglied> { return this.http.post<Mitglied>(`/api/mitglieder/${id}/duplizieren`, {}); }
  mitgliedLoeschen(id: number): Observable<void> { return this.http.delete<void>(`/api/mitglieder/${id}`); }

  // --- Spenden ---
  spenden(seite: number, groesse = 20, suche?: string | null): Observable<Seite<Spende>> {
    return this.http.get<Seite<Spende>>('/api/spenden', {
      params: this.paramsMitSuche({ page: seite, size: groesse, sort: 'datum,desc' }, suche),
    });
  }
  spendeVerlauf(id: number): Observable<VerlaufEintrag[]> { return this.http.get<VerlaufEintrag[]>(`/api/spenden/${id}/verlauf`); }
  spendeAnlegen(s: SpendeRequest): Observable<Spende> { return this.http.post<Spende>('/api/spenden', s); }
  spendeAendern(id: number, s: SpendeRequest): Observable<Spende> { return this.http.put<Spende>(`/api/spenden/${id}`, s); }
  spendeLoeschen(id: number): Observable<void> { return this.http.delete<void>(`/api/spenden/${id}`); }

  // --- Bußgelder ---
  bussgelder(seite: number, groesse = 20, suche?: string | null): Observable<Seite<Bussgeld>> {
    return this.http.get<Seite<Bussgeld>>('/api/bussgelder', {
      params: this.paramsMitSuche({ page: seite, size: groesse, sort: 'datum,desc' }, suche),
    });
  }
  bussgeldVerlauf(id: number): Observable<VerlaufEintrag[]> { return this.http.get<VerlaufEintrag[]>(`/api/bussgelder/${id}/verlauf`); }
  bussgeldAnlegen(b: BussgeldRequest): Observable<Bussgeld> { return this.http.post<Bussgeld>('/api/bussgelder', b); }
  bussgeldAendern(id: number, b: BussgeldRequest): Observable<Bussgeld> { return this.http.put<Bussgeld>(`/api/bussgelder/${id}`, b); }
  bussgeldLoeschen(id: number): Observable<void> { return this.http.delete<void>(`/api/bussgelder/${id}`); }
  eingangHinzufuegen(bussgeldId: number, e: EingangRequest): Observable<Bussgeld> {
    return this.http.post<Bussgeld>(`/api/bussgelder/${bussgeldId}/eingaenge`, e);
  }
  eingangEntfernen(bussgeldId: number, eingangId: number): Observable<Bussgeld> {
    return this.http.delete<Bussgeld>(`/api/bussgelder/${bussgeldId}/eingaenge/${eingangId}`);
  }

  // --- Dokument-Anhänge ---
  dokumente(entityTyp: DokumentEntityTyp, entityId: string | number): Observable<DokumentMetadaten[]> {
    return this.http.get<DokumentMetadaten[]>(`/api/dokumente/${entityTyp}/${encodeURIComponent(String(entityId))}`);
  }
  dokumentHochladen(entityTyp: DokumentEntityTyp, entityId: string | number, file: File): Observable<DokumentMetadaten> {
    const formData = new FormData();
    formData.append('datei', file);
    return this.http.post<DokumentMetadaten>(`/api/dokumente/${entityTyp}/${encodeURIComponent(String(entityId))}`, formData);
  }
  dokumentHerunterladen(id: number): Observable<void> { return this.download(`/api/dokumente/${id}/download`, {}); }
  dokumentLoeschen(id: number): Observable<void> { return this.http.delete<void>(`/api/dokumente/${id}`); }

  /** E-Mail-Verteiler zu den gegebenen Stichworten. */
  verteilerEmails(stichworte: string[]): Observable<string[]> {
    const params = new HttpParams({ fromObject: { stichworte } });
    return this.http.get<string[]>('/api/reports/verteiler-emails', { params });
  }

  /** Verteiler-E-Mail an alle Empfänger per BCC versenden. */
  verteilerVersenden(request: VerteilerVersandRequest): Observable<VerteilerVersandErgebnis> {
    return this.http.post<VerteilerVersandErgebnis>('/api/reports/verteiler/versenden', request);
  }

  /** Mitgliedersuche über ein oder mehrere Stichworte, optional auf Förderverein/Frauenhaus eingeschränkt. */
  stichwortsuche(stichworte: string[], foerderverein: boolean, frauenhaus: boolean): Observable<Mitglied[]> {
    const params = new HttpParams({
      fromObject: { stichworte, foerderverein: String(foerderverein), frauenhaus: String(frauenhaus) },
    });
    return this.http.get<Mitglied[]>('/api/reports/stichwortsuche', { params });
  }

  /** Neues Stichwort aus bestehenden zusammenstellen (alte bleiben erhalten). */
  zusammenstellen(neu: string, alte: string[]): Observable<{ zugeordnet: number }> {
    return this.http.post<{ zugeordnet: number }>('/api/stichworte/zusammenstellen', { neu, alte });
  }

  /** Stichworte zu einem neuen zusammenfassen, alte werden gelöscht. */
  zusammenfassen(neu: string, alte: string[]): Observable<{ zugeordnet: number }> {
    return this.http.post<{ zugeordnet: number }>('/api/stichworte/zusammenfassen', { neu, alte });
  }

  // --- Benutzerverwaltung (nur Rolle ADMIN) ---
  benutzer(): Observable<AppUser[]> { return this.http.get<AppUser[]>('/api/admin/users'); }
  benutzerAnlegen(username: string, passwort: string, role: AppUserRole): Observable<AppUser> {
    return this.http.post<AppUser>('/api/admin/users', { username, passwort, role });
  }
  benutzerAendern(id: number, role: AppUserRole, enabled: boolean): Observable<AppUser> {
    return this.http.put<AppUser>(`/api/admin/users/${id}`, { role, enabled });
  }
  benutzerPasswortZuruecksetzen(id: number, neuesPasswort: string): Observable<AppUser> {
    return this.http.put<AppUser>(`/api/admin/users/${id}/passwort`, { neuesPasswort });
  }

  /** Lädt einen Report herunter und stößt das Speichern im Browser an. */
  download(pfad: string, params: Record<string, string | readonly string[]>): Observable<void> {
    return this.http
      .get(pfad, {
        params: new HttpParams({ fromObject: params }),
        responseType: 'blob',
        observe: 'response',
      })
      .pipe(map(res => speichern(res)));
  }

  private paramsMitSuche(params: Record<string, string | number>, suche?: string | null): HttpParams {
    let httpParams = new HttpParams({
      fromObject: Object.fromEntries(Object.entries(params).map(([key, value]) => [key, String(value)])),
    });
    const suchbegriff = suche?.trim();
    if (suchbegriff) {
      httpParams = httpParams.set('suche', suchbegriff);
    }
    return httpParams;
  }
}

/** Fehlermeldung für die Anzeige im UI. */
export function fehlertext(err: HttpErrorResponse): string {
  const detail = typeof err.error === 'string'
    ? err.error
    : typeof err.error?.detail === 'string'
      ? err.error.detail
      : typeof err.error?.message === 'string'
        ? err.error.message
        : '';
  if (err.status === 0) return 'Backend nicht erreichbar.';
  if (err.status === 401) return 'Anmeldung abgelaufen – bitte neu anmelden.';
  if (err.status === 404) return 'Nicht gefunden – bitte ID prüfen.';
  if (err.status === 413) return detail || 'Datei zu groß – maximal 10 MB.';
  return `Fehler ${err.status}${detail ? ': ' + detail : err.message ? ': ' + err.message : ''}`;
}

/** Speichert die Antwort als Datei; Dateiname aus dem Content-Disposition-Header. */
function speichern(res: HttpResponse<Blob>): void {
  const disposition = res.headers.get('Content-Disposition') ?? '';
  const name = /filename="?([^";]+)"?/.exec(disposition)?.[1] ?? 'report';
  const url = URL.createObjectURL(res.body!);
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.click();
  URL.revokeObjectURL(url);
}
