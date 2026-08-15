# ADR-004: Authentifizierung & Autorisierung – Session-basierte Formular-Authentifizierung mit BCrypt und RBAC (Vaadin / Spring Security)

## Status

**Akzeptiert** – Juni 2026

Die session-basierte Vaadin-Anmeldung ist implementiert. Eine zusätzliche zustandslose
HTTP-Basic-Filterkette für die REST-API wird in
[ADR-012](012-dual-security-filter-chains.md) dokumentiert.

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

Gemäß ADR-006 wird das System ausschließlich **On-Premises im lokalen Netzwerk** betrieben,
ist **nicht über das Internet erreichbar** und hat nur wenige, bekannte Nutzer (Mitarbeitende
des Vereins). Dieser eingeschränkte Bedrohungskontext beeinflusst maßgeblich, welches
Authentifizierungsverfahren angemessen ist – ein für öffentlich erreichbare Multi-Tenant-Systeme
optimiertes Verfahren (z.B. JWT mit Refresh-Tokens) wäre hier unverhältnismäßig komplex.

**Technische Randbedingung aus ADR-003 (Vaadin):** Das Frontend wird mit **Vaadin Flow**
umgesetzt. Vaadin Flow rendert die UI serverseitig und hält den UI-Zustand pro Benutzer in einer
**serverseitigen `VaadinSession`** (getragen von der HTTP-Session, `JSESSIONID`-Cookie). Die
Anwendung ist damit auf UI-Ebene **inhärent zustandsbehaftet** – ein rein zustandsloses
Authentifizierungsverfahren (z.B. HTTP-Basic mit `SessionCreationPolicy.STATELESS`, das pro
Request neu authentifiziert) passt nicht zum Programmiermodell von Vaadin und würde dessen
Session-Handling unterlaufen. Das gewählte Verfahren muss sich daher sauber in die
Vaadin-/Spring-Security-Integration (`VaadinWebSecurity`) einfügen.

## Entscheidung

Wir entscheiden uns für **session-basierte Formular-Authentifizierung** über die
**Vaadin-/Spring-Security-Integration (`VaadinWebSecurity`)** mit einer eigenen
**Vaadin-Login-View**, in Kombination mit **BCrypt-gehashten Passwörtern** und einem
**einfachen rollenbasierten Zugriffsmodell (RBAC)** mit genau zwei Rollen: `ADMIN` und
`SACHBEARBEITUNG`. Der Authentifizierungsstatus wird nach dem Login serverseitig in der
HTTP-/`VaadinSession` gehalten (`JSESSIONID`, `HttpOnly` + `Secure` über HTTPS).

```
Vaadin Login-View ──(Benutzername/Passwort, POST /login)──> Spring Security Filter Chain
                                                                   │
                                          UsernamePasswordAuthenticationFilter
                                                                   │
                                             DbUserDetailsService ── liest app.app_user
                                                                   │
                                          BCryptPasswordEncoder.matches(...)
                                                                   │
                        Erfolg: SecurityContext in serverseitiger Session (JSESSIONID, HttpOnly/Secure)
                                                                   │
                        Folge-Requests: Vaadin/Spring Security prüfen Session + Rolle
                                                                   │
                             hasRole("ADMIN") für Admin-/Stammdaten-Views
                             authenticated() für alle übrigen Views
```

Zentrale Bausteine:

- **`SecurityConfig extends VaadinWebSecurity`**: Konfiguriert die Spring Security 7 Filter Chain
  über die offizielle Vaadin-Integration. `VaadinWebSecurity` verdrahtet Vaadins internen
  CSRF-Schutz, die Behandlung von Vaadin-Requests und die Weiterleitung nicht angemeldeter
  Nutzer auf die Login-View. Die Session-Policy ist bewusst **nicht** `STATELESS`, da Vaadin Flow
  eine serverseitige Session benötigt.
- **`LoginView`**: Eigene Vaadin-View mit Vaadins `LoginForm`-Komponente; ersetzt den
  nativen Browser-Basic-Auth-Dialog und liefert eine konsistente, benutzerfreundliche
  Login-Oberfläche (NFR-5 Bedienbarkeit).
- **`AppUser`**: JPA-Entity (`app.app_user`) mit `username`, `passwordHash` (BCrypt),
  `role` (Enum `ADMIN` / `SACHBEARBEITUNG`), `enabled`-Flag, ersetzt `compucrash.user_def`.
- **`DbUserDetailsService`**: Lädt Benutzer aus der Datenbank statt in-memory/statisch zu
  konfigurieren – neue Benutzer und Deaktivierungen sind ohne Neustart wirksam.
- **`AdminBootstrap`**: Legt beim allerersten Start automatisch einen Admin-Account an, sofern
  die Benutzertabelle leer ist. Passwort kommt aus `APP_ADMIN_PASSWORD` (Umgebungsvariable) oder
  wird als Einmal-Zufallswert generiert und **einmalig geloggt** – es gibt kein eingebranntes
  Standardpasswort (vermeidet CWE-798 *Use of Hard-coded Credentials*).
