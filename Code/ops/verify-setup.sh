#!/usr/bin/env bash
#
# verify-setup.sh
#
# Prueft nach dem Umbau der Projektstruktur, ob Build, Artefakt und
# Docker-Stack noch stimmen. Linux/macOS-Variante von ops/verify-setup.ps1.
#
# Abgedeckt sind genau die Punkte, die durch das Verschieben von db/ und
# frontend/ sowie durch die neue Migration V7 kaputtgehen koennten:
#
#   1. Build + Tests (inkl. Produktions-Frontend)  -> ./mvnw clean verify -Pproduction
#   2. Inhalt der erzeugten JAR
#        enthaelt      : db/migration, db/testdata, application.yml, META-INF/VAADIN
#        enthaelt NICHT: db/init, frontend-Quellen
#   3. Docker-Stack faehrt hoch, Health-Endpunkt meldet UP
#   4. Datenbank: Flyway-Historie, App-Rolle, RLS-Policies, append-only-Historie
#
# Aufruf (aus dem Projektverzeichnis oder von ueberall):
#   ./ops/verify-setup.sh
#   ./ops/verify-setup.sh --recreate      # setzt den Stack neu auf, LOESCHT das DB-Volume
#   ./ops/verify-setup.sh --skip-build
#   ./ops/verify-setup.sh --skip-docker
#
# Exitcode 0 = alle Pruefungen bestanden.

set -uo pipefail

SKIP_BUILD=false
SKIP_DOCKER=false
RECREATE=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-build)  SKIP_BUILD=true ;;
        --skip-docker) SKIP_DOCKER=true ;;
        --recreate)    RECREATE=true ;;
        -h|--help)
            sed -n '3,28p' "$0" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *)
            echo "Unbekannte Option: $1 (--help fuer die Uebersicht)" >&2
            exit 2
            ;;
    esac
    shift
done

SKRIPT_VERZEICHNIS="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJEKT_WURZEL="$(dirname -- "$SKRIPT_VERZEICHNIS")"
cd "$PROJEKT_WURZEL"

FARBE_ROT=$'\033[31m'
FARBE_GRUEN=$'\033[32m'
FARBE_CYAN=$'\033[36m'
FARBE_AUS=$'\033[0m'

ERGEBNISSE=()
FEHLERZAHL=0

schritt()
{
    printf '\n%s==> %s%s\n' "$FARBE_CYAN" "$1" "$FARBE_AUS"
}

# ergebnis <erfolg:0|1> <beschreibung> [hinweis]
ergebnis()
{
    local erfolg="$1" beschreibung="$2" hinweis="${3:-}"

    if [[ "$erfolg" -eq 0 ]]; then
        printf '    %s[ok]%s   %s\n' "$FARBE_GRUEN" "$FARBE_AUS" "$beschreibung"
        ERGEBNISSE+=("ok   $beschreibung")
    else
        printf '    %s[FEHL]%s %s %s\n' "$FARBE_ROT" "$FARBE_AUS" "$beschreibung" "$hinweis"
        ERGEBNISSE+=("FEHL $beschreibung")
        FEHLERZAHL=$((FEHLERZAHL + 1))
    fi
}

# Listet die Eintraege eines JAR. Bevorzugt "jar" aus dem JDK, sonst unzip.
jar_eintraege()
{
    local jar_pfad="$1"

    if command -v jar >/dev/null 2>&1; then
        jar tf "$jar_pfad"
    elif command -v unzip >/dev/null 2>&1; then
        unzip -Z1 "$jar_pfad"
    else
        echo "Weder 'jar' noch 'unzip' gefunden - JAR-Pruefung nicht moeglich." >&2
        return 1
    fi
}

psql_wert()
{
    docker compose exec -T db psql -U frauenhaus_app -d frauenhaus -tAc "$1" 2>/dev/null | tr -d '[:space:]'
}

# Prueft, ob auf dem Host ein TCP-Port erreichbar ist. Nutzt bashs /dev/tcp;
# "timeout" wird nur verwendet, wenn es vorhanden ist (fehlt z.B. auf macOS).
port_offen()
{
    local ziel="$1" port="$2"

    if command -v timeout >/dev/null 2>&1; then
        timeout 2 bash -c "exec 3<>/dev/tcp/${ziel}/${port}" 2>/dev/null
    else
        (exec 3<>"/dev/tcp/${ziel}/${port}") 2>/dev/null
    fi
}

