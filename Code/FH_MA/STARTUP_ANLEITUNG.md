# Startup-Anleitung – Frauenhaus Adress- und Bußgeldverwaltung (IST-System)

## 🎯 Übersicht

Die alte Anwendung ist eine **Java Swing Desktop-App** (ca. 2005–2013) mit folgender Architektur:

```
Java Swing GUI (run.bat)
         ↓
    JDBC (jTDS)
         ↓
SQL Server 2008/2012 Express
  (Datenbank: frauenhaus)
```

**Wichtig**: Die aktuelle `.ini`-Datei ist auf den alten PC **HOLGER-NBSML** konfiguriert. Wir müssen sie auf deinen aktuellen PC anpassen.

---

## ✅ Voraussetzungen (Checkliste)

### 1. Java Runtime Environment (JRE)
```bash
# Überprüfe deine aktuelle Java-Version
java -version
```

**Anforderung**: Java 1.4 – 1.6+ funktioniert (alte App), aber Java 17+ ist auch kompatibel
- Falls nicht installiert: [Java 17 (OpenJDK / Temurin)](https://adoptium.net/download/) downloaden

### 2. SQL Server-Datenbank
Die App benötigt eine Instanz von **MS SQL Server** mit:
- **Datenbankname**: `frauenhaus`
- **Schema (`compucrash`)**: für UI-Konfiguration
- **Daten-Schema**: Tabellen für Mitglieder, Spenden, Bußgelder, etc.

```sql
-- Überprüfe, ob die DB existiert:
SELECT name FROM sys.databases WHERE name = 'frauenhaus';
```

**Problem**: Das Backup-File `Backup_MSSQL_FH_anonymisiert.bak` liegt vor und muss **wiederhergestellt** werden.

### 3. Netzwerk-Laufwerk Y:
Die `.ini`-Datei referenziert `Y:\` für Reports:
```ini
reports=Y\:
```

Falls nicht vorhanden, muss ein Netzlaufwerk gemappt oder lokal konfiguriert werden.

---

## 🔧 Schritt-für-Schritt Setup

### Schritt 1: Java-Version prüfen

```cmd
java -version
```

**Erwartete Ausgabe**:
```
java version "17.0.x" 2021-09-14 LTS
Java(TM) SE Runtime Environment (build 17.0.x+x)
Java HotSpot(TM) 64-Bit Server VM (build 17.x, mixed mode, sharing)
```

Falls `java: command not found` → [Java 17 LTS installieren](https://adoptium.net/) und `System-Umgebungsvariable JAVA_HOME` setzen.

---

### Schritt 2: SQL Server-Datenbank wiederherstellen

Wir müssen das Backup `Backup_MSSQL_FH_anonymisiert.bak` einspielen.

#### Option A: Via SQL Server Management Studio (SSMS)

1. **SSMS öffnen** und mit SQL Server verbinden
   - Falls noch nicht installiert: [SSMS kostenlos runterladen](https://learn.microsoft.com/en-us/sql/ssms/download-sql-server-management-studio)

2. Im Object Explorer auf **"Databases"** → **Rechtsklick** → **Restore Database...**

3. **General**-Tab:
   - **Restore from**: `Device` wählen
   - **Backup media**: `...` klicken → `Backup_MSSQL_FH_anonymisiert.bak` auswählen

4. **Destination**-Tab:
   - **Database**: `frauenhaus` eingeben (oder der Name, den das Backup hatte)

5. **OK** → warten bis `<DatabaseName> restored successfully` erscheint

#### Option B: Via PowerShell / T-SQL-Script

```powershell
# PowerShell als Administrator ausführen
$backupFile = "C:\Users\p.faller\Documents\Master\ASE\FH_MA\Backup_MSSQL_FH_anonymisiert.bak"
$serverInstance = "."  # oder "tcp:localhost"
$databaseName = "frauenhaus"

# SQL-Restore-Befehl
$restoreCmd = 
@"
RESTORE DATABASE [$databaseName]
FROM DISK = '$backupFile'
WITH REPLACE
"@

# Ausführen
Invoke-SqlCmd -ServerInstance $serverInstance -Query $restoreCmd -QueryTimeout 3600
```

**Überprüfung** (in SSMS):
```sql
USE frauenhaus;
-- Prüfe, ob die wichtigsten Tabellen existieren
SELECT COUNT(*) FROM frauenhaus.mitglied;
SELECT COUNT(*) FROM frauenhaus.spende;
SELECT COUNT(*) FROM frauenhaus.bussgeld;
SELECT COUNT(*) FROM compucrash.user_def;
```

---

### Schritt 3: .ini-Datei konfigurieren

Die aktuell verwendete `pc-6.ini` ist noch auf den **alten PC HOLGER-NBSML** konfiguriert.
Wir müssen sie auf deinen PC anpassen.

**Erstelle eine neue Datei:** `C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z\localhost.ini`

```ini
# Frauenhaus Adress- und Bußgeldverwaltung
# Konfiguration für lokalen Betrieb

# ===== Datenbankverbindung =====
database=SQLSERVER
dbhost=localhost
dbport=1433
dbsid=frauenhaus;instance\=MSSQLSERVER
# Für SQL Server Express: instance\=sqlexpress
dbuser=sa
# Hinweis: Passwort wird bei Login abgefragt, nicht hier speichern!
access=ODBC

# ===== UI-Einstellungen =====
title=Frauenhaus Adress und Busgeldverwaltung
custom=frauenhaus
email=frauenhaus-mannheim@t-online.de
icon=images/frauenhaus_logo.jpg
debug=true

# ===== Verzeichnisse =====
# Lokal speichern (kein Y:\ Netzwerk nötig für Tests)
reports=C\:/frauenhaus/reports
vorlagen=frauenhaus\\vorlagen
excel=C\:\\Program Files (x86)\\Microsoft Office\\Office14

# ===== GUI-Fenster-Positionen (auto-gespeichert) =====
mitglied.list.width=1920
mitglied.list.height=1046
mitglied.list.x=0
mitglied.list.y=0

spende.list.width=905
spende.list.height=547
spende.list.x=0
spende.list.y=0

bussgeld.list.width=1920
bussgeld.list.height=1046
bussgeld.list.x=0
bussgeld.list.y=0

null.width=299
null.height=140
null.x=0
null.y=0
```

**Erstelle auch die Report- und Template-Verzeichnisse:**
```cmd
mkdir C:\frauenhaus\reports
mkdir C:\frauenhaus\vorlagen
```

---

### Schritt 4: Korrigiertes run.bat-Script erstellen

Die ursprüngliche `run.bat` hat Pfade auf **C:\Holger\FH_MA\...** 

Erstelle eine neue Datei: `C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z\run_local.bat`

```batch
@echo off
REM ============================================================
REM Frauenhaus Adress- und Busgeldverwaltung - Startup Script
REM ============================================================

REM === Java-Pfad prüfen ===
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Java nicht gefunden! Bitte installieren.
    pause
    exit /b 1
)

