# Authentifiziere Benutzer über Spring Security mit BCrypt-Hashes statt Klartext-Credentials in ini-Dateien

## Das Problem

Aktuell meldet sich ein User mit Datenbank-Zugangsdaten an, was mit der neuen Struktur unverträglich ist und ein Sicherheitsrisiko darstellt.

## Berücksichtigte Optionen

- Beibehaltung der DB-Login-basierten Anmeldung (ein DB-User pro Benutzer)
- JWT-basierte, zustandslose API-Absicherung
- **Spring Security mit eigener Benutzertabelle**

## Begründung

Spring Security entspricht modernen Sicherheitsanforderungen und ist mit einem Webbasierten Frontend verträglich.
## Anmerkungen