# Wartet, bis Postgres Verbindungen annimmt. "docker compose up -d" kehrt zurueck,
# sobald der Container laeuft - Postgres selbst ist dann noch mitten in initdb und
# lauscht nicht auf TCP. Ohne dieses Warten startet der Build zu frueh, Flyway
# bekommt keine Verbindung ("Connect timed out") und ALLE @SpringBootTest-Klassen
# scheitern am ApplicationContext. Beim allerersten Start (Altdaten-Uebernahme aus
# ../data.sql) dauert initdb Minuten, daher die grosszuegige Frist.
warte_auf_db()
{
    local frist=$(( $(date +%s) + 300 ))

    printf '    Warte auf die Datenbank (max. 5 Minuten)'
    while [[ $(date +%s) -lt $frist ]]; do
        # Erst der Container (initdb durch?), dann der veroeffentlichte Port
        # (die Tests verbinden sich von aussen ueber das Port-Mapping).
        if docker compose exec -T db pg_isready -U frauenhaus_app -d frauenhaus -q 2>/dev/null \
           && port_offen "$DB_HOST" "$DB_PORT"; then
            printf ' bereit\n'
            return 0
        fi
        printf '.'
        sleep 3
    done

    printf '\n'
    echo "    ${FARBE_ROT}Die Datenbank ist unter ${DB_HOST}:${DB_PORT} nicht erreichbar.${FARBE_AUS}" >&2
    echo '    Pruefen: docker compose ps  /  docker compose logs db' >&2
    return 1
}

# --- 1. Build + Tests -------------------------------------------------------
if [[ "$SKIP_BUILD" == false ]]; then
    schritt 'Build inkl. Tests und Produktions-Frontend (./mvnw clean verify -Pproduction)'
    echo '    Hinweis: die Integrationstests brauchen die DB auf 127.0.0.1:15432.'
    docker compose up -d db >/dev/null

    # localhost bewusst nicht verwendet: es kann auf ::1 aufloesen, der Port ist
    # laut docker-compose.override.yml aber nur auf 127.0.0.1 veroeffentlicht.
    export DB_PORT=15432
    export DB_HOST=127.0.0.1
    export DB_USER=frauenhaus_app
    export DB_PASSWORD="${DB_PASSWORD:-frauenhaus}"

    if ! warte_auf_db; then
        ergebnis 1 'Datenbank erreichbar (Voraussetzung der Integrationstests)'
        printf '\n%sAbbruch: ohne Datenbank scheitern alle Integrationstests.%s\n' \
               "$FARBE_ROT" "$FARBE_AUS" >&2
        exit 1
    fi
    ergebnis 0 'Datenbank erreichbar (Voraussetzung der Integrationstests)'

    ./mvnw clean verify -Pproduction
    build_status=$?
    ergebnis "$build_status" 'Maven-Build inkl. Tests' "(Exitcode $build_status)"

    # --- 2. Artefakt-Inhalt -------------------------------------------------
    schritt 'Inhalt der erzeugten JAR pruefen'
    jar_datei="$(find target -maxdepth 1 -name '*.jar' ! -name '*.original' -print -quit 2>/dev/null)"

    if [[ -z "$jar_datei" ]]; then
        ergebnis 1 'JAR gefunden' '(target/*.jar fehlt)'
    else
        echo "    Artefakt: $jar_datei"
        eintraege="$(jar_eintraege "$jar_datei")"

        for eintrag in \
            'BOOT-INF/classes/db/migration/V1__baseline_schema.sql' \
            'BOOT-INF/classes/db/migration/V7__sicherheit_rollen_und_rls.sql' \
            'BOOT-INF/classes/db/testdata/V5__testdaten.sql' \
            'BOOT-INF/classes/application.yml'
        do
            if grep -qxF "$eintrag" <<<"$eintraege"; then
                ergebnis 0 "JAR enthaelt $eintrag"
            else
                ergebnis 1 "JAR enthaelt $eintrag"
            fi
        done

        if grep -q 'META-INF/VAADIN/' <<<"$eintraege"; then
            ergebnis 0 'JAR enthaelt das gebaute Vaadin-Bundle (META-INF/VAADIN)'
        else
            ergebnis 1 'JAR enthaelt das gebaute Vaadin-Bundle (META-INF/VAADIN)'
        fi

        init_treffer="$(grep 'classes/db/init/' <<<"$eintraege" || true)"
        if [[ -z "$init_treffer" ]]; then
            ergebnis 0 'JAR enthaelt KEINE db/init-Skripte'
        else
            ergebnis 1 'JAR enthaelt KEINE db/init-Skripte' "($(tr '\n' ' ' <<<"$init_treffer"))"
        fi

        frontend_treffer="$(grep 'classes/frontend/' <<<"$eintraege" || true)"
        if [[ -z "$frontend_treffer" ]]; then
            ergebnis 0 'JAR enthaelt KEINE Frontend-Quellen'
        else
            ergebnis 1 'JAR enthaelt KEINE Frontend-Quellen' "($(tr '\n' ' ' <<<"$frontend_treffer"))"
        fi
    fi
fi

