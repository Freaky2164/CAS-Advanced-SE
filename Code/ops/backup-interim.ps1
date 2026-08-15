<#
.SYNOPSIS
    Uebergangs-Backupjob: erzeugt einen verschluesselten, protokollierten
    pg_dump der Frauenhaus-Datenbank auf ein physisch getrenntes Ziel.

.DESCRIPTION
    Lauffaehige Zwischenstufe bis zur produktiven pgBackRest-Integration
    (siehe ops/pgbackrest.conf). Der Job laeuft vollstaendig unter Windows und
    wird ueber die Windows-Aufgabenplanung zeitgesteuert ausgefuehrt.

    Ablauf:
      1. Vorpruefung : Docker erreichbar, DB-Container laeuft, Ziel beschreibbar,
                       Passphrasendatei vorhanden und ausreichend restriktiv
      2. Dump        : pg_dump -F c schreibt IM Container direkt in eine Datei
                       (-f). Eine PowerShell-Umleitung (>) scheidet aus, weil
                       Windows PowerShell 5.1 den Stream als UTF-16LE-Text
                       kodiert und den Custom-Format-Dump damit zerstoert.
      3. Uebernahme  : docker cp holt die Datei binaergetreu auf den Host,
                       anschliessend wird die Containerkopie geloescht
      4. Sicherung   : gpg --symmetric (AES256) verschluesselt den Dump auf das
                       Zielvolume; der unverschluesselte Zwischenstand wird
                       entfernt
      5. Nachweis    : SHA-256, Groesse und Dauer werden protokolliert
      6. Aufbewahrung: Sicherungen aelter als -RetentionDays werden geloescht,
                       sofern mindestens -MinKeep Sicherungen erhalten bleiben

    Jeder Teilschritt wird ueber $LASTEXITCODE geprueft und in die Logdatei
    geschrieben; das Monitoring wertet Alter und Ergebnis des letzten Laufs aus.
    Der Exitcode ist 0 bei Erfolg und 1 im Fehlerfall.

    ACHTUNG: Der Job ersetzt pgBackRest nicht. Er kennt kein Point-in-Time
    Recovery (keine WAL-Archivierung) und ersetzt keinen protokollierten
    Restore-Test.

.PARAMETER Container
    Name oder ID des PostgreSQL-Containers. Ohne Angabe wird der Container des
    Compose-Dienstes "db" ermittelt; schlaegt das fehl, gilt 'frauenhaus-db'.

.PARAMETER Database
    Name der zu sichernden Datenbank. Standard: frauenhaus

.PARAMETER DbUser
    Datenbankrolle fuer den Dump. Standard: frauenhaus_app (Schema-Eigentuemer,
    die eingeschraenkte RLS-Rolle frauenhaus_backend darf NICHT verwendet
    werden, da sie nicht alle Zeilen lesen kann).

.PARAMETER TargetPath
    Zielverzeichnis auf einem physisch getrennten Volume.
    Standard: D:\backups\frauenhaus

.PARAMETER PassphraseFile
    Datei mit der GPG-Passphrase. Liegt bewusst NICHT im Backup und wird per
    ACL auf das Dienstkonto beschraenkt:
    icacls C:\secure\backup.pass /inheritance:r /grant:r SYSTEM:R
    Standard: C:\secure\backup.pass

.PARAMETER RetentionDays
    Aufbewahrungsdauer in Tagen. Standard: 14

.PARAMETER MinKeep
    Mindestanzahl vorzuhaltender Sicherungen, unabhaengig vom Alter.
    Verhindert, dass eine laenger stillstehende Umgebung alle Sicherungen
    verliert. Standard: 3

.PARAMETER SkipRetention
    Ueberspringt Schritt 6 (Aufbewahrung), z.B. fuer einen manuellen Zusatzlauf.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File ops\backup-interim.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File ops\backup-interim.ps1 `
        -TargetPath 'E:\backups\frauenhaus' -RetentionDays 30

.EXAMPLE
    # Registrierung in der Windows-Aufgabenplanung (taeglich 02:00 Uhr)
    schtasks /Create /TN "Frauenhaus-DB-Backup" /SC DAILY /ST 02:00 `
      /TR "powershell -ExecutionPolicy Bypass -File C:\ops\backup-interim.ps1" /RU SYSTEM
#>
[CmdletBinding()]
param(
    [string] $Container,
    [string] $Database       = 'frauenhaus',
    [string] $DbUser         = 'frauenhaus_app',
    [string] $TargetPath     = 'D:\backups\frauenhaus',
    [string] $PassphraseFile = 'C:\secure\backup.pass',
    [ValidateRange(1, 3650)]
    [int]    $RetentionDays  = 14,
    [ValidateRange(1, 100)]
    [int]    $MinKeep        = 3,
    [switch] $SkipRetention
)

