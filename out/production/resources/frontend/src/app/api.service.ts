import { HttpClient, HttpErrorResponse, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

/**
 * @author Nils
 *
 * Zugriff auf die Report- und Stichwort-Endpunkte des Backends.
 * Report-Downloads werden als Blob geladen und direkt als Datei gespeichert.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private readonly http: HttpClient) {}

  /** E-Mail-Verteiler zu den gegebenen Stichworten. */
  verteilerEmails(stichworte: string[]): Observable<string[]> {
    const params = new HttpParams({ fromObject: { stichworte } });
    return this.http.get<string[]>('/api/reports/verteiler-emails', { params });
  }

  /** Neues Stichwort aus bestehenden zusammenstellen (alte bleiben erhalten). */
  zusammenstellen(neu: string, alte: string[]): Observable<{ zugeordnet: number }> {
    return this.http.post<{ zugeordnet: number }>('/api/stichworte/zusammenstellen', { neu, alte });
  }

  /** Stichworte zu einem neuen zusammenfassen, alte werden gelöscht. */
  zusammenfassen(neu: string, alte: string[]): Observable<{ zugeordnet: number }> {
    return this.http.post<{ zugeordnet: number }>('/api/stichworte/zusammenfassen', { neu, alte });
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
}

/** Fehlermeldung für die Anzeige im UI. */
export function fehlertext(err: HttpErrorResponse): string {
  if (err.status === 0) return 'Backend nicht erreichbar.';
  if (err.status === 401) return 'Anmeldung abgelaufen – bitte neu anmelden.';
  if (err.status === 404) return 'Nicht gefunden – bitte ID prüfen.';
  return `Fehler ${err.status}${err.message ? ': ' + err.message : ''}`;
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
