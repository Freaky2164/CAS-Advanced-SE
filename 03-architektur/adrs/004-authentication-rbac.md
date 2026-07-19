# ADR-005: Authentifizierung & Autorisierung – Zustandslose HTTP-Basic-Auth mit BCrypt und RBAC

## Status

**Akzeptiert** – Juni 2026

## Kontext

Gemäß ADR-001 übernimmt die neue Anwendungsschicht (Backend) die zentrale Sicherheitsbarriere,
die im IST-System vollständig fehlt. Konkret fordert das Requirements-Dokument:

- **FR-1.1 Benutzeranmeldung**: Authentifizierung mittels Benutzername und Passwort.
- **FR-1.2 Rollenverwaltung**: Unterstützung der Rollen *Administrator* und *Sachbearbeiter*,
  Berechtigungen müssen rollenbasiert vergeben werden.
- **NFR-1 Sicherheit/DSGVO**: Verarbeitung personenbezogener Daten nach DSGVO.

Das IST-System (`CLoginFrame` / `compucrash.user_def`) bietet keinerlei Rollenkonzept auf
Anwendungsebene und speichert keine sinnvoll gehashten Passwörter – jeder Client, der eine
DB-Verbindung aufbauen kann, hat vollen Zugriff (siehe ADR-001). Diese Schwachstelle muss
durch einen zentralen, im Backend erzwungenen Authentifizierungs- und Autorisierungsmechanismus
vollständig ersetzt werden.

Gemäß ADR-005 wird das System ausschließlich **On-Premises im lokalen Netzwerk** betrieben,
ist **nicht über das Internet erreichbar** und hat nur wenige, bekannte Nutzer (Mitarbeitende
des Vereins). Dieser eingeschränkte Bedrohungskontext beeinflusst maßgeblich, welches
Authentifizierungsverfahren angemessen ist – ein für öffentlich erreichbare Multi-Tenant-Systeme
optimiertes Verfahren (z.B. JWT mit Refresh-Tokens) wäre hier unverhältnismäßig komplex.

## Entscheidung

Wir entscheiden uns für **zustandslose HTTP-Basic-Authentifizierung über HTTPS** in Kombination
mit **BCrypt-gehashten Passwörtern** und einem **einfachen rollenbasierten Zugriffsmodell (RBAC)**
mit genau zwei Rollen: `ADMIN` und `SACHBEARBEITUNG`.

```
Angular SPA ──(Authorization: Basic <user:pass base64>)──> Spring Security Filter Chain
                                                                   │
                                                    SessionCreationPolicy.STATELESS
                                                                   │
                                              DbUserDetailsService ── liest app.app_user
                                                                   │
                                        BCryptPasswordEncoder.matches(...)
                                                                   │
                                    hasRole("ADMIN") für /api/admin/**
                                    authenticated() für alle übrigen Endpunkte
```

Zentrale Bausteine:

- **`SecurityConfig`**: Spring Security 6 Filter Chain, CSRF deaktiviert (rein zustandslose REST-API),
  `SessionCreationPolicy.STATELESS`, `httpBasic()` mit eigenem `AuthenticationEntryPoint`
  (liefert `401` **ohne** `WWW-Authenticate: Basic`-Header, damit der Browser keinen nativen
  Auth-Dialog öffnet – die Angular-SPA übernimmt Login-UI und Credential-Handling selbst).
- **`AppUser`**: JPA-Entity (`app.app_user`) mit `username`, `passwordHash` (BCrypt),
  `role` (Enum `ADMIN` / `SACHBEARBEITUNG`), `enabled`-Flag, ersetzt `compucrash.user_def`.
- **`DbUserDetailsService`**: Lädt Benutzer aus der Datenbank statt in-memory/statisch zu
  konfigurieren – Passwortänderungen und neue Benutzer sind ohne Neustart wirksam.
- **`AdminBootstrap`**: Legt beim allerersten Start automatisch einen Admin-Account an, sofern
  die Benutzertabelle leer ist. Passwort kommt aus `APP_ADMIN_PASSWORD` (Umgebungsvariable) oder
  wird als Einmal-Zufallswert generiert und **einmalig geloggt** – es gibt kein eingebranntes
  Standardpasswort (vermeidet CWE-798 *Use of Hard-coded Credentials*).
- **`@EnableMethodSecurity`**: Ermöglicht zusätzlich feingranulare Autorisierung auf Service-/
  Controller-Methodenebene (`@PreAuthorize`), nicht nur auf URL-Pattern-Ebene.

## Betrachtete Alternativen

### Alternative A: JWT (Bearer Token, zustandslos mit Refresh-Token)

Backend stellt beim Login ein signiertes JWT aus, das die Rolle als Claim enthält;
Refresh-Tokens ermöglichen erneuerbare Sessions ohne erneute Passworteingabe.

