<#
.SYNOPSIS
    Prueft nach dem Umbau der Projektstruktur, ob Build, Artefakt und
    Docker-Stack noch stimmen.

.DESCRIPTION
    Deckt genau die Punkte ab, die durch das Verschieben von db/, frontend/
    und durch die neue Migration V7 kaputtgehen koennten:

      1. Build + Tests (inkl. Produktions-Frontend)     -> mvnw clean verify -Pproduction
      2. Inhalt der erzeugten JAR
           enthaelt : db/migration, db/testdata, application.yml, META-INF/VAADIN
           enthaelt NICHT: db/init, frontend-Quellen
      3. Docker-Stack faehrt hoch, Health-Endpunkt meldet UP
      4. Datenbank: Flyway-Historie, App-Rolle, RLS-Policies, append-only-Historie

    Ohne Parameter laeuft alles nacheinander. Am Ende steht eine Zusammenfassung;
    der Exitcode ist 0, wenn alle Pruefungen bestanden sind.

.PARAMETER SkipBuild
    Ueberspringt Schritt 1 und 2 (z.B. wenn gerade schon gebaut wurde).

.PARAMETER SkipDocker
    Ueberspringt Schritt 3 und 4 (z.B. wenn Docker nicht laeuft).

.PARAMETER Recreate
    Setzt den Docker-Stack vorher komplett neu auf (docker compose down -v).
    ACHTUNG: loescht das DB-Volume samt aller Daten - genau das prueft aber den
    kompletten initdb-Pfad (01-05) und die Flyway-Baseline.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File ops\verify-setup.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File ops\verify-setup.ps1 -Recreate
#>
[CmdletBinding()]
param(
    [switch] $SkipBuild,
    [switch] $SkipDocker,
    [switch] $Recreate
)

$ErrorActionPreference = 'Stop'
$projektWurzel = Split-Path -Parent $PSScriptRoot
$ergebnisse = [System.Collections.Generic.List[object]]::new()

function Write-Schritt
{
    param([string] $Text)
    Write-Host ''
    Write-Host "==> $Text" -ForegroundColor Cyan
}

function Add-Ergebnis
{
    param(
        [string] $Pruefung,
        [bool] $Erfolg,
        [string] $Hinweis = ''
    )

    $ergebnisse.Add([pscustomobject]@{ Pruefung = $Pruefung; Erfolg = $Erfolg; Hinweis = $Hinweis })
    if ($Erfolg)
    {
        Write-Host "    [ok]   $Pruefung" -ForegroundColor Green
    }
    else
    {
        Write-Host "    [FEHL] $Pruefung $Hinweis" -ForegroundColor Red
    }
}

# Wartet, bis Postgres Verbindungen annimmt. "docker compose up -d" kehrt zurueck,
# sobald der Container laeuft - Postgres selbst ist dann noch mitten in initdb und
# lauscht nicht auf TCP. Ohne dieses Warten startet der Build zu frueh, Flyway
# bekommt keine Verbindung ("Connect timed out") und ALLE @SpringBootTest-Klassen
# scheitern am ApplicationContext. Beim allerersten Start (Altdaten-Uebernahme aus
# ../data.sql) dauert initdb Minuten, daher die grosszuegige Frist.
function Wait-Datenbank
{
    param(
        [string] $DbHost,
        [int]    $Port,
        [int]    $TimeoutSekunden = 300
    )

    $frist = (Get-Date).AddSeconds($TimeoutSekunden)
    Write-Host '    Warte auf die Datenbank (max. 5 Minuten)' -NoNewline

    while ((Get-Date) -lt $frist)
    {
        docker compose exec -T db pg_isready -U frauenhaus_app -d frauenhaus -q 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0)
        {
            # Der Container ist bereit - jetzt noch pruefen, ob der Host den
            # veroeffentlichten Port erreicht (die Tests verbinden sich von aussen).
            $client = [System.Net.Sockets.TcpClient]::new()
            try
            {
                if ($client.ConnectAsync($DbHost, $Port).Wait(2000))
                {
                    Write-Host ' bereit'
                    return $true
                }
            }
            catch { }
            finally { $client.Dispose() }
        }

        Write-Host '.' -NoNewline
        Start-Sleep -Seconds 3
    }

    Write-Host ''
    Write-Host "    Die Datenbank ist unter ${DbHost}:${Port} nicht erreichbar." -ForegroundColor Red
    Write-Host '    Pruefen: docker compose ps  /  docker compose logs db' -ForegroundColor Red
    return $false
}

