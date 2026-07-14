# Docker Compose Deployment mit nginx

## Das Problem

Das aktuelle Deployment setzt die Installation der Desktop-App auf jedem Nutzer-PC voraus und bietet keine Sicherheit.

## Berücksichtigte Optionen

- Zugriff auf ein System mit einer Desktop-App (Installation pro Arbeitsplatz)
- Backend als nativer Windows-Dienst und separat betriebene Datenbank
- **Docker-Compose-Stack mit nginx, postgresql und spring backend, Zugriff über Webadresse**

## Begründung

Docker Compose macht das Deployment betriebssystemagnostisch. Backend und Datenbank sind von außen nicht erreichbar. TLS terminiert im nginx: echtes Zertifikat per Volume-Mount, andernfalls kann beim ersten Start ein selbstsigniertes Zertifikat erzeugt werden, sodass Zugangsdaten nie im Klartext übers Netz laufen.

## Anmerkungen
