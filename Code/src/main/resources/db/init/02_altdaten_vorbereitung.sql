-- @author Nils
-- Vorbereitung für das Einspielen des Altsystem-Backups (data.sql):
-- Der pg_dump stammt aus einer Konvertierung (rebasedata) und setzt die Rolle
-- "rebasedata" als Tabellen-Eigentümer voraus. Die Rolle wird nach der
-- Datenübernahme (04_datenuebernahme.sql) wieder entfernt.
-- Idempotent, damit das Skript auch manuell nachgefahren werden kann.
--
-- Läuft NICHT über Flyway (kein V-Präfix), sondern nur als initdb-Skript des
-- Postgres-Containers (siehe docker-compose.yml). Liegt hier, damit alle
-- Datenbank-Skripte gemeinsam unter src/main/resources/db/ stehen.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rebasedata') THEN
        CREATE ROLE rebasedata;
    END IF;
END
$$;