function Get-JarEintraege
{
    param([string] $JarPfad)

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archiv = [System.IO.Compression.ZipFile]::OpenRead($JarPfad)
    try
    {
        return $archiv.Entries | ForEach-Object { $_.FullName }
    }
    finally
    {
        $archiv.Dispose()
    }
}

function Invoke-Psql
{
    param([string] $Sql)

    $ausgabe = docker compose exec -T db psql -U frauenhaus_app -d frauenhaus -tAc $Sql 2>&1
    if ($LASTEXITCODE -ne 0)
    {
        throw "psql fehlgeschlagen: $ausgabe"
    }
    return ($ausgabe | Out-String).Trim()
}

Push-Location $projektWurzel
try
{
    # --- 1. Build + Tests ---------------------------------------------------
    if (-not $SkipBuild)
    {
        Write-Schritt 'Build inkl. Tests und Produktions-Frontend (mvnw clean verify -Pproduction)'
        Write-Host '    Hinweis: die Integrationstests brauchen die DB auf 127.0.0.1:15432.'
        docker compose up -d db | Out-Null

        # localhost bewusst nicht verwendet: es kann auf ::1 aufloesen, der Port ist
        # laut docker-compose.override.yml aber nur auf 127.0.0.1 veroeffentlicht.
        $env:DB_PORT = '15432'
        $env:DB_HOST = '127.0.0.1'
        $env:DB_USER = 'frauenhaus_app'
        if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = 'frauenhaus' }

        if (-not (Wait-Datenbank -DbHost $env:DB_HOST -Port ([int] $env:DB_PORT)))
        {
            Add-Ergebnis 'Datenbank erreichbar (Voraussetzung der Integrationstests)' $false
            Write-Host ''
            Write-Host 'Abbruch: ohne Datenbank scheitern alle Integrationstests.' -ForegroundColor Red
            exit 1
        }
        Add-Ergebnis 'Datenbank erreichbar (Voraussetzung der Integrationstests)' $true

        & (Join-Path $projektWurzel 'mvnw.cmd') clean verify -Pproduction
        Add-Ergebnis 'Maven-Build inkl. Tests' ($LASTEXITCODE -eq 0) "(Exitcode $LASTEXITCODE)"

        # --- 2. Artefakt-Inhalt --------------------------------------------
        Write-Schritt 'Inhalt der erzeugten JAR pruefen'
        $jar = Get-ChildItem (Join-Path $projektWurzel 'target') -Filter '*.jar' |
               Where-Object { $_.Name -notlike '*.original' } |
               Select-Object -First 1

        if (-not $jar)
        {
            Add-Ergebnis 'JAR gefunden' $false '(target/*.jar fehlt)'
        }
        else
        {
            Write-Host "    Artefakt: $($jar.Name)"
            $eintraege = Get-JarEintraege $jar.FullName

            $mussEnthalten = @(
                'BOOT-INF/classes/db/migration/V1__baseline_schema.sql',
                'BOOT-INF/classes/db/migration/V7__sicherheit_rollen_und_rls.sql',
                'BOOT-INF/classes/db/testdata/V5__testdaten.sql',
                'BOOT-INF/classes/application.yml'
            )
            foreach ($eintrag in $mussEnthalten)
            {
                Add-Ergebnis "JAR enthaelt $eintrag" ($eintraege -contains $eintrag)
            }

            $vaadinBundle = $eintraege | Where-Object { $_ -like '*META-INF/VAADIN/*' } | Select-Object -First 1
            Add-Ergebnis 'JAR enthaelt das gebaute Vaadin-Bundle (META-INF/VAADIN)' ($null -ne $vaadinBundle)

            $initDrin = $eintraege | Where-Object { $_ -like '*classes/db/init/*' }
            Add-Ergebnis 'JAR enthaelt KEINE db/init-Skripte' ($initDrin.Count -eq 0) "($($initDrin -join ', '))"

            $frontendDrin = $eintraege | Where-Object { $_ -like '*classes/frontend/*' }
            Add-Ergebnis 'JAR enthaelt KEINE Frontend-Quellen' ($frontendDrin.Count -eq 0) "($($frontendDrin -join ', '))"
        }
    }

    # --- 3. Docker-Stack ----------------------------------------------------
    if (-not $SkipDocker)
    {
        if ($Recreate)
        {
            Write-Schritt 'Docker-Stack komplett neu aufsetzen (down -v) - loescht das DB-Volume'
            docker compose down -v | Out-Null
        }

        Write-Schritt 'Docker-Stack starten (docker compose up -d --build)'
        docker compose up -d --build
        Add-Ergebnis 'docker compose up' ($LASTEXITCODE -eq 0) "(Exitcode $LASTEXITCODE)"

        Write-Schritt 'Auf den Health-Endpunkt warten (max. 5 Minuten)'
        $frist = (Get-Date).AddMinutes(5)
        $status = 'unbekannt'
        do
        {
            Start-Sleep -Seconds 5
            try
            {
                $status = (Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health' -TimeoutSec 5).status
            }
            catch
            {
                $status = 'startet'
            }
            Write-Host "    Status: $status"
        }
        while ($status -ne 'UP' -and (Get-Date) -lt $frist)

        Add-Ergebnis 'Anwendung meldet Health UP' ($status -eq 'UP') "(letzter Status: $status)"

        # --- 4. Datenbank-Zustand ------------------------------------------
        Write-Schritt 'Datenbank pruefen (Flyway-Historie, Rolle, RLS, append-only)'

        $migrationen = Invoke-Psql "SELECT string_agg(version, ',' ORDER BY installed_rank) FROM frauenhaus.flyway_schema_history"
        Write-Host "    angewendete Versionen: $migrationen"
        Add-Ergebnis 'Flyway-Historie enthaelt V7' ($migrationen -match '(^|,)7($|,)') "(gefunden: $migrationen)"

        $rolle = Invoke-Psql "SELECT count(*) FROM pg_roles WHERE rolname = 'frauenhaus_backend'"
        Add-Ergebnis 'App-Rolle frauenhaus_backend existiert' ($rolle -eq '1')

        $policies = Invoke-Psql "SELECT count(*) FROM pg_policies WHERE policyname = 'benutzerkontext_erforderlich'"
        Write-Host "    RLS-Policies: $policies (erwartet: 11)"
        Add-Ergebnis 'RLS-Policies auf allen personenbezogenen Tabellen' ($policies -eq '11') "(gefunden: $policies)"

        $auditUpdate = Invoke-Psql "SELECT has_table_privilege('frauenhaus_backend', 'frauenhaus.mitglied_aud', 'UPDATE')"
        Add-Ergebnis 'Audit-Historie ist append-only (kein UPDATE)' ($auditUpdate -eq 'f') "(has_table_privilege = $auditUpdate)"

        $flywayLesbar = Invoke-Psql "SELECT has_table_privilege('frauenhaus_backend', 'frauenhaus.flyway_schema_history', 'SELECT')"
        Add-Ergebnis 'Flyway-Historie fuer die App-Rolle gesperrt' ($flywayLesbar -eq 'f') "(has_table_privilege = $flywayLesbar)"

        if ($Recreate)
        {
            $mitglieder = Invoke-Psql 'SELECT count(*) FROM frauenhaus.mitglied'
            Write-Host "    uebernommene Mitglieder: $mitglieder"
            Add-Ergebnis 'Altdaten-Uebernahme hat Mitglieder geschrieben' ([int]$mitglieder -gt 0) "(count = $mitglieder)"
        }
    }
}
finally
{
    Pop-Location
}

# --- Zusammenfassung --------------------------------------------------------
Write-Host ''
Write-Host '=== Zusammenfassung ===' -ForegroundColor Cyan
$ergebnisse | ForEach-Object {
    $markierung = if ($_.Erfolg) { 'ok  ' } else { 'FEHL' }
    Write-Host ("  [{0}] {1}" -f $markierung, $_.Pruefung)
}

$fehler = @($ergebnisse | Where-Object { -not $_.Erfolg })
if ($fehler.Count -gt 0)
{
    Write-Host ''
    Write-Host "$($fehler.Count) Pruefung(en) fehlgeschlagen." -ForegroundColor Red
    exit 1
}

Write-Host ''
Write-Host 'Alle Pruefungen bestanden.' -ForegroundColor Green
