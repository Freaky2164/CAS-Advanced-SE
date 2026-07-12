# 🔧 Fehlerbehandlung – Adobe Acrobat ReaderPrefs Error

## Problem
```
Exception in thread "main" java.lang.Error: Unresolved compilation problems:
        ReaderPrefs cannot be resolved
        at compucrash.CStart.main(CStart.java:17)
```

## Ursache

Die alte Anwendung versucht, Adobe Acrobat Reader Preferences zu initialisieren. Diese Klasse ist optional (es gibt einen try-catch Block), aber:

1. Das Adobe SDK JAR (`acrobat.jar`) ist auf dem System nicht korrekt zugänglich
2. Die `.class`-Dateien wurden mit fehlender Abhängigkeit kompiliert

## Lösung (3 Schritte)

### Schritt 1: Java Development Kit (JDK) installieren

❌ Du hast nur **Java Runtime Environment (JRE)** – brauchen wir **Java Development Kit (JDK)**

**Download:**
- Gehe zu: https://adoptium.net/download/
- Wähle: **JDK (nicht JRE)**
- Version: **Java 17 LTS** (oder neuer)
- Installiere

**Überprüfe Installation:**
```cmd
javac -version
```

Sollte etwas ausgeben wie:
```
javac 17.0.x
```

Wenn nicht → Installation wiederholen

---

### Schritt 2: Quellen neu kompilieren

Die Fehler sind in den `.class`-Dateien "eingebacken". Wir müssen die `.java`-Quellen neu kompilieren.

**öffne Command Prompt:**
```cmd
cd "C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z"
compile.bat
```

**Erwartete Ausgabe:**
```
============================================================
[KOMPILIEREN] Frauenhaus Java-Quellen
============================================================

[INFO] javac gefunden
javac 17.0.x

[INFO] Kompiliere compucrash-Package...
[SUCCESS] compucrash kompiliert

[INFO] Kompiliere frauenhaus-Package...
[SUCCESS] Kompilierung abgeschlossen!

Du kannst die App jetzt starten mit:
   run_local.bat
```

---

### Schritt 3: Anwendung starten

```cmd
run_local.bat
```

Falls **immer noch Fehler**: → Versuche Schritt 2 erneut

---

## Alternative: Wenn compile.bat nicht funktioniert

Du kannst auch **manuell kompilieren**:

```cmd
cd "C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z"

REM Kompiliere compucrash-Package
javac -encoding UTF-8 -d . compucrash\*.java

REM Kompiliere frauenhaus-Package
javac -encoding UTF-8 -d . frauenhaus\*.java
```

---

## Häufige Fehler

| Fehler | Lösung |
|--------|--------|
| `javac: command not found` | JDK nicht installiert – [hier runterladen](https://adoptium.net/download/) |
| `cannot find symbol` in `.java` | Fehlende JAR-Dateien – alle `.jar` in `ext/` müssen erreichbar sein |
| `[WARNUNG] frauenhaus-Kompilierung fehlgeschlagen` | Nicht kritisch – ignorieren |
| **Fehler bleibt nach compile.bat** | Alle `.class`-Dateien löschen und erneut versuchen |

---

## ✅ Was wir gemacht haben

**In `CStart.java`:**
- ❌ Entfernt: `import com.adobe.acrobat.gui.ReaderPrefs;`
- ❌ Auskommentiert: `ReaderPrefs.initialize();` und verwandte Zeilen

Diese Adobe-Zeilen waren **optional** (versuchten nur, Adobe Reader vorzukonfigurieren). Ohne sie funktioniert die App genauso.

---

## 🚀 Jetzt neu starten

```cmd
cd "C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z"
compile.bat
run_local.bat
```

**Erfolg**, wenn:
1. ✓ Splash Screen erscheint (5 Sekunden)
2. ✓ Login-Fenster öffnet sich
3. ✓ Login mit `dorle` möglich

---

## 💡 Faustregel

Wenn Java-Fehler nach Code-Änderungen auftreten:
1. Starte `compile.bat` (rekompiliert `.java` → `.class`)
2. Starte App mit `run_local.bat`

Fertig! 🎉
