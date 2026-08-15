# Schema-Migrationen (Flyway)

Verbindliche Regeln für dieses Verzeichnis. Konfiguration: `src/main/resources/application.yml`
(`spring.flyway.*`), Erläuterung für Anwender im Projekt-`README.md`.

Alle Datenbank-Skripte liegen unter `src/main/resources/db/`:

| Verzeichnis | Ausführung |
|---|---|
| `migration/` | Flyway, versioniert, vom Classpath beim App-Start |
| `testdata/` | Flyway, nur Profil `dev` (eigene `locations`) |
| `init/` | **kein** Flyway – nur als docker-initdb-Skripte gemountet; per `pom.xml` aus dem Artefakt ausgeschlossen |

## Belegte Versionen

| Version | Datei / Herkunft | Läuft auf |
|---|---|---|
| `V1` | `db/migration/V1__baseline_schema.sql` – Zielschema (`frauenhaus.*`, `app.app_user`, Envers-Audit) | leere DB via Flyway; im Compose-Stack zusätzlich als initdb-Skript `01_schema.sql` |
| `V2`–`V4` | **bewusst frei** – reserviert für die initdb-Schritte `db/init/02_altdaten_vorbereitung.sql`, `03` (`../data.sql`) und `db/init/04_datenuebernahme.sql`, die es nur auf Bestands-Datenbanken gibt | – |
| `V5` | `db/testdata/V5__testdaten.sql` – Testdaten, nur im Profil `dev` (eigene `locations`) | nur leere DB im Profil `dev` |
| `V6` | `V6__audit_historie_append_only.sql` – Entzug von UPDATE/DELETE auf der Audit-Historie | alle |
| `V7` | `V7__sicherheit_rollen_und_rls.sql` – App-Rolle, Rechtevergabe, Row Level Security (idempotent, einzige Quelle der Wahrheit) | alle |

`baseline-on-migrate: true` mit `baseline-version: 5` bedeutet: eine Datenbank, die bereits
Objekte enthält, aber keine Flyway-Historie hat (= aus den docker-initdb-Skripten entstanden),
gilt bis einschließlich `V5` als migriert. Dort laufen nur `V6` aufwärts. Deshalb dürfen
Testdaten niemals eine Version > 5 bekommen – sie würden sonst in Bestände mit echten
Altdaten geschrieben.

Die Lücke `V2`–`V4` ist damit die direkte Entsprechung der initdb-Slots 02–04: Schritte, die
es nur auf Bestands-Datenbanken gibt (Import-Rolle, Altdaten-Dump, Datenübernahme) und die
deshalb nie als Migration existieren können. Dass in `db/init/` umgekehrt die Nummern 01 und
03 fehlen, hat denselben Hintergrund – siehe [`../init/README.md`](../init/README.md).

## Regeln

1. **`V1` ist eingefroren.** Es wird zusätzlich als initdb-Skript gemountet; jede Änderung
   erzeugt Checksum-Fehler bzw. stille Schema-Divergenz. Änderungen nur als neue Version.
2. **Neue Migrationen ab `V8`** aufwärts, fortlaufend, mit sprechendem Namen
   (`V8__<was_geaendert_wird>.sql`).
3. **Idempotent schreiben, wo möglich** (`IF EXISTS` / `IF NOT EXISTS` / `DROP … IF EXISTS`
   vor `CREATE`). Migrationen müssen sowohl auf einer frisch aufgebauten als auch auf einer
   baselineten Bestands-Datenbank sinnvoll laufen.
4. **Keine Secrets im Skript.** Passwörter kommen als Flyway-Placeholder aus der Umgebung
   (siehe `app_db_password` in `application.yml` und `V7`).
5. **Rechte/RLS gehören hierher**, nicht in `db/init/*`. Die initdb-Skripte laufen nur im
   Compose-Stack mit Altdaten-Import und erreichen weder lokale Entwicklung noch CI.6. Migrationen laufen mit den privilegierten Zugangsdaten (`FLYWAY_DB_USER`/`FLYWAY_DB_PASSWORD`),
   die Anwendung selbst verbindet als eingeschränkte Rolle `frauenhaus_backend`.
