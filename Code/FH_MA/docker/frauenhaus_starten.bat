@echo off
echo ===================================
echo Frauenhaus-Verwaltung STARTEN
echo ===================================
echo.
cd /d "%~dp0"

REM Pruefe ob Docker laeuft
docker info >nul 2>nul
if errorlevel 1 (
    echo [FEHLER] Docker laeuft nicht!
    echo.
    echo Bitte starte Docker Desktop zuerst:
    echo   - Suche "Docker Desktop" im Startmenue
    echo   - Starte die Anwendung
    echo   - Warte bis das Docker-Symbol unten rechts erscheint
    echo   - Dann dieses Script erneut ausfuehren
    echo.
    pause
    exit /b 1
)

echo [OK] Docker laeuft
echo [INFO] Starte Frauenhaus-Verwaltung...
echo.

docker compose up -d

echo.
echo ===================================
echo [ERFOLGREICH] Anwendung gestartet!
echo ===================================
echo.
echo Oeffne deinen Browser und gehe zu:
echo.
echo   https://localhost
echo.
echo (Falls eine Sicherheitswarnung kommt:
echo  "Erweitert" klicken, dann "Weiter")
echo.
echo ===================================
pause
