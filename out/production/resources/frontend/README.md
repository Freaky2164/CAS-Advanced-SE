# Frauenhaus-Verwaltung – Frontend

Angular 19 (Standalone Components). Einfache Oberfläche für die Funktionen des
Backends: Login (HTTP Basic), Report-Downloads (Bußgelder, Spenden, Verteiler)
und die Pflege der Verteiler-Stichworte.

## Start (lokal)

Voraussetzung: das Backend läuft auf Port 8080 (siehe ../backend/README.md).

    npm install
    npm start

Dann http://localhost:4200 öffnen und mit den Backend-Zugangsdaten anmelden
(z. B. `admin` / Wert von `APP_ADMIN_PASSWORD`). Der Dev-Server leitet `/api`
und `/actuator` per Proxy (proxy.conf.json) an das Backend weiter – dadurch
ist kein CORS nötig.

## Aufbau

    src/app/
      auth.service.ts        Anmeldung, hält den Basic-Auth-Header (sessionStorage)
      auth.interceptor.ts    hängt den Header an jede Anfrage, leitet bei 401 zum Login
      auth.guard.ts          schützt alle Routen außer /login
      api.service.ts         Report-Downloads (Blob) und Stichwort-Aktionen
      login/                 Login-Maske
      reports/               alle Report-Downloads und der E-Mail-Verteiler
      stichworte/            Stichworte zusammenstellen/zusammenfassen

## Build und Tests

    npm run build   # Produktions-Build nach dist/
    npm test        # Unit-Tests (Karma)
