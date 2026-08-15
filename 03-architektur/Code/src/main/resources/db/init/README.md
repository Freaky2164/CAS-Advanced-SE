# docker-initdb-Skripte

Diese Skripte laufen **nicht** über Flyway. Sie werden ausschließlich vom
`docker-compose`-Stack nach `/docker-entrypoint-initdb.d/` gemountet und dort
**einmalig beim allerersten Start auf leerem DB-Volume** ausgeführt – der
Postgres-Entrypoint arbeitet die Dateien **alphabetisch** ab. Per `pom.xml`
(`<resources>`-`exclude`) sind sie aus dem Artefakt ausgeschlossen; sie liegen
hier nur, damit alle Datenbank-Skripte gemeinsam unter `src/main/resources/db/`
stehen.

## Warum fehlen 01 und 03?

Die Nummer ist reine Reihenfolgesteuerung im Container. Zwei der fünf Slots
werden nicht aus diesem Verzeichnis befüllt:

| Slot im Container | Quelle | Grund |
|---|---|---|
| `01_schema.sql` | `../migration/V1__baseline_schema.sql` | Das Schema ist eine Flyway-Migration und darf nicht doppelt gepflegt werden. |
| `02_altdaten_vorbereitung.sql` | **hier** | Import-Rolle `rebasedata` für den Alt-Dump. |
| `03_altdaten.sql` | `../data.sql` (eine Ebene über `backend/`) | Das Altsystem-Backup liegt außerhalb des Projekts und gehört nicht ins Repository. |
| `04_datenuebernahme.sql` | **hier** | Transformation der Alt-Tabellen ins Zielschema. |
| `05_sicherheit.sh` | **hier** | Legt nur die Login-Rolle an; Rechte/RLS kommen aus Migration `V7`. |

Die Dateinamen tragen absichtlich dieselbe Nummer wie ihr Slot, damit die
Ausführungsreihenfolge auf einen Blick erkennbar ist. Die exakte Zuordnung
steht in `docker-compose.yml`.

## Zusammenhang mit den Flyway-Versionen

Die Flyway-Baseline (`baseline-version: 5`) bildet genau diese Slots ab: `V2`–`V4`
bleiben frei, weil sie den initdb-Schritten 02–04 entsprechen, die es nur auf
Bestands-Datenbanken mit Altdaten gibt. Details: `../migration/README.md`.

## Regeln

1. Skripte müssen **idempotent** sein (`IF EXISTS` / `IF NOT EXISTS`), damit sie
   sich auch manuell nachfahren lassen.
2. **Kein Schema-DDL** hier – das gehört in `../migration`. Diese Skripte laufen
   nur im Compose-Stack und erreichen weder lokale Entwicklung noch CI.
3. **Kein Rechtemodell** hier – Rollenrechte und Row Level Security liegen
   ausschließlich in `../migration/V7__sicherheit_rollen_und_rls.sql`.
4. Neue Slots am Ende anfügen (`06_…`) und den Mount in `docker-compose.yml`
   ergänzen.