$ErrorActionPreference = 'Stop'

$stamp     = Get-Date -Format 'yyyyMMdd-HHmmss'
$start     = Get-Date
$logDatei  = Join-Path $TargetPath 'backup.log'
$inDump    = "/tmp/$Database-$stamp.dump"
$outDump   = Join-Path $env:TEMP "$Database-$stamp.dump"
$zielDatei = Join-Path $TargetPath "$Database-$stamp.dump.gpg"

function Write-Log
{
    param(
        [Parameter(Mandatory)][string] $Text,
        [ValidateSet('INFO', 'WARN', 'FEHLER')][string] $Stufe = 'INFO'
    )

    $zeile = '{0} {1,-6} {2}' -f (Get-Date -Format o), $Stufe, $Text
    Write-Host $zeile
    try
    {
        Add-Content -Encoding utf8 -Path $logDatei -Value $zeile
    }
    catch
    {
        # Ein nicht schreibbares Log darf den Backuplauf nicht verhindern,
        # muss aber sichtbar bleiben (Aufgabenplanung protokolliert stdout).
        Write-Host "WARN   Logdatei nicht schreibbar: $($_.Exception.Message)"
    }
}

function Invoke-Schritt
{
    <#
        Fuehrt ein externes Kommando aus und bricht bei einem Exitcode <> 0 ab.
        Die Ausgabe wird eingesammelt, damit sie im Fehlerfall im Log landet.
    #>
    param(
        [Parameter(Mandatory)][string]   $Beschreibung,
        [Parameter(Mandatory)][string]   $Datei,
        [Parameter(Mandatory)][string[]] $Argumente,
        [switch] $FehlerTolerieren
    )

    $ausgabe = & $Datei @Argumente 2>&1
    $code    = $LASTEXITCODE

    if ($code -ne 0)
    {
        $text = ($ausgabe | Out-String).Trim()
        if ($FehlerTolerieren)
        {
            Write-Log "$Beschreibung fehlgeschlagen (Exitcode $code): $text" -Stufe WARN
            return $false
        }
        throw "$Beschreibung fehlgeschlagen (Exitcode $code): $text"
    }

    return $true
}

function Resolve-Container
{
    if ($Container)
    {
        return $Container
    }

    # Compose vergibt ohne container_name projektabhaengige Namen (z.B. code-db-1).
    $projektWurzel = Split-Path -Parent $PSScriptRoot
    $composeDatei  = Join-Path $projektWurzel 'docker-compose.yml'

    if (Test-Path -LiteralPath $composeDatei)
    {
        $id = & docker compose -f $composeDatei ps -q db 2>&1
        if ($LASTEXITCODE -eq 0 -and $id)
        {
            return ($id | Select-Object -First 1).Trim()
        }
    }

    return 'frauenhaus-db'
}

$erfolgreich = $false

