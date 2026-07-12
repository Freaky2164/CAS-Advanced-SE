@echo off
REM ============================================================
REM SQL Server Diagnose - Fehlerbehandlung
REM ============================================================

cls
echo.
echo ============================================================
echo [DIAGNOSE] SQL Server Verbindungsprobleme
echo ============================================================
echo.

REM === Check 1: SQL Server Services ===
echo [CHECK 1] Laufen SQL Server Services?
echo.

tasklist /FI "IMAGENAME eq sqlservr.exe" 2>NUL | find /I /N "sqlservr.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo [SUCCESS] SQL Server Process laeuft!
    tasklist | find "sqlservr.exe"
) else (
    echo [WARNING] SQL Server Process nicht gefunden!
    echo.
    echo Moegliche Instanzen im System:
    tasklist | findstr /I "sql"
)

echo.
echo ============================================================
echo [CHECK 2] Verfuegbare SQL Server Instanzen?
echo ============================================================
echo.

REM Versuche, verfuegbare Instanzen zu finden
sqlcmd -L 2>NUL
if "%ERRORLEVEL%"=="0" (
    echo [SUCCESS] sqlcmd ausgefuehrt
) else (
    echo [WARNING] sqlcmd nicht gefunden - versuch manuell:
    echo   sqlcmd -L
)

echo.
echo ============================================================
echo [CHECK 3] Kann die App sich verbinden?
echo ============================================================
echo.

REM Versuche, mit localhost zu verbinden
echo Teste: localhost (Standard)
sqlcmd -S localhost -E -Q "SELECT @@VERSION" 2>NUL
if "%ERRORLEVEL%"=="0" (
    echo [SUCCESS] Verbindung zu localhost erfolgreich!
) else (
    echo [INFO] Verbindung zu localhost fehlgeschlagen
)

echo.
echo Teste: localhost\SQLEXPRESS (Express Edition)
sqlcmd -S localhost\SQLEXPRESS -E -Q "SELECT @@VERSION" 2>NUL
if "%ERRORLEVEL%"=="0" (
    echo [SUCCESS] Verbindung zu localhost\SQLEXPRESS erfolgreich!
    echo           Benutze diese Einstellung in localhost.ini!
) else (
    echo [INFO] Verbindung zu localhost\SQLEXPRESS fehlgeschlagen
)

echo.
echo Teste: localhost\MSSQLSERVER (Standard Named Instance)
sqlcmd -S localhost\MSSQLSERVER -E -Q "SELECT @@VERSION" 2>NUL
if "%ERRORLEVEL%"=="0" (
    echo [SUCCESS] Verbindung zu localhost\MSSQLSERVER erfolgreich!
) else (
    echo [INFO] Verbindung zu localhost\MSSQLSERVER fehlgeschlagen
)

echo.
echo Teste: (local) (Default Instance)
sqlcmd -S (local) -E -Q "SELECT @@VERSION" 2>NUL
if "%ERRORLEVEL%"=="0" (
    echo [SUCCESS] Verbindung zu (local) erfolgreich!
) else (
    echo [INFO] Verbindung zu (local) fehlgeschlagen
)

echo.
echo ============================================================
echo [CHECK 4] Existiert die Datenbank 'frauenhaus'?
echo ============================================================
echo.

REM Versuche, sich mit verschiedenen Methoden zu verbinden
echo Versuche mit Windows Authentication...
sqlcmd -S localhost -E -Q "SELECT name FROM sys.databases WHERE name='frauenhaus'" 2>NUL
if "%ERRORLEVEL%"=="0" (
    echo [SUCCESS] Datenbank 'frauenhaus' gefunden!
) else (
    echo [INFO] Abfrage fehlgeschlagen oder Datenbank nicht vorhanden
)

echo.
echo ============================================================
echo [ZUSAMMENFASSUNG UND NÄCHSTEN SCHRITTE]
echo ============================================================
echo.
echo Falls [SUCCESS] fuer CHECK 3 (Verbindung):
echo   - Notiere die Instanz-Bezeichnung
echo   - Passe localhost.ini an:
echo.
echo   Wenn "localhost" funktionierte:
echo     dbhost=localhost
echo     dbsid=frauenhaus
echo.
echo   Wenn "localhost\SQLEXPRESS" funktionierte:
echo     dbhost=localhost
echo     dbsid=frauenhaus;instance\=sqlexpress
echo.
echo   Wenn "localhost\MSSQLSERVER" funktionierte:
echo     dbhost=localhost
echo     dbsid=frauenhaus;instance\=MSSQLSERVER
echo.
echo.
echo Falls ALLE Checks fehlschlagen:
echo   1. Starte SQL Server:
echo      Services (services.msc) oeffnen
echo      Suche: SQL Server
echo      Rechtsklick -> Starten
echo.
echo   2. Falls Service nicht sichtbar:
echo      SQL Server wahrscheinlich nicht installiert!
echo      Download: https://www.microsoft.com/en-us/sql-server/sql-server-downloads
echo.
echo ============================================================
echo.
pause
