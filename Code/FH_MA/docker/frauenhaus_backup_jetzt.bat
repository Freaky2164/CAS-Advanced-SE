@echo off
echo ===================================
echo Frauenhaus-Verwaltung SOFORT-BACKUP
echo ===================================
echo.
cd /d "%~dp0"

echo Erstelle Backup der Datenbank...
echo.

for /f "tokens=2 delims==" %%i in ('wmic os get localdatetime /value') do set datetime=%%i
set TIMESTAMP=%datetime:~0,8%_%datetime:~8,6%

docker compose exec db /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "%MSSQL_SA_PASSWORD%" -C -Q "BACKUP DATABASE [frauenhaus] TO DISK = N'/backups/frauenhaus_manual_%TIMESTAMP%.bak' WITH COMPRESSION, CHECKSUM, STATS = 10"

if errorlevel 1 (
    echo.
    echo [FEHLER] Backup fehlgeschlagen!
    echo Ist die Datenbank gestartet?
    echo Fuehre frauenhaus_status.bat aus um zu pruefen.
) else (
    echo.
    echo ===================================
    echo [OK] Backup erfolgreich erstellt!
    echo ===================================
    echo Datei: frauenhaus_manual_%TIMESTAMP%.bak
)
echo.
pause