# --- 3. Docker-Stack --------------------------------------------------------
if [[ "$SKIP_DOCKER" == false ]]; then
    if [[ "$RECREATE" == true ]]; then
        schritt 'Docker-Stack komplett neu aufsetzen (down -v) - loescht das DB-Volume'
        docker compose down -v >/dev/null
    fi

    schritt 'Docker-Stack starten (docker compose up -d --build)'
    docker compose up -d --build
    ergebnis $? 'docker compose up'

    schritt 'Auf den Health-Endpunkt warten (max. 5 Minuten)'
    status='startet'
    frist=$(( $(date +%s) + 300 ))
    while [[ "$status" != 'UP' && $(date +%s) -lt $frist ]]; do
        sleep 5
        antwort="$(curl -fsS --max-time 5 http://localhost:8080/actuator/health 2>/dev/null || true)"
        if [[ "$antwort" == *'"status":"UP"'* ]]; then
            status='UP'
        else
            status='startet'
        fi
        echo "    Status: $status"
    done

    [[ "$status" == 'UP' ]] && ergebnis 0 'Anwendung meldet Health UP' \
                            || ergebnis 1 'Anwendung meldet Health UP' "(letzter Status: $status)"

    # --- 4. Datenbank-Zustand ----------------------------------------------
    schritt 'Datenbank pruefen (Flyway-Historie, Rolle, RLS, append-only)'

    migrationen="$(psql_wert "SELECT string_agg(version, ',' ORDER BY installed_rank) FROM frauenhaus.flyway_schema_history")"
    echo "    angewendete Versionen: $migrationen"
    if [[ ",$migrationen," == *',7,'* ]]; then
        ergebnis 0 'Flyway-Historie enthaelt V7'
    else
        ergebnis 1 'Flyway-Historie enthaelt V7' "(gefunden: $migrationen)"
    fi

    rolle="$(psql_wert "SELECT count(*) FROM pg_roles WHERE rolname = 'frauenhaus_backend'")"
    [[ "$rolle" == '1' ]] && ergebnis 0 'App-Rolle frauenhaus_backend existiert' \
                          || ergebnis 1 'App-Rolle frauenhaus_backend existiert' "(count = $rolle)"

    policies="$(psql_wert "SELECT count(*) FROM pg_policies WHERE policyname = 'benutzerkontext_erforderlich'")"
    echo "    RLS-Policies: $policies (erwartet: 11)"
    [[ "$policies" == '11' ]] && ergebnis 0 'RLS-Policies auf allen personenbezogenen Tabellen' \
                              || ergebnis 1 'RLS-Policies auf allen personenbezogenen Tabellen' "(gefunden: $policies)"

    audit_update="$(psql_wert "SELECT has_table_privilege('frauenhaus_backend', 'frauenhaus.mitglied_aud', 'UPDATE')")"
    [[ "$audit_update" == 'f' ]] && ergebnis 0 'Audit-Historie ist append-only (kein UPDATE)' \
                                 || ergebnis 1 'Audit-Historie ist append-only (kein UPDATE)' "(has_table_privilege = $audit_update)"

    flyway_lesbar="$(psql_wert "SELECT has_table_privilege('frauenhaus_backend', 'frauenhaus.flyway_schema_history', 'SELECT')")"
    [[ "$flyway_lesbar" == 'f' ]] && ergebnis 0 'Flyway-Historie fuer die App-Rolle gesperrt' \
                                  || ergebnis 1 'Flyway-Historie fuer die App-Rolle gesperrt' "(has_table_privilege = $flyway_lesbar)"

    if [[ "$RECREATE" == true ]]; then
        mitglieder="$(psql_wert 'SELECT count(*) FROM frauenhaus.mitglied')"
        echo "    uebernommene Mitglieder: $mitglieder"
        if [[ "$mitglieder" =~ ^[0-9]+$ && "$mitglieder" -gt 0 ]]; then
            ergebnis 0 'Altdaten-Uebernahme hat Mitglieder geschrieben'
        else
            ergebnis 1 'Altdaten-Uebernahme hat Mitglieder geschrieben' "(count = $mitglieder)"
        fi
    fi
fi

# --- Zusammenfassung --------------------------------------------------------
printf '\n%s=== Zusammenfassung ===%s\n' "$FARBE_CYAN" "$FARBE_AUS"
for zeile in "${ERGEBNISSE[@]}"; do
    printf '  [%s] %s\n' "${zeile:0:4}" "${zeile:5}"
done

if [[ "$FEHLERZAHL" -gt 0 ]]; then
    printf '\n%s%d Pruefung(en) fehlgeschlagen.%s\n' "$FARBE_ROT" "$FEHLERZAHL" "$FARBE_AUS"
    exit 1
fi

printf '\n%sAlle Pruefungen bestanden.%s\n' "$FARBE_GRUEN" "$FARBE_AUS"
