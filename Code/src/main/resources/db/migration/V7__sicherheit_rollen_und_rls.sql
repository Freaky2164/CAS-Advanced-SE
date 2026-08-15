-- Sicherheitsmodell der Datenbank: eingeschraenkte App-Rolle, Rechtevergabe,
-- append-only Audit-Historie und Row Level Security.
--
-- WARUM ALS MIGRATION (Gruppen-Review-Nachzug):
-- Bis V6 lebte dieses Modell ausschliesslich im docker-initdb-Skript
-- db/init/05_sicherheit.sh. Datenbanken, die NICHT ueber den Compose-Stack mit
-- Altdaten-Import entstehen (lokale Entwicklung, CI, Tests, kuenftige
-- Neuinstallation ohne Altbestand), bekamen dadurch weder die App-Rolle noch
-- RLS. Ab hier ist diese Migration die EINZIGE Quelle der Wahrheit; das
-- initdb-Skript legt nur noch die Login-Rolle mit ihrem Passwort an.
--
-- Die Migration ist bewusst vollstaendig IDEMPOTENT: sie laeuft sowohl auf einer
-- frisch per Flyway aufgebauten Datenbank als auch auf einer Bestands-Datenbank
-- (Baseline 5), auf der 05_sicherheit.sh dieselben Rechte bereits gesetzt hat.
--
-- GRENZE (Gruppen-Review): Die RLS-Session-Variablen kann jede Verbindung selbst
-- per set_config setzen - auch eine mit geleakten Backend-Zugangsdaten. RLS
-- schuetzt hier vor kontextlosem Zugriff (versehentliches psql, naive Clients),
-- ist aber keine harte Barriere. Deshalb zusaetzlich die append-only Historie.

-- 1. Login-Rolle -----------------------------------------------------------
-- Passwort kommt als Flyway-Placeholder aus der Umgebung (DB_APP_PASSWORD bzw.
-- DB_PASSWORD, siehe application.yml). Ist es leer, wird die Rolle hier nicht
-- angelegt - dann stammt sie aus dem initdb-Skript oder der Betrieb legt sie
-- selbst an. Ein Passwort steht NIE im Quellcode.
DO $$
DECLARE
    app_pw text := '${app_db_password}';
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'frauenhaus_backend') THEN
        RAISE NOTICE 'Rolle frauenhaus_backend existiert bereits - Passwort bleibt unveraendert.';
    ELSIF app_pw = '' THEN
        RAISE NOTICE 'Rolle frauenhaus_backend fehlt und DB_APP_PASSWORD ist leer - '
                     'Rolle, Rechte und RLS werden uebersprungen.';
    ELSE
        EXECUTE format(
            'CREATE ROLE frauenhaus_backend LOGIN PASSWORD %L '
            'NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS', app_pw);
    END IF;
END
$$;

-- 2. Rechte und Row Level Security ----------------------------------------
DO $$
DECLARE
    tabelle text;
    -- Tabellen mit personenbezogenen Daten (DSGVO Art. 5/9). Bewusst OHNE RLS:
    --  * app.app_user  - wird bei der Anmeldung gelesen, BEVOR ein Benutzer-
    --                    kontext existiert (Henne-Ei), enthaelt nur Name + Hash
    --  * reine Lookups ohne Personenbezug (anrede, verein, gericht, spendenart,
    --    spendentyp, stichwort, bussgeldstatus, verein_aud, gericht_aud)
    rls_tabellen text[] := ARRAY[
        'frauenhaus.mitglied',
        'frauenhaus.spende',
        'frauenhaus.bussgeld',
        'frauenhaus.eingang',
        'frauenhaus.stichwort_person',
        'frauenhaus.verein_mitglied',
        'frauenhaus.dokument',
        'frauenhaus.mitglied_aud',
        'frauenhaus.spende_aud',
        'frauenhaus.bussgeld_aud',
        'app.revinfo'
    ];
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'frauenhaus_backend') THEN
        RETURN;
    END IF;

    -- Least Privilege: Datenzugriff ja, DDL nein (kein Eigentum, kein CREATE).
    EXECUTE 'GRANT USAGE ON SCHEMA frauenhaus, app TO frauenhaus_backend';
    EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA frauenhaus, app '
            'TO frauenhaus_backend';
    EXECUTE 'GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA frauenhaus, app TO frauenhaus_backend';

    -- Die Flyway-Historie gehoert dem Migrationsbenutzer, nicht der Anwendung.
    IF to_regclass('frauenhaus.flyway_schema_history') IS NOT NULL THEN
        EXECUTE 'REVOKE ALL ON TABLE frauenhaus.flyway_schema_history FROM frauenhaus_backend';
    END IF;

    -- Append-only Audit-Historie: Envers schreibt nur INSERTs und liest fuer den
    -- Verlauf; UPDATE/DELETE braucht die Anwendung dort nie. Damit kann selbst
    -- ein Angreifer mit gueltigen Zugangsdaten die Historie nicht bereinigen.
    EXECUTE 'REVOKE UPDATE, DELETE ON '
            'frauenhaus.mitglied_aud, frauenhaus.spende_aud, frauenhaus.bussgeld_aud, '
            'frauenhaus.verein_aud, frauenhaus.gericht_aud, app.revinfo '
            'FROM frauenhaus_backend';

    -- RLS: Freigabe nur mit gesetztem, gueltigem Benutzerkontext.
    -- current_setting(..., true) liefert NULL statt Fehler, wenn nichts gesetzt ist.
    FOREACH tabelle IN ARRAY rls_tabellen
    LOOP
        EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY', tabelle);
        EXECUTE format('DROP POLICY IF EXISTS benutzerkontext_erforderlich ON %s', tabelle);
        EXECUTE format(
            'CREATE POLICY benutzerkontext_erforderlich ON %s
                 FOR ALL TO frauenhaus_backend
                 USING (current_setting(''app.benutzer_rolle'', true) IN (''ADMIN'', ''SACHBEARBEITUNG''))
                 WITH CHECK (current_setting(''app.benutzer_rolle'', true) IN (''ADMIN'', ''SACHBEARBEITUNG''))',
            tabelle);
    END LOOP;
END
$$;
