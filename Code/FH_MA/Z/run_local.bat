@echo off
REM ============================================================
REM Frauenhaus Adress- und Busgeldverwaltung - Startup Script
REM ============================================================
REM Dieses Skript startet die alte Swing-Anwendung mit lokalen
REM Einstellungen. Es korrigiert die urspruenglichen Pfade von
REM C:\Holger\FH_MA auf das aktuelle Verzeichnis.
REM ============================================================

setlocal enabledelayedexpansion

REM === Java-Pfad prüfen ===
where java >nul 2>nul
if errorlevel 1 (
    cls
    echo.
    echo ############################################################
    echo ERROR: Java nicht gefunden!
    echo ############################################################
    echo.
    echo Eine Java Runtime Environment ist erforderlich.
    echo Bitte installieren Sie Java 17 LTS oder neuer:
    echo https://adoptium.net/download/
    echo.
    echo Nach Installation bitte diese Datei erneut starten.
    echo.
    pause
    exit /b 1
)

REM === Aktuelles Verzeichnis ermitteln ===
set APPDIR=%~dp0
echo [INFO] App-Verzeichnis: %APPDIR%

REM === Classpath setzen (alle JARs aus ext/) ===
set CLASSPATH=%APPDIR%ext\acrobat.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\dnsns.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\edtftpj.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\joc-v14.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\joda-time-1.0.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\joutlookconnector.dll
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\jtds-1.1.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\localedata.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\mysql-connector-java-3.1.10-bin.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\ojdbc14.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\poi-2.5.1-final-20040804.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\poi-contrib-2.5.1-final-20040804.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\poi-scratchpad-2.5.1-final-20040804.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\sunjce_provider.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\sunpkcs11.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%ext\wordProcessing.jar
set CLASSPATH=!CLASSPATH!;%APPDIR%.

REM === Kompilierte .class-Dateien zum Classpath ===
set CLASSPATH=!CLASSPATH!;%APPDIR%compucrash
set CLASSPATH=!CLASSPATH!;%APPDIR%frauenhaus

echo [INFO] Classpath konfiguriert

REM === Konfigurationsdatei auswählen ===
if "%1"=="" (
    set CONFIGFILE=%APPDIR%localhost.ini
    echo [INFO] Verwende Standard-Konfiguration: localhost.ini
) else (
    set CONFIGFILE=%APPDIR%%1
    echo [INFO] Verwende Konfiguration: %1
)

if not exist "!CONFIGFILE!" (
    cls
    echo.
    echo ############################################################
    echo ERROR: Konfigurationsdatei nicht gefunden!
    echo ############################################################
    echo.
    echo Gesuchte Datei: !CONFIGFILE!
    echo.
    echo Bitte erstellen Sie die Konfigurationsdatei oder uebergeben
    echo Sie den korrekten Dateinamen als Parameter:
    echo.
    echo   run_local.bat localhost.ini
    echo.
    pause
    exit /b 1
)

REM === Verzeichnisse erstellen, falls nicht vorhanden ===
if not exist "C:\frauenhaus\reports" mkdir "C:\frauenhaus\reports"
if not exist "C:\frauenhaus\vorlagen" mkdir "C:\frauenhaus\vorlagen"

echo [INFO] Reports-Verzeichnis: C:\frauenhaus\reports\
echo [INFO] Vorlagen-Verzeichnis: C:\frauenhaus\vorlagen\

REM === Anwendung starten ===
echo.
echo ============================================================
echo [STARTEN] Frauenhaus Adress- und Busgeldverwaltung
echo ============================================================
echo.
echo Benutzername: (wird im Login-Fenster abgefragt)
echo Passwort:    (wird im Login-Fenster abgefragt)
echo.
echo Bitte warten Sie auf das Splash-Screen...
echo.

java.exe compucrash.CStart "!CONFIGFILE!"

REM === Fehlerbehandlung ===
if errorlevel 1 (
    echo.
    echo ############################################################
    echo FEHLER: Die Anwendung ist mit einem Fehler beendet worden
    echo ############################################################
    echo.
    pause
    exit /b 1
)

exit /b 0