- **`@EnableMethodSecurity`**: Ermöglicht zusätzlich feingranulare Autorisierung auf Service-/
  View-Methodenebene (`@PreAuthorize`), nicht nur auf Routen-/URL-Pattern-Ebene.
- **Logout**: Über Spring Securitys `/logout` bzw. `logout()`-Handler wird die serverseitige
  Session invalidiert – ein echter, serverseitig erzwingbarer Logout ist möglich.

## Betrachtete Alternativen

### Alternative A: Zustandslose HTTP-Basic-Auth + BCrypt

Jeder Request trägt den `Authorization: Basic <user:pass base64>`-Header; das Backend
authentifiziert bei jeder Anfrage neu (`SessionCreationPolicy.STATELESS`), ohne serverseitige
Session.

| Aspekt | Bewertung |
|--------|-----------|
| Implementierungsaufwand (reine REST-API) | ✅ Minimal – native Spring-Security-Unterstützung |
| **Kompatibilität mit Vaadin Flow** | ❌ **Grundlegender Konflikt**: Vaadin Flow benötigt eine serverseitige `VaadinSession`; ein `STATELESS`-Modell unterläuft das Session-Handling und wird von `VaadinWebSecurity` nicht unterstützt |
| Passwort-Sicherheit | ✅ BCrypt (adaptiv, gesalzen) statt Klartext |
| Login-UX | ❌ Nativer Browser-Basic-Auth-Dialog statt konsistenter Vaadin-Login-View (Widerspruch zu NFR-5) |
| Serverseitiger Logout | ❌ Kein echter Logout – Credentials werden bei jedem Request erneut gesendet |
| Wiederholte Credential-Übertragung | ⚠️ Passwort (base64) bei **jedem** Request erneut über die Leitung (nur durch TLS geschützt) |

**Ablehnung**: Passt nicht zum serverseitig zustandsbehafteten Programmiermodell von Vaadin Flow
(ADR-003). Der scheinbare Vorteil „Zustandslosigkeit“ ist in einer Vaadin-Anwendung nicht
realisierbar, da die UI ohnehin eine Session hält. Zudem widerspricht der native Basic-Auth-Dialog
der Usability-Anforderung (NFR-5).

### Alternative B: JWT (Bearer-Token, zustandslos mit Refresh-Token)

Backend stellt beim Login ein signiertes JWT aus, das die Rolle als Claim enthält;
Refresh-Tokens ermöglichen erneuerbare Sessions ohne erneute Passworteingabe.

| Aspekt | Bewertung |
|--------|-----------|
| Skalierbarkeit über mehrere Server | ✅ Kein zentraler Session-Store nötig |
| Kompatibilität mit Vaadin Flow | ❌ Vaadin hält UI-Zustand serverseitig – der Hauptvorteil von JWT (Zustandslosigkeit) verpufft |
| Implementierungsaufwand | ❌ Signing-Key-Verwaltung, Token-Refresh-Flow, Blacklisting bei Logout |
| Widerruf vor Ablauf | ❌ Erfordert zusätzliche Token-Blacklist oder kurze Lebensdauer + Refresh-Komplexität |
| Eignung für Kleinstsystem (1 Server, wenige Nutzer) | ❌ Löst ein Problem (horizontale Skalierung), das hier nicht existiert |

**Ablehnung**: JWT löst primär Probleme verteilter/skalierender Systeme (mehrere Backend-Instanzen,
Third-Party-APIs, mobile Clients mit langlebigen Sessions). Für einen einzelnen Windows-Dienst mit
wenigen bekannten Nutzern im LAN und einem serverseitig zustandsbehafteten Vaadin-Frontend steht
der Mehraufwand (Signing-Key-Rotation, Refresh-Logik, Token-Widerruf) in keinem Verhältnis zum
Nutzen.

### Alternative C: Session-basierte Formular-Authentifizierung (Vaadin/Spring Security) (gewählt) ✅

Login über eine Vaadin-Login-View; nach erfolgreicher Prüfung hält Spring Security den
`SecurityContext` in der serverseitigen HTTP-/`VaadinSession` (`JSESSIONID`, `HttpOnly` + `Secure`).

