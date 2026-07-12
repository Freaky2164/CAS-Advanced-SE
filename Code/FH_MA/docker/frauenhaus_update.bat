@echo off
echo ===================================
echo Frauenhaus-Verwaltung UPDATE
echo ===================================
echo.
cd /d "%~dp0"

echo [1/3] Lade neue Version herunter...
docker compose pull
echo.

echo [2/3] Starte neue Version...
docker compose up -d
echo.

echo [3/3] Pruefe Status...
timeout /t 10 >nul
docker compose ps
echo.

echo ===================================
echo [OK] Update abgeschlossen!
echo ===================================
echo.
echo Browser aktualisieren (F5) um neue
echo Version zu sehen.
echo.
pause
