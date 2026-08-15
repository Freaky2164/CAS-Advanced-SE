#!/bin/bash
# Deployment-Schritt beim allerersten Start der Datenbank (docker-entrypoint-initdb.d):
# legt die eingeschraenkte Login-Rolle der Anwendung an.
#
# Laeuft NICHT ueber Flyway, sondern nur als initdb-Skript des Postgres-Containers
# (siehe docker-compose.yml). Liegt hier, damit alle Datenbank-Skripte gemeinsam
# unter src/main/resources/db/ stehen (migration/, testdata/, init/).
#
# ABGRENZUNG (bewusst, Ergebnis des Gruppen-Reviews):
#   * HIER: nur die Rolle samt Passwort - ein Deployment-/Secret-Belang, der aus
#     der Umgebung kommen muss (DB_APP_PASSWORD) und nicht ins Repository gehoert.
#   * NICHT hier: Rechtevergabe, append-only Audit-Historie und Row Level
#     Security. Dieses Rechtemodell ist ein Schema-Belang und liegt seit V7
#     ausschliesslich in der Flyway-Migration
#     src/main/resources/db/migration/V7__sicherheit_rollen_und_rls.sql.
#     Frueher stand es zusaetzlich hier - Datenbanken ohne diesen initdb-Pfad
#     (lokale Entwicklung, CI, Tests) blieben dadurch ganz ohne RLS, und die
#     doppelte Pflege beider Stellen war fehleranfaellig.
#
# V7 wuerde die Rolle notfalls selbst anlegen (Passwort als Flyway-Placeholder);
# dieses Skript stellt sicher, dass sie im Compose-Stack schon existiert, bevor
# die Anwendung sich zum ersten Mal verbindet.
#
# Hinweis: Das Passwort wird als SQL-Literal eingesetzt - es darf keine
# einfachen Anfuehrungszeichen enthalten.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<EOSQL
DO \$\$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'frauenhaus_backend') THEN
        RAISE NOTICE 'Rolle frauenhaus_backend existiert bereits - unveraendert.';
    ELSE
        EXECUTE format(
            'CREATE ROLE frauenhaus_backend LOGIN PASSWORD %L '
            'NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS',
            '${DB_APP_PASSWORD:-frauenhaus}');
    END IF;
END
\$\$;
EOSQL
