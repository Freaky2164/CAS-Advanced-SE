@echo off
echo ===================================
echo Frauenhaus-Verwaltung STATUS
echo ===================================
echo.
cd /d "%~dp0"

docker compose ps
echo.
echo ===================================
echo.
echo Status-Bedeutung:
echo   "running"  = Alles OK
echo   "exited"   = Problem! Starte mit frauenhaus_starten.bat
echo   "starting" = Wird gerade gestartet, bitte warten
echo.
echo ===================================
echo.

REM Zeige auch Speicherverbrauch
echo Speicherverbrauch:
docker compose stats --no-stream 2>nul
echo.
pause
