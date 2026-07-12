# ============================================================
# Frauenhaus - SQL Server Datenbank Wiederherstellung
# ============================================================
# Dieses Skript stellt das Backup der Datenbank wieder her.
# 
# Anforderungen:
# - PowerShell als Administrator ausfuehren
# - SQL Server LocalDB oder SQL Server Express/Standard muss laufen
# - Das SqlServer PowerShell-Modul wird ggf. automatisch geladen
#
# Verwendung:
#   .\Restore-Database.ps1
# ============================================================

param(
    [string]$BackupFile = "C:\Users\p.faller\Documents\Master\ASE\FH_MA\Backup_MSSQL_FH_anonymisiert.bak",
    [string]$ServerInstance = ".",
    [string]$DatabaseName = "frauenhaus"
)

# Farben fuer Console-Ausgabe
function Write-Header {
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host $args -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor Cyan
}

function Write-Success {
    Write-Host "[SUCCESS] $args" -ForegroundColor Green
}

function Write-Error-Custom {
    Write-Host "[ERROR] $args" -ForegroundColor Red
}

function Write-Info {
    Write-Host "[INFO] $args" -ForegroundColor Yellow
}

# ===== PRAEAMBEL =====
Write-Header "Frauenhaus Datenbank-Wiederherstellung"
Write-Host ""
Write-Info "Backup-Datei: $BackupFile"
Write-Info "SQL Server: $ServerInstance"
Write-Info "Datenbank: $DatabaseName"
Write-Host ""

# ===== ADMINISTRATORRECHTE PRUEFEN =====
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Error-Custom "Dieses Skript muss als Administrator ausgefuehrt werden!"
    Write-Host "Bitte PowerShell mit 'als Administrator ausfuehren' starten."
    pause
    exit 1
}

# ===== BACKUP-DATEI PRUEFEN =====
if (-not (Test-Path $BackupFile)) {
    Write-Error-Custom "Backup-Datei nicht gefunden: $BackupFile"
    exit 1
}

Write-Success "Backup-Datei existiert"

# ===== SQL SERVER MODUL LADEN =====
Write-Info "Lade SQL Server PowerShell-Modul..."
try {
    # Versuche SqlServer-Modul zu laden
    Import-Module SqlServer -ErrorAction Stop
    Write-Success "SqlServer-Modul geladen"
} catch {
    Write-Info "SqlServer-Modul nicht verfuegbar. Verwende SMO stattdessen..."
    [System.Reflection.Assembly]::LoadWithPartialName("Microsoft.SqlServer.SMO") | Out-Null
}

# ===== DATENBANK-VERBINDUNG TESTEN =====
Write-Info "Teste Verbindung zu SQL Server: $ServerInstance"
try {
    if ($ServerInstance -eq ".") {
        $server = New-Object Microsoft.SqlServer.Management.Smo.Server "localhost"
    } else {
        $server = New-Object Microsoft.SqlServer.Management.Smo.Server $ServerInstance
    }
    
    # Teste Verbindung durch Abruf von Informationen
    $version = $server.Information.Version
    Write-Success "Erfolgreich verbunden mit SQL Server $version"
} catch {
    Write-Error-Custom "Kann sich nicht mit SQL Server verbinden: $_"
    pause
    exit 1
}

# ===== BESTEHENDE DATENBANK PRUEFEN =====
$existingDb = $server.Databases | Where-Object { $_.Name -eq $DatabaseName }
if ($existingDb) {
    Write-Host ""
    Write-Host "Die Datenbank '$DatabaseName' existiert bereits!" -ForegroundColor Yellow
    Write-Host ""
    $continue = Read-Host "Moechten Sie sie ueberschreiben? (j/n)"
    
    if ($continue -ne "j") {
        Write-Host "Abgebrochen."
        exit 0
    }
    
    Write-Info "Loesche bestehende Datenbank '$DatabaseName'..."
    try {
        $server.KillAllProcesses($DatabaseName)
        $server.Databases[$DatabaseName].Drop()
        Write-Success "Datenbank geloescht"
    } catch {
        Write-Error-Custom "Fehler beim Loeschen: $_"
        exit 1
    }
    
    Start-Sleep -Seconds 2
}

# ===== BACKUP WIEDERHERSTELLEN =====
Write-Host ""
Write-Info "Stelle Datenbank aus Backup wieder her..."
Write-Info "Dies kann mehrere Minuten dauern..."
Write-Host ""

try {
    $restore = New-Object Microsoft.SqlServer.Management.Smo.Restore
    $restore.Devices.AddDevice($BackupFile, [Microsoft.SqlServer.Management.Smo.DeviceType]::File)
    $restore.Database = $DatabaseName
    $restore.ReplaceDatabase = $true
    $restore.PercentCompleteNotification = 10
    
    Register-ObjectEvent -InputObject $restore -EventName PercentComplete -SourceIdentifier "RestorePercent" -Action {
        Write-Host "  ... $($eventargs[0].Percent)% abgeschlossen" -ForegroundColor Cyan
    } | Out-Null
    
    $restore.SqlRestore($server)
    
    Unregister-Event -SourceIdentifier "RestorePercent" -ErrorAction SilentlyContinue
    
    Write-Success "Datenbank erfolgreich wiederhergestellt!"
    
} catch {
    Write-Error-Custom "Fehler beim Wiederherstellen: $_"
    pause
    exit 1
}

# ===== WIEDERHERGESTELLTE DATENBANK PRUEFEN =====
Write-Host ""
Write-Info "Ueberpruefte wiederhergestellte Datenbank..."

try {
    $server.Refresh()
    $db = $server.Databases[$DatabaseName]
    
    if ($db) {
        Write-Success "Datenbank '$DatabaseName' existiert"
        Write-Info "Datenbank-Besitzer: $($db.Owner)"
        Write-Info "Wiederherstellungsmodell: $($db.RecoveryModel)"
        Write-Info "Groesse: $($db.Size) KB"
        Write-Info "Tabellen: $($db.Tables.Count)"
        
        # Prueefe wichtige Tabellen
        Write-Host ""
        Write-Info "Prueefe wichtige Tabellen:"
        
        $tablesToCheck = @("mitglied", "spende", "bussgeld", "user_def")
        foreach ($table in $tablesToCheck) {
            $existingTable = $db.Tables | Where-Object { $_.Name -eq $table }
            if ($existingTable) {
                Write-Success "  ✓ Tabelle '$table' vorhanden"
            } else {
                Write-Host "  ✗ Tabelle '$table' NICHT gefunden" -ForegroundColor Red
            }
        }
    } else {
        Write-Error-Custom "Datenbank konnte nach Wiederherstellung nicht gefunden werden!"
        exit 1
    }
    
} catch {
    Write-Error-Custom "Fehler beim Ueberpruefen: $_"
}

# ===== ABSCHLUSS =====
Write-Host ""
Write-Header "Wiederherstellung abgeschlossen!"
Write-Host ""
Write-Info "Die Datenbank '$DatabaseName' ist jetzt einsatzbereit."
Write-Info "Du kannst die Anwendung jetzt starten:"
Write-Host ""
Write-Host "  cd C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z" -ForegroundColor Cyan
Write-Host "  .\run_local.bat" -ForegroundColor Cyan
Write-Host ""

pause