| Aspekt | Bewertung |
|--------|-----------|
| Skalierbarkeit über mehrere Server | ✅ Kein zentraler Session-Store nötig |
| Implementierungsaufwand | ❌ Signing-Key-Verwaltung, Token-Refresh-Flow, Blacklisting bei Logout |
| Widerruf vor Ablauf | ❌ Erfordert zusätzliche Token-Blacklist oder kurze Lebensdauer + Refresh-Komplexität |
| Eignung für Kleinstsystem (1 Server, wenige Nutzer) | ❌ Löst ein Problem (horizontale Skalierung), das hier nicht existiert |
| Frontend-Komplexität | ❌ Token-Speicherung (localStorage vs. Cookie), Refresh-Interceptor nötig |
| Sicherheitsgewinn ggü. Basic Auth im LAN | ⚠️ Marginal, da kein Multi-Server-Szenario vorliegt |

**Ablehnung**: JWT löst primär Probleme verteilter/skalierender Systeme (mehrere Backend-Instanzen,
Third-Party-APIs, mobile Clients mit langlebigen Sessions). Für einen einzelnen Windows-Dienst mit
wenigen bekannten Nutzern im LAN steht der Mehraufwand (Signing-Key-Rotation, Refresh-Logik,
Token-Widerruf) in keinem Verhältnis zum Sicherheitsgewinn.

### Alternative B: Server-seitige Session mit Cookie (Spring Session)

Klassische Server-Session: Login erzeugt eine Session-ID, die als `HttpOnly`-Cookie an den
Browser übergeben wird; der Server hält den Auth-Status serverseitig vor.

| Aspekt | Bewertung |
|--------|-----------|
| Widerruf/Logout | ✅ Server kann Session jederzeit sofort invalidieren |
| CSRF-Schutz nötig | ❌ Cookie-basierte Auth erfordert aktiven CSRF-Schutz (bei reiner REST-API sonst deaktiviert) |
| Statefulness | ⚠️ Server muss Session-Zustand vorhalten (In-Memory reicht bei 1 Dienst, aber Kopplung an Prozesslaufzeit) |
| Implementierungsaufwand | ⚠️ Mittel – Cookie-Handling, CSRF-Token im Frontend, SameSite-Konfiguration |
| Angular-Integration | ⚠️ `withCredentials`, CSRF-Interceptor zusätzlich nötig |

**Nicht gewählt**: Technisch valide und im Grunde vergleichbar sicher, bringt aber zusätzliche
Komplexität (CSRF-Handling, Session-Zustand im Prozess) ohne relevanten Mehrwert gegenüber der
zustandslosen Basic-Auth-Variante für dieses Einzelserver-Szenario.

### Alternative C: Zustandslose HTTP-Basic-Auth + BCrypt (gewählt) ✅

| Aspekt | Bewertung |
|--------|-----------|
| Implementierungsaufwand | ✅ Minimal – native Spring-Security-Unterstützung, keine Token-/Session-Infrastruktur |
| Statefulness | ✅ Vollständig zustandslos (`STATELESS`) – kein Session-Speicher, keine Ausfallszenarien durch Session-Verlust |
| CSRF-Risiko | ✅ Entfällt strukturell (kein Cookie, kein automatischer Credential-Versand durch den Browser) |
| Passwort-Sicherheit | ✅ BCrypt (adaptiv, gesalzen) statt Klartext wie im IST-System |
| Transport-Sicherheit | ✅ Ausschließlich über HTTPS im geschlossenen LAN (ADR-005) – kein Internet-Angriffsvektor |
| Eignung für Kleinstsystem | ✅ Exakt proportional zur tatsächlichen Bedrohungslage (wenige Nutzer, kein externer Zugriff) |
| Logout / Token-Widerruf | ⚠️ Kein serverseitiger "Logout" nötig – Client verwirft die Credentials lokal |
| Passwortwechsel | ✅ Sofort wirksam (DB-Lookup bei jeder Anfrage, kein zwischengespeicherter Token) |

## Begründung

### 1. Verhältnismäßigkeit zum Bedrohungskontext

Das System läuft ausschließlich On-Premises, ist nicht über das Internet erreichbar (ADR-005)
und wird von einer kleinen Anzahl bekannter Mitarbeitender im LAN genutzt. Die für
Internet-exponierte Multi-Server-Systeme entwickelten Verfahren (JWT mit Refresh-Tokens,
verteilte Session-Stores) lösen Probleme, die in diesem Kontext nicht existieren. Ein einfaches,
gut verstandenes Verfahren reduziert die Angriffsfläche durch geringere Komplexität
("Einfachheit als Sicherheitsmerkmal").

### 2. Vollständige Elimination der IST-Schwachstelle

Die zentrale, im ADR-001 identifizierte Schwachstelle – kein Rollenkonzept, keine zentrale
Zugriffskontrolle – wird durch die Kombination aus zentralem `SecurityFilterChain`,
DB-gestützter Benutzerverwaltung und rollenbasierter Autorisierung (`hasRole("ADMIN")`,
`@PreAuthorize`) vollständig geschlossen. Kein Client hat mehr direkten, ungeprüften Zugriff.