REM === Aktuelles Verzeichnis ermitteln ===
set APPDIR=%~dp0
echo [INFO] App-Verzeichnis: %APPDIR%

REM === Classpath setzen (alle JARs aus ext/)===
set CLASSPATH=%APPDIR%ext\acrobat.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\dnsns.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\edtftpj.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\joc-v14.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\joda-time-1.0.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\joutlookconnector.dll
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\jtds-1.1.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\localedata.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\mysql-connector-java-3.1.10-bin.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\ojdbc14.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\poi-2.5.1-final-20040804.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\poi-contrib-2.5.1-final-20040804.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\poi-scratchpad-2.5.1-final-20040804.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\sunjce_provider.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\sunpkcs11.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%ext\wordProcessing.jar
set CLASSPATH=%CLASSPATH%;%APPDIR%.

REM === Kompilierte .class-Dateien zum Classpath hinzufügen ===
set CLASSPATH=%CLASSPATH%;%APPDIR%compucrash
set CLASSPATH=%CLASSPATH%;%APPDIR%frauenhaus

echo [INFO] Classpath gesetzt

REM === Konfigurationsdatei auswählen ===
if "%1"=="" (
    set CONFIGFILE=%APPDIR%localhost.ini
    echo [INFO] Verwende Standard-Konfiguration: %CONFIGFILE%
) else (
    set CONFIGFILE=%APPDIR%%1
    echo [INFO] Verwende Konfiguration: %CONFIGFILE%
)

if not exist "%CONFIGFILE%" (
    echo ERROR: Konfigurationsdatei nicht gefunden: %CONFIGFILE%
    pause
    exit /b 1
)

