# ADR-012: Getrennte Security-Filterketten für Vaadin-UI und REST-API

## Status

**Akzeptiert und in `Code.zip` implementiert** – August 2026

Ergänzt [ADR-004](004-authentication-rbac.md).

## Kontext

ADR-004 definiert eine session-basierte Formularanmeldung für die Vaadin-Oberfläche. Der
Prototyp stellt zusätzlich REST-Endpunkte unter `/api/**` und Actuator-Endpunkte bereit. Für diese
Endpunkte ist eine zustandslose Authentifizierung sinnvoll, während Vaadin eine serverseitige
HTTP-Session benötigt.

## Entscheidung

Spring Security verwendet zwei geordnete Filterketten:

1. `@Order(1)` für `/api/**` und `/actuator/**`: HTTP Basic, `STATELESS`, CSRF deaktiviert;
   Health-Endpunkte sind freigegeben, `/api/admin/**` erfordert `ROLE_ADMIN`.
2. `@Order(2)` für die Vaadin-UI: `VaadinSecurityConfigurer` mit `LoginView`, serverseitiger
   Session und Vaadins CSRF-Mechanismus.

Beide Wege verwenden denselben `DbUserDetailsService`, BCrypt und dasselbe Rollenmodell.

## Konsequenzen

### Positiv

- Das UI-Modell bleibt mit Vaadin kompatibel.
- REST-Clients benötigen keine serverseitige Session.
- Sicherheitsregeln können je Eintrittspunkt explizit formuliert werden.

### Negativ

- Zwei Sicherheitsmodelle erhöhen Dokumentations- und Testaufwand.
- HTTP Basic überträgt wiederverwendbare Zugangsdaten bei jedem API-Request und setzt TLS voraus.
- Abweichende Regeln zwischen UI und API können zu Autorisierungslücken führen.

## Erforderliche Tests

- anonyme, berechtigte und unberechtigte Zugriffe für beide Filterketten,
- Admin-Endpunkte mit beiden Rollen,
- Session-Logout und Session-Fixation der UI,
- CSRF-Schutz der UI sowie bewusste CSRF-Deaktivierung nur für die zustandslose API,
- konsistente 401-/403-Antworten ohne Informationsleck.