| Aspekt | Bewertung |
|--------|-----------|
| **Kompatibilität mit Vaadin Flow** | ✅ Nativer, offiziell unterstützter Weg über `VaadinWebSecurity` – passt exakt zum serverseitigen Session-Modell |
| Implementierungsaufwand | ✅ Gering – `VaadinWebSecurity` liefert Login-Weiterleitung, CSRF-Verdrahtung und Request-Handling out of the box |
| Serverseitiger Logout / Widerruf | ✅ Session jederzeit sofort invalidierbar; Deaktivierung eines Nutzers wirkt beim nächsten Request |
| CSRF-Schutz | ✅ Durch Vaadins eingebauten CSRF-Schutz und Spring Security abgedeckt (über `VaadinWebSecurity` konfiguriert) |
| Passwort-Sicherheit | ✅ BCrypt (adaptiv, gesalzen) statt Klartext wie im IST-System |
| Login-UX | ✅ Konsistente Vaadin-Login-View statt nativem Browser-Dialog (NFR-5) |
| Transport-Sicherheit | ✅ Ausschließlich über HTTPS im geschlossenen LAN (ADR-006), `Secure`/`HttpOnly`-Cookie |
| Statefulness | ⚠️ Serverseitiger Session-Zustand nötig – bei 1 Dienst unkritisch; ein Dienst-Neustart erfordert erneuten Login |

## Begründung

### 1. Verhältnismäßigkeit zum Bedrohungskontext

Das System läuft ausschließlich On-Premises, ist nicht über das Internet erreichbar (ADR-006)
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

### 6. Session-Modell passt zum Vaadin-Programmiermodell

Vaadin Flow hält den UI-Zustand ohnehin serverseitig in der `VaadinSession`. Die
Authentifizierung an denselben Session-Mechanismus zu binden, ist der **native, offiziell
unterstützte Weg** (`VaadinWebSecurity`) und vermeidet einen Bruch im Programmiermodell. Ein
Neustart des Windows-Dienstes (z.B. nach einem Update, siehe ADR-002) beendet die Sessions –
angemeldete Nutzer müssen sich danach neu anmelden. Das ist für einen kurzen, planbaren
Wartungsvorgang im Bürobetrieb akzeptabel und wird durch die geringe Startzeit des Dienstes
abgefedert.

## Risiken und Mitigationen

| Risiko | Wahrscheinlichkeit | Auswirkung | Mitigation |
|--------|--------------------|-----------|-------------|
| Credential-Sniffing im LAN | Niedrig | Hoch | Ausschließlich HTTPS, auch innerhalb des LAN (Transportverschlüsselung) |
| Brute-Force auf Login-Endpunkt | Niedrig (kein Internet-Zugriff) | Mittel | BCrypt-Kostenfaktor, optional Rate-Limiting auf `/login`-Pfad |
| Session-Hijacking / -Fixation | Niedrig | Hoch | `Secure` + `HttpOnly`-Cookie über HTTPS, Session-ID-Erneuerung nach Login (Spring Security Default) |
| CSRF | Niedrig | Mittel | Vaadins eingebauter CSRF-Schutz + Spring Security (über `VaadinWebSecurity` verdrahtet) |
| Kein automatischer Logout bei Inaktivität | Mittel | Niedrig | Konfigurierbares Session-Timeout (`server.servlet.session.timeout`) + organisatorische Bildschirmsperre |
| Verlust des Admin-Passworts | Niedrig | Mittel | `AdminBootstrap` kann bei leerer Benutzertabelle erneut ausgeführt werden (Notfall-Wiederherstellung via DB) |
| Fehlkonfiguration der Rollen-Endpunkte | Niedrig | Hoch | `anyRequest().authenticated()` als sicherer Default (Deny-by-Default), Admin-Views explizit whitelisted |

## Konsequenzen

### Positiv
- Vollständige Elimination der Klartext-Credential-Problematik des IST-Systems
- Geringer Implementierungsaufwand durch die native Vaadin-/Spring-Security-Integration
  (`VaadinWebSecurity`) – Login-Weiterleitung und CSRF-Verdrahtung out of the box
- Echter serverseitiger Logout und sofort wirksame Deaktivierung von Benutzern
- Konsistente, benutzerfreundliche Login-View (NFR-5) statt nativem Browser-Dialog
- CSRF-Schutz durch Vaadin/Spring Security abgedeckt (`HttpOnly`/`Secure`-Cookie)
- Passend zur On-Premises-Entscheidung (ADR-006) – kein unnötiger Schutz gegen Bedrohungen,
  die im geschlossenen LAN nicht relevant sind

### Negativ
- Serverseitiger Session-Zustand: Ein Neustart des Windows-Dienstes beendet aktive Sessions,
  Nutzer müssen sich erneut anmelden (akzeptabel bei planbaren Wartungsfenstern)
- Kein Out-of-the-box-Mechanismus „alle Sessions eines Nutzers zentral beenden" über mehrere
  Instanzen – im Einzelserver-Betrieb ohne Bedeutung
- Sollte das System später extern erreichbar gemacht werden (Abweichung von ADR-006), sind
  zusätzliche Härtungen (z.B. Rate-Limiting, MFA, ggf. Token-Verfahren) neu zu bewerten

### Neutral
- Erfordert Pflege der Benutzerliste durch Administratoren über eine eigene
  Benutzerverwaltungs-View (Vaadin `benutzer`-Modul)
- Passwort-Policy (Mindestlänge, Komplexität) ist eine organisatorische Ergänzung, kein
  architektonischer Bestandteil dieses ADRs
