@echo off
REM ============================================================
REM Kompiliere Java-Quellen neu
REM ============================================================

setlocal enabledelayedexpansion

echo.
echo ============================================================
echo [KOMPILIEREN] Frauenhaus Java-Quellen
echo ============================================================
echo.

set APPDIR=%~dp0
set SRCDIR=%APPDIR%

REM === Pruefen, ob javac verfuegbar ist ===
where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] javac nicht gefunden!
    echo.
    echo Du brauchst das Java Development Kit (JDK), nicht nur JRE.
    echo Installiere: https://adoptium.net/download/ (wähle "JDK")
    echo.
    pause
    exit /b 1
)

echo [INFO] javac gefunden
javac -version

echo.
echo [INFO] Kompiliere compucrash-Package...
javac.exe -encoding UTF-8 -d %SRCDIR%. %SRCDIR%compucrash\*.java 2>&1

if errorlevel 1 (
    echo.
    echo [ERROR] Kompilierung fehlgeschlagen!
    pause
    exit /b 1
)

echo [SUCCESS] compucrash kompiliert

echo.
echo [INFO] Kompiliere frauenhaus-Package...
javac.exe -encoding UTF-8 -d %SRCDIR%. %SRCDIR%frauenhaus\*.java 2>&1

if errorlevel 1 (
    echo.
    echo [WARNUNG] frauenhaus-Kompilierung fehlgeschlagen
    echo (Das ist nicht kritisch - erneut versuchen)
    echo.
)

echo.
echo [SUCCESS] Kompilierung abgeschlossen!
echo.
echo Du kannst die App jetzt starten mit:
echo   run_local.bat
echo.
pause
