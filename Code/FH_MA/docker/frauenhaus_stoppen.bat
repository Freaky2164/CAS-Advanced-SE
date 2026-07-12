@echo off
echo ===================================
echo Frauenhaus-Verwaltung STOPPEN
echo ===================================
echo.
cd /d "%~dp0"
docker compose down
echo.
echo [OK] Anwendung gestoppt.
echo     Alle Daten bleiben erhalten!
echo.
pause
