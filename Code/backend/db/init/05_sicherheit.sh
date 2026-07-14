#!/bin/bash
# @author Nils
#
# DSGVO-Schutz der personenbezogenen Daten auf Datenbankebene:
#
# 1. Eigene, eingeschränkte Login-Rolle "frauenhaus_backend" für die Anwendung
#    (Least Privilege: kein DDL, kein SUPERUSER, kein BYPASSRLS). Der Superuser
#    "frauenhaus_app" bleibt Schema-Eigentümer und dient nur noch Init/Wartung.
#
# 2. Row Level Security auf allen Tabellen mit personenbezogenen Daten
#    (DSGVO Art. 5/9 – Adressen, Spenden, Bußgelder, Anhänge, Änderungshistorie):
#    Zeilen sind nur sichtbar/änderbar, wenn die Anwendung pro Verbindung den
#    authentifizierten Benutzerkontext gesetzt hat
#    (Session-Variablen app.benutzer / app.benutzer_rolle, siehe
#    RowLevelSecurityDataSource im Backend). Eine Verbindung mit den
#    Backend-Zugangsdaten OHNE diesen Kontext – etwa ein direkt angebundenes
#    psql oder ein kompromittierter Client – sieht keine einzige Zeile.
#
# Bewusst ohne RLS:
#  - app.app_user: wird bei der Anmeldung gelesen, BEVOR ein Benutzerkontext
#    existiert (Henne-Ei); enthält nur Benutzername + BCrypt-Hash.
#  - Reine Lookup-Tabellen ohne Personenbezug (anrede, verein, gericht,
#    spendenart, spendentyp, stichwort, bussgeldstatus, verein_aud, gericht_aud).
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<EOSQL
-- Eingeschränkte Rolle für die Anwendung (Passwort aus DB_APP_PASSWORD)
CREATE ROLE frauenhaus_backend LOGIN PASSWORD '${DB_APP_PASSWORD:-frauenhaus}'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS;

GRANT USAGE ON SCHEMA frauenhaus, app TO frauenhaus_backend;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA frauenhaus, app TO frauenhaus_backend;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA frauenhaus, app TO frauenhaus_backend;

-- Row Level Security: Freigabe nur mit gesetztem, gültigem Benutzerkontext.
-- current_setting(..., true) liefert NULL statt Fehler, wenn nichts gesetzt ist.
DO \$\$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY[
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
    ]
    LOOP
        EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format(
            'CREATE POLICY benutzerkontext_erforderlich ON %s
                 FOR ALL TO frauenhaus_backend
                 USING (current_setting(''app.benutzer_rolle'', true) IN (''ADMIN'', ''SACHBEARBEITUNG''))
                 WITH CHECK (current_setting(''app.benutzer_rolle'', true) IN (''ADMIN'', ''SACHBEARBEITUNG''))',
            t);
    END LOOP;
END
\$\$;
EOSQL

echo "05_sicherheit: Rolle frauenhaus_backend angelegt, Row Level Security aktiv."