try
{
    # ---------------------------------------------------------------- 1. Vorpruefung
    if (-not (Test-Path -LiteralPath $TargetPath))
    {
        New-Item -ItemType Directory -Path $TargetPath -Force | Out-Null
    }

    Write-Log "Start Uebergangs-Backup $stamp (Ziel: $TargetPath)"

    foreach ($werkzeug in 'docker', 'gpg')
    {
        if (-not (Get-Command $werkzeug -ErrorAction SilentlyContinue))
        {
            throw "Benoetigtes Werkzeug '$werkzeug' wurde nicht gefunden (PATH pruefen)."
        }
    }

    if (-not (Test-Path -LiteralPath $PassphraseFile))
    {
        throw "Passphrasendatei '$PassphraseFile' fehlt. Ohne Schluesselmaterial wird kein unverschluesseltes Backup abgelegt."
    }

    # Die Passphrase darf nicht fuer normale Benutzerkonten lesbar sein.
    $zugriff = (Get-Acl -LiteralPath $PassphraseFile).Access |
        Where-Object { $_.IdentityReference -match 'Users|Jeder|Everyone|Authenticated' }
    if ($zugriff)
    {
        Write-Log "Passphrasendatei ist zu weit freigegeben ($($zugriff.IdentityReference -join ', ')). Empfehlung: icacls '$PassphraseFile' /inheritance:r /grant:r SYSTEM:R" -Stufe WARN
    }

    $containerName = Resolve-Container
    $status = (& docker inspect -f '{{.State.Running}}' $containerName 2>&1)
    if ($LASTEXITCODE -ne 0 -or "$status".Trim() -ne 'true')
    {
        throw "Datenbankcontainer '$containerName' laeuft nicht."
    }
    Write-Log "Datenbankcontainer: $containerName"

    # ---------------------------------------------------------------- 2. Dump
    # Binaersicherer Dump direkt in eine Datei; KEINE PowerShell-Umleitung (>).
    Invoke-Schritt -Beschreibung 'pg_dump' -Datei 'docker' -Argumente @(
        'exec', $containerName,
        'pg_dump', '-U', $DbUser, '-d', $Database, '-F', 'c', '-Z', '6', '-f', $inDump
    ) | Out-Null

    # ---------------------------------------------------------------- 3. Uebernahme
    Invoke-Schritt -Beschreibung 'docker cp' -Datei 'docker' -Argumente @(
        'cp', "${containerName}:$inDump", $outDump
    ) | Out-Null

    Invoke-Schritt -Beschreibung 'Aufraeumen im Container' -Datei 'docker' -Argumente @(
        'exec', $containerName, 'rm', '-f', $inDump
    ) -FehlerTolerieren | Out-Null

    if (-not (Test-Path -LiteralPath $outDump))
    {
        throw "Dump '$outDump' wurde nicht erzeugt."
    }

    $rohGroesse = (Get-Item -LiteralPath $outDump).Length
    if ($rohGroesse -lt 1kb)
    {
        throw "Dump ist mit $rohGroesse Byte unplausibel klein - Abbruch ohne Ablage."
    }

    # ---------------------------------------------------------------- 4. Verschluesselung
    Invoke-Schritt -Beschreibung 'gpg-Verschluesselung' -Datei 'gpg' -Argumente @(
        '--batch', '--yes', '--symmetric', '--cipher-algo', 'AES256',
        '--passphrase-file', $PassphraseFile,
        '--output', $zielDatei, $outDump
    ) | Out-Null

    Remove-Item -LiteralPath $outDump -Force

    if (-not (Test-Path -LiteralPath $zielDatei))
    {
        throw "Verschluesselte Sicherung '$zielDatei' fehlt nach der Verschluesselung."
    }

    # ---------------------------------------------------------------- 5. Nachweis
    $ziel   = Get-Item -LiteralPath $zielDatei
    $hash   = (Get-FileHash -LiteralPath $zielDatei -Algorithm SHA256).Hash
    $dauer  = [int]((Get-Date) - $start).TotalSeconds
    $mbRoh  = [math]::Round($rohGroesse / 1mb, 2)
    $mbZiel = [math]::Round($ziel.Length / 1mb, 2)

    Write-Log "OK $stamp Datei=$($ziel.Name) Groesse=${mbZiel}MB (Dump ${mbRoh}MB) SHA256=$hash Dauer=${dauer}s"
    Add-Content -Encoding utf8 -Path "$zielDatei.sha256" -Value "$hash *$($ziel.Name)"

    # ---------------------------------------------------------------- 6. Aufbewahrung
    if ($SkipRetention)
    {
        Write-Log 'Aufbewahrung uebersprungen (-SkipRetention).'
    }
    else
    {
        $grenze  = (Get-Date).AddDays(-$RetentionDays)
        $alle    = @(Get-ChildItem -LiteralPath $TargetPath -Filter '*.dump.gpg' | Sort-Object LastWriteTime -Descending)
        $behalten = @($alle | Select-Object -First $MinKeep)
        $alt      = @($alle | Where-Object { $_.LastWriteTime -lt $grenze -and $behalten -notcontains $_ })

        foreach ($datei in $alt)
        {
            Remove-Item -LiteralPath $datei.FullName -Force
            Remove-Item -LiteralPath "$($datei.FullName).sha256" -Force -ErrorAction SilentlyContinue
            Write-Log "Aufbewahrung: $($datei.Name) geloescht (aelter als $RetentionDays Tage)."
        }

        Write-Log "Bestand: $($alle.Count - $alt.Count) Sicherungen, davon $($alt.Count) in diesem Lauf entfernt."
    }

    $erfolgreich = $true
}
catch
{
    Write-Log "Lauf $stamp abgebrochen: $($_.Exception.Message)" -Stufe FEHLER
}
finally
{
    # Ein unverschluesselter Zwischenstand darf unter keinen Umstaenden liegen bleiben.
    if (Test-Path -LiteralPath $outDump)
    {
        Remove-Item -LiteralPath $outDump -Force -ErrorAction SilentlyContinue
        Write-Log 'Unverschluesselter Zwischenstand entfernt.' -Stufe WARN
    }
}

if (-not $erfolgreich)
{
    exit 1
}

exit 0
