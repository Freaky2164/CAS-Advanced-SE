<#
.SYNOPSIS
    Führt eine vollautomatische SonarQube-Analyse des Frauenhaus-Backends aus.

.DESCRIPTION
    Das Skript erledigt alle Schritte, die sonst manuell nötig wären:
      1. startet die lokale SonarQube (docker-compose.sonarqube.yml, eigener Compose-Stack)
      2. wartet, bis die Instanz den Status "UP" meldet
      3. erzeugt (bzw. erneuert) einen Analyse-Token über die SonarQube-API
      4. startet bei Bedarf die Postgres-DB des App-Stacks, damit die Tests laufen können
      5. ruft "mvnw verify sonar:sonar" auf (JaCoCo-Coverage inklusive)

    Der Token wird zur Laufzeit erzeugt, nur an Maven durchgereicht und nie
    in eine Datei geschrieben.

.PARAMETER AdminPassword
    Passwort des SonarQube-Administrators. Default: Umgebungsvariable
    SONAR_ADMIN_PASSWORD, sonst "admin" (Werkseinstellung der frischen Instanz).

.PARAMETER SkipTests
    Überspringt die Tests. Dann entsteht kein Coverage-Report – die Analyse
    zeigt anschließend 0 % Testabdeckung.

.PARAMETER HostUrl
    Basis-URL der SonarQube-Instanz. Default: http://localhost:9000

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File ops\sonar-analyse.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File ops\sonar-analyse.ps1 -SkipTests -AdminPassword 'geheim'
#>
[CmdletBinding()]
param(
    [string] $AdminPassword = $(if ($env:SONAR_ADMIN_PASSWORD) { $env:SONAR_ADMIN_PASSWORD } else { 'admin' }),
    [switch] $SkipTests,
    [string] $HostUrl = 'http://localhost:9000'
)

$ErrorActionPreference = 'Stop'

$projektWurzel = Split-Path -Parent $PSScriptRoot
$composeDatei  = Join-Path $projektWurzel 'docker-compose.sonarqube.yml'
$composeProjekt = 'sonarqube'
$tokenName     = 'frauenhaus-backend-lokal'

function Invoke-SonarApi
{
    param(
        [Parameter(Mandatory)] [ValidateSet('Get', 'Post')] [string] $Methode,
        [Parameter(Mandatory)] [string] $Pfad,
        [hashtable] $Parameter = @{}
    )

    $basic = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("admin:$AdminPassword"))
    return Invoke-RestMethod -Method $Methode -Uri "$HostUrl$Pfad" -Body $Parameter `
        -Headers @{ Authorization = "Basic $basic" }
}

# --- 1. SonarQube starten ---------------------------------------------------
Write-Host '==> Starte SonarQube-Stack ...' -ForegroundColor Cyan
docker compose -f $composeDatei -p $composeProjekt up -d
if ($LASTEXITCODE -ne 0) { throw 'SonarQube-Container konnte nicht gestartet werden.' }

# --- 2. Auf Betriebsbereitschaft warten -------------------------------------
Write-Host '==> Warte auf Status UP (kann beim ersten Start einige Minuten dauern) ...' -ForegroundColor Cyan
$frist = (Get-Date).AddMinutes(10)
do
{
    Start-Sleep -Seconds 5
    try
    {
        $status = (Invoke-RestMethod -Uri "$HostUrl/api/system/status" -TimeoutSec 10).status
    }
    catch
    {
        $status = 'STARTING'
    }
    Write-Host "    Status: $status"
}
while ($status -ne 'UP' -and (Get-Date) -lt $frist)

if ($status -ne 'UP') { throw "SonarQube ist nicht bereit geworden (letzter Status: $status)." }

# --- 3. Analyse-Token erzeugen ----------------------------------------------
Write-Host '==> Erzeuge Analyse-Token ...' -ForegroundColor Cyan
try
{
    # Alten Token gleichen Namens entfernen; "revoke" ist idempotent.
    Invoke-SonarApi -Methode Post -Pfad '/api/user_tokens/revoke' -Parameter @{ name = $tokenName } | Out-Null
    $token = (Invoke-SonarApi -Methode Post -Pfad '/api/user_tokens/generate' `
        -Parameter @{ name = $tokenName; type = 'GLOBAL_ANALYSIS_TOKEN' }).token
}
catch
{
    throw ("Token-Erzeugung fehlgeschlagen ({0}). Bei einer frisch initialisierten Instanz muss das " +
        "Admin-Passwort einmalig in der UI ({1}) geändert und danach per -AdminPassword bzw. " +
        'SONAR_ADMIN_PASSWORD übergeben werden.') -f $_.Exception.Message, $HostUrl
}

# --- 4. Datenbank für die Tests bereitstellen -------------------------------
if (-not $SkipTests)
{
    Write-Host '==> Stelle sicher, dass die Test-Datenbank läuft ...' -ForegroundColor Cyan
    Push-Location $projektWurzel
    try { docker compose up -d db } finally { Pop-Location }

    # docker-compose.override.yml mappt die DB auf 127.0.0.1:15432 (siehe README)
    $env:DB_PORT     = '15432'
    $env:DB_HOST     = 'localhost'
    $env:DB_USER     = 'frauenhaus_app'
    $env:DB_PASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { 'frauenhaus' }
}

# --- 5. Build + Analyse -----------------------------------------------------
Write-Host '==> Führe Build und Analyse aus ...' -ForegroundColor Cyan
$argumente = @('verify', 'sonar:sonar', "-Dsonar.host.url=$HostUrl", "-Dsonar.token=$token")
if ($SkipTests) { $argumente += '-DskipTests' }

Push-Location $projektWurzel
try
{
    & (Join-Path $projektWurzel 'mvnw.cmd') @argumente
    $bauStatus = $LASTEXITCODE
}
finally
{
    Pop-Location
    # Token nicht länger als nötig im Prozess halten
    $token = $null
}

if ($bauStatus -ne 0) { throw "Analyse fehlgeschlagen (Maven-Exitcode $bauStatus)." }

Write-Host ''
Write-Host "==> Fertig. Ergebnis: $HostUrl/dashboard?id=frauenhaus-backend" -ForegroundColor Green