### 3. BCrypt statt Klartext

BCrypt ist ein adaptiver, gesalzener Hash-Algorithmus, der gezielt für Passwort-Hashing entwickelt
wurde (konfigurierbarer Kostenfaktor gegen Brute-Force, automatisches Salting gegen
Rainbow-Table-Angriffe). Dies ersetzt die im IST-System vollständig fehlende Passwort-Sicherung
(Klartext-Credentials in lokalen `.ini`-Dateien) durch Industriestandard.

### 4. Kein eingebranntes Standardpasswort

`AdminBootstrap` erzeugt den initialen Administrator-Account **nur beim allerersten Start**
mit einem konfigurierbaren oder zufällig generierten Einmal-Passwort. Dies vermeidet eine der
häufigsten Schwachstellenklassen in Kleinanwendungen (CWE-798, hart codierte
Standard-Zugangsdaten wie `admin/admin`).

### 5. Zwei Rollen genügen der fachlichen Anforderung

FR-1.2 fordert exakt zwei Rollen (Administrator, Sachbearbeiter). Ein komplexeres RBAC-Modell
mit feingranularen Permissions, Rollenhierarchien oder dynamischer Rechtevergabe würde eine
nicht existierende Anforderung vorwegnehmen ("Overengineering"). `@EnableMethodSecurity` erlaubt
bei Bedarf spätere Erweiterung auf feingranulare Berechtigungen, ohne das Grundmodell zu ändern.

### 6. Zustandslosigkeit vereinfacht Betrieb

Da keine Server-Session vorgehalten wird, überlebt ein Neustart des Windows-Dienstes (z.B. nach
einem Update, siehe ADR-002) angemeldete Nutzer unterbrechungsfrei – der Browser sendet die
Anmeldedaten bei der nächsten Anfrage einfach erneut mit. Es gibt keinen Session-Speicher, der
bei einem Absturz verloren gehen könnte.

## Risiken und Mitigationen

| Risiko | Wahrscheinlichkeit | Auswirkung | Mitigation |
|--------|--------------------|-----------|-------------|
| Credential-Sniffing im LAN | Niedrig | Hoch | Ausschließlich HTTPS, auch innerhalb des LAN (Transportverschlüsselung) |
| Brute-Force auf Login-Endpunkt | Niedrig (kein Internet-Zugriff) | Mittel | BCrypt-Kostenfaktor, optional Rate-Limiting auf `/login`-Pfad |
| Kein automatischer Logout bei Inaktivität | Mittel | Niedrig | Organisatorisch: Bildschirmsperre der Arbeitsplätze (bestehende Policy) |
| Verlust des Admin-Passworts | Niedrig | Mittel | `AdminBootstrap` kann bei leerer Benutzertabelle erneut ausgeführt werden (Notfall-Wiederherstellung via DB) |
| Fehlkonfiguration der Rollen-Endpunkte | Niedrig | Hoch | `anyRequest().authenticated()` als sicherer Default (Deny-by-Default), Admin-Pfade explizit whitelisted |

## Konsequenzen

### Positiv
- Vollständige Elimination der Klartext-Credential-Problematik des IST-Systems
- Minimaler Implementierungs- und Betriebsaufwand – keine zusätzliche Token-/Session-Infrastruktur
- Rollenwechsel und Deaktivierung von Benutzern wirken sofort (kein Token-Cache)
- Kein CSRF-Schutz nötig, da keine automatisch mitgesendeten Cookies verwendet werden
- Passend zur On-Premises-Entscheidung (ADR-005) – kein unnötiger Schutz gegen Bedrohungen,
  die im geschlossenen LAN nicht relevant sind

### Negativ
- Bei jeder Anfrage wird das Passwort (base64-kodiert, nicht verschlüsselt außerhalb TLS)
  erneut übertragen – unkritisch innerhalb von HTTPS, aber kein Verfahren für zukünftige
  Internet-Exposition ohne Re-Evaluation
- Kein serverseitiger "alle Sessions eines Nutzers beenden"-Mechanismus (nicht gefordert,
  da keine nebenläufigen Multi-Device-Logins vorgesehen sind)
- Sollte das System später doch extern erreichbar gemacht werden (Abweichung von ADR-005),
  muss diese Entscheidung neu bewertet werden (JWT oder Session+CSRF würden dann relevanter)

### Neutral
- Erfordert Pflege der Benutzerliste durch Administratoren über eine eigene
  Benutzerverwaltungs-Oberfläche (Angular `benutzer`-Modul)
- Passwort-Policy (Mindestlänge, Komplexität) ist eine organisatorische Ergänzung, kein
  architektonischer Bestandteil dieses ADRs