REM === Anwendung starten ===
echo.
echo [STARTEN] Frauenhaus Verwaltung...
echo.
java.exe compucrash.CStart "%CONFIGFILE%"

REM === Fehlerbehandlung ===
if errorlevel 1 (
    echo.
    echo ERROR: Die Anwendung ist mit einem Fehler beendet worden.
    pause
)
```

**Speichern** unter: `C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z\run_local.bat`

---

### Schritt 5: Anwendung starten

```cmd
cd C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z
run_local.bat
```

**Oder mit spezifischer Konfiguration:**
```cmd
run_local.bat pc-6.ini
```

**Erwartete Ausgabe:**
```
[INFO] App-Verzeichnis: C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z\
[INFO] Classpath gesetzt
[INFO] Verwende Standard-Konfiguration: C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z\localhost.ini
[STARTEN] Frauenhaus Verwaltung...

[Splash Screen wird angezeigt – 5 Sekunden]

[Login-Fenster öffnet sich]
```

---

## 🔑 Anmelden

Nach Start sollte das **Login-Fenster** (CLoginFrame) erscheinen:

```
┌──────────────────────────────────┐
│  Frauenhaus Verwaltung - Anmeld. │
├──────────────────────────────────┤
│ Benutzer:   [dorle         ]    │
│ Passwort:   [          ]        │
├──────────────────────────────────┤
│  [Ok]                    [Abort] │
└──────────────────────────────────┘
```

**Standard-Benutzer (aus der Datenbank):**
- Benutzername: `dorle` (siehe in `pc-6.ini: dbuser=dorle`)
- Passwort: ??? (aus der `compucrash.user_def`-Tabelle)

**Um das Passwort zu prüfen:**
```sql
USE frauenhaus;
SELECT user_name, password FROM compucrash.user_def;
```

---

## ⚠️ Häufige Fehler & Lösungen

| Fehler | Ursache | Lösung |
|--------|--------|--------|
| `java: command not found` | Java nicht installiert/nicht im PATH | [Java 17+ installieren](https://adoptium.net/), JAVA_HOME setzen |
| `com.microsoft.sqlserver.jdbc.SQLServerException: Login failed` | DB nicht erreichbar | SQL Server läuft? Hostname/Port korrekt? |
| `SqlException: The index does not exist` | Falsche Datenbank | Backup korrekt eingespielt? Schema `compucrash` vorhanden? |
| `FileNotFoundException: ...vorlagen...` | Verzeichnis existiert nicht | `mkdir C:\frauenhaus\vorlagen` erstellen |
| `ClassNotFoundException: compucrash.CStart` | Classpath falsch | run_local.bat aus dem `Z\` Verzeichnis ausführen |
| `NullPointerException` beim GUI-Aufbau | DB-Schema fehlt | Alle nötigen Tabellen in der Datenbank? |

---

## 🔍 Debugging

Falls Probleme auftreten, erhöhe das Debug-Level:

**In der .ini-Datei:**
```ini
debug=true
```

Die App gibt dann zusätzliche Debug-Ausgaben in der Console aus.

---

## 📊 Überprüfungs-Checkliste

- [ ] Java-Version: `java -version` zeigt Java 1.4+
- [ ] SQL Server läuft: `sqlcmd -L` zeigt verfügbare Server
- [ ] Datenbank existiert: `SELECT name FROM sys.databases WHERE name='frauenhaus'`
- [ ] Tabellen vorhanden: `USE frauenhaus; SELECT COUNT(*) FROM mitglied;`
- [ ] `.ini`-Datei existiert: `C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z\localhost.ini`
- [ ] Pfade in `.ini` erreichbar: `echo %reports%`
- [ ] run_local.bat ist ausführbar
- [ ] Kein Whitespace in Pfaden (wenn möglich)
- [ ] Reports-Verzeichnis existiert: `C:\frauenhaus\reports\`

---

## 🚀 Schnellstart (TL;DR)

```cmd
REM 1. Backup einspielen (SSMS oder PowerShell)
REM    → Backup_MSSQL_FH_anonymisiert.bak → RESTORE DATABASE

REM 2. localhost.ini erstellen (siehe Schritt 3)

REM 3. run_local.bat ausführen
cd C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z
run_local.bat

REM 4. Login-Daten in die Datenbank überprüfen
REM    (Standard: dorle / ???)
```

---

**Fragen? Fehler? Lass mich wissen, dann helfe ich weiter!** 🔧
