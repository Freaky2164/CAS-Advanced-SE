# 📋 ADMIN-HANDBUCH – Für den Administrator (keine IT-Vorkenntnisse nötig)

## 🎯 Deine Rolle als Admin

**Du bist nicht der IT-Support!**

Du bist der "Hauswart" der Anwendung:
- ✓ Täglich: Schauen, ob alles läuft
- ✓ Wöchentlich: Backups prüfen
- ✓ Monatlich: Performance monitoren
- ✓ Bei Problemen: Richtige Person anrufen

---

## 📅 WÖCHENTLICHES ADMIN-RITUAL (30 Minuten)

Jeden **Freitag um 16:00 Uhr**:

### Schritt 1: Dashboard öffnen (5 Min)

```
URL: https://admin.frauenhaus.local
Benutzername: admin
Passwort: [dein_admin_passwort]
```

### Schritt 2: Health Status prüfen (5 Min)

**Du siehst eine Seite wie diese:**

```
┌─────────────────────────────────────────────────┐
│ SYSTEM HEALTH STATUS                            │
├─────────────────────────────────────────────────┤
│                                                 │
│ Database:          🟢 HEALTHY                   │
│ Backup (Last):     🟢 SUCCESS (26.06.2024)      │
│ Storage:           🟢 OK (60% used)             │
│ API Response Time: 🟢 FAST (<200ms)             │
│ User Sessions:     🟢 NORMAL (4 active)         │
│                                                 │
│ ALERTS: Keine ✓                                 │
│                                                 │
│ Last Check: 2024-06-26 12:00 (2 hours ago)     │
│                                                 │
└─────────────────────────────────────────────────┘
```

**Was bedeuten die Farben?**

| Farbe | Bedeutung | Aktion |
|-------|-----------|--------|
| 🟢 Grün | Alles OK | Nichts tun |
| 🟡 Gelb | Warnung | Beobachten, nächste Woche nachschauen |
| 🔴 Rot | Problem | Sofort Aktion, IT anrufen wenn > 5 Min |

**Normale Ausgabe (Freitagvormittag):**
- ✓ Alle grün? → Super, Wochenende genießen!
- ⚠️ Ein Gelb? → Notiere es, nächste Woche weiterschauen
- ⚠️ Ein Rot? → Lese die Fehlermeldung, runterscrollen...

### Schritt 3: Backup-Status prüfen (10 Min)

**Gehe zu: Dashboard → Backups**

```
┌─────────────────────────────────────────────────┐
│ BACKUP STATUS (Letzte 14 Tage)                 │
├─────────────────────────────────────────────────┤
│                                                 │
│ 26.06.2024 (Do) 🟢 SUCCESS (840 MB)             │
│ 25.06.2024 (Mi) 🟢 SUCCESS (835 MB)             │
│ 24.06.2024 (Di) 🟢 SUCCESS (832 MB)             │
│ 23.06.2024 (Mo) 🟢 SUCCESS (828 MB)             │
│ 22.06.2024 (So) 🟢 SUCCESS (825 MB)             │
│ ...                                             │
│                                                 │
└─────────────────────────────────────────────────┘
```

**Checkliste:**

```
☑ Alle Backups von letzte 7 Tage grün?
  ☐ Ja → Perfekt!
  ☐ Nein → Welches ist rot? 
       Lese die Fehlermeldung:
       - "Disk full"? → IT muss aufräumen
       - "Network timeout"? → Netzwerk-Problem
       - "Permission denied"? → IT muss Fix
       
☑ Größe der Backups steigt langsam?
  ☐ Ja (200-250 MB täglich) → Normal
  ☐ Nein oder riesig (>2000 MB)? → Frage IT
  
☑ Recovery-Test erfolgreich letzte Woche?
  ☐ Ja (zeigt Datum) → Backups funktionieren!
  ☐ Nein oder kein Datum → IT sollte testen
```

### Schritt 4: User Management (5 Min)

**Gehe zu: Admin → Users**

```
┌─────────────────────────────────────────────────┐
│ BENUTZER ÜBERSICHT                             │
├──────────┬──────────────┬──────────┬────────────┤
│ Benutzer │ Letzter Login│ Status   │ Aktionen   │
├──────────┼──────────────┼──────────┼────────────┤
│ maria    │ 26.06 15:32  │ 🟢 Aktiv │ [...]      │
│ anna     │ 26.06 10:15  │ 🟢 Aktiv │ [...]      │
│ petra    │ 25.06 16:00  │ 🔵 Offline 1d│ [...]  │
│ dorle    │ 22.06 09:00  │ 🔵 Offline 4d│ [...]  │
│ josef    │ -- (neu)     │ 🟠 Pending   │ [...]  │
│                                                 │
│ + [Neuer Benutzer]  🗑️ [Löschen]               │
│                                                 │
└─────────────────────────────────────────────────┘
```

**Deine Aufgaben:**

```
☑ Sind alle Benutzer noch aktiv, die es sein sollten?
  - Maria, Anna, Petra sollten aktiv sein
  - Dorle: 4 Tage offline → Okay (Urlaub?)
  - Josef: Neu → Braucht Freigabe?

☑ Neue Benutzer hinzufügen?
  - Kandidat stellen sich vor
  - Klick: "+ Neuer Benutzer"
  - Formular ausfüllen:
    * Name: _______________
    * Email: _______________
    * Rolle: [Sachbearbeiter ▼]
    * [Speichern]
  - System generiert Passwort & sendet Email
  - Benutzer soll sich anmelden & Passwort ändern

☑ Benutzer sollte gehen?
  - Klick auf Benutzer
  - [Delete] Button
  - WARNUNG lesen:
    "Dieser Benutzer kann sich nicht mehr anmelden.
     Daten bleiben erhalten. Sicher?"
  - [Ja, Löschen]
```

### Schritt 5: Notizen machen

**Ausfüllen:**

```
ADMIN WOCHENREPORT KW 26 (24-30 Juni 2024)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Datum Prüfung: 28.06.2024
Prüfer: Anna Mueller

SYSTEM STATUS:
  ☐ Alles grün
  ☐ Nachricht im System
  ☐ Probleme beobachtet

Falls Probleme:
  Problem 1: ____________________________
  Status: ☐ Gelöst  ☐ In Arbeit  ☐ Wartet auf IT
  Notiz: _________________________________
  
  Problem 2: ____________________________
  ...

BACKUPS:
  ☐ Alle erfolgreich (7 Tage)
  ☐ Eins oder mehr fehlgeschlagen
      Welche? _____________________________

USER MANAGEMENT:
  ☐ Neuer Benutzer? Ja / Nein    Wer? ________
  ☐ Benutzer gelöscht? Ja / Nein Wer? ________
  ☐ Passwort-Resets nötig? Ja / Nein Wer? ____

NÄCHSTE WOCHE:
  - [ ] IT kontaktieren wegen: ______________
  - [ ] Besprechung mit Team? Falls ja: Termin ____
  - [ ] Sonstiges: _________________________

UNTERSCHRIFT: _______________________
```

---

## 🚨 NOTFALL-SZENARIEN

### Szenario 1: "Die App geht nicht"

**Dein Ablauf (max. 10 Minuten):**

```
1. Dashboard öffnen → geladen?
   Ja → Gehe zu Schritt 2
   Nein → Browser neu starten → Nochmal versuchen
       Falls nochmal Fehler → Schritt 3

2. Dashboard öffnet → Health Status?
   Alles grün → Fehler ist client-seitig
      → Sag Benutzer: Browser Cache löschen
      → Falls immer noch: Benutzer PC neu starten
   Etwas rot → Lies die Fehlermeldung!
      → Falls "Disk full" → Aufräumen nötig
      → Falls "Database connection" → IT anrufen!

3. Dashboard geht nicht auf
   → Server wahrscheinlich OFFLINE
   → Schnell handeln!
      a) Prüfe deine Internet-Verbindung (Google.com)
      b) Frage Kollegen: "Habt ihr auch Problem?"
      c) IT SOFORT anrufen: "Server ist offline!"
```

| Aktion | Zeit | Wer macht's |
|--------|------|-----------|
| Browser neu starten | 1 Min | Benutzer |
| Dashboard prüfen | 2 Min | Admin |
| IT anrufen | sofort | Admin |

### Szenario 2: "Backup fehlgeschlagen"

**Das passiert manchmal, ist aber normalerweise unkritisch!**

```
1. Finde das fehlerhafte Backup im Dashboard
2. Lese die Fehlermeldung (sehr wichtig!)
3. Handle nach Fehlertyp:

   Fehler: "Disk full - No space left"
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   → Storage-Problem
   → Admin kann handeln:
      1. Gehe zu: Admin → System → Storage
      2. Schaue was Platz verbraucht
      3. Alte Reports löschen? (Admin darf)
         Admin → Maintenance → Clear Reports > 6 months old
      4. Falls nicht hilft → IT muss aufräumen
   
   Fehler: "Network timeout - NAS unreachable"
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   → Netzwerk-Problem
   → Admin kann NICHT handeln
   → IT MUSS anrufen: "Backup-NAS erreichbar?"
   
   Fehler: "Permission denied on backup location"
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   → Berechtigungs-Problem
   → IT MUSS anrufen: "Backup-Service-Konto hat keine Rechte"
```

**Wann ist es ein Notfall?**

```
NOTFALL (sofort IT anrufen):
━━━━━━━━━━━━━━━━━━━━━━━━━
☑ 3 oder mehr Backups hintereinander fehlgeschlagen
☑ Fehler seit > 2 Tagen
☑ Fehlermeldung enthält "CRITICAL"

KEIN NOTFALL (bis nächste Woche warten):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
☑ 1 Backup fehlgeschlagen, nächstes OK
☑ Fehler sind "Disk full" (einfach zu fixen)
☑ Nächstes Backup lädt in der neuen Wende neu
```

### Szenario 3: "Performance ist langsam"

```
Benutzer beschwert sich: "Die App ist mega langsam!"

1. Frage nach:
   - Alle Benutzer betroffen oder nur ein Benutzer?
   - Seit wann?
   - Benutzer PC oder App?

2. Prüfe im Dashboard:
   Admin → Performance → API Response Time
   Grafik der letzten 24 Stunden
   
   🟢 < 200ms: Alles OK (nicht die App)
   🟡 200-500ms: Performance-Abfall (wird überwacht)
   🔴 > 500ms: Langsam, IT sollte schauen

3. Wenn 🟢: Problem ist beim Benutzer
   → Browser tab hat zu viele andere offen?
   → PC hat zu viele Programme offen?
   → Datenverbindung langsam?
   → Browser-Cache voll?
   → Lösungsverschlag: Browser neu starten, PC neu starten

4. Wenn 🟡 oder 🔴: Problem ist im System
   → IT anrufen: "Performance über 500ms, bitte schauen"
```

---

## 💡 Die 3 NOTFALL-TELEFONNUMMERN

Drucke diese aus und klebe an den Monitor!

```
┌─────────────────────────────────────────┐
│        🚨 NOTFALL-HOTLINE 🚨            │
├─────────────────────────────────────────┤
│                                         │
│ 1. IT-SUPPORT (Allgemein)              │
│    Tel: +49 XXXX YYYYY                 │
│    Mail: support@...                   │
│    Öffnungszeiten: Mo-Fr 08:00-17:00   │
│                                         │
│ 2. ADMIN (Schlussel)                   │
│    Tel: +49 XXXX ADMIN                 │
│    (Nur nach Stunden)                  │
│                                         │
│ 3. GESCHÄFTSFÜHRER (Super-Notfall)    │
│    Im Handy-Buch                       │
│    (Nur wenn alles brennt!)            │
│                                         │
└─────────────────────────────────────────┘
```

---

## 🎓 ADMIN SCHULUNG (30 Minuten)

**Diese Punkte sollte jeder Admin verstehen:**

### 1. Das System (Was ist was?)

```
Frontend:  Das, was der Nutzer sieht (Browser)
Backend:   Das, was im Hintergrund läuft (Server)
Database:  Wo die Daten gespeichert sind
Backup:    Kopie der Datenbank, falls was kaputt geht
Logging:   Aufzeichnung was im System passiert
```

### 2. Die Health-Farben

```
🟢 Grün:  Alles OK, normal arbeiter
🟡 Gelb:  Warnung, beobachten aber nicht kritisch
🔴 Rot:   Problem, sofort handeln!
```

### 3. Backup-Parameter

```
Full Backup:     Komplette Kopie der Datenbank (täglich)
Incremental:     Nur was neu ist (6-stündlich)
Point-in-Time:   Kann zu jedem Zeitpunkt zurück (stündlich)
Offsite:         Kopie an anderem Ort (Sicherheit!)
Recovery Test:   Regelmäßig testen ob Restore funktioniert
```

### 4. Security (Sicherheit)

```
Niemals:
  ❌ Passwörter weitergeben
  ❌ Admin-Zugang mit anderen teilen
  ❌ Benutzer-Daten an Privat-Email
  ❌ "Schreib mal dein Passwort" sagen

Immer:
  ✓ Benutzer selbst Password-Reset durchführen
  ✓ Neue Benutzer persönlich Passwort geben
  ✓ Passwörter regelmäßig ändern (empfohlen: 3 Monate)
  ✓ Verdächtige Aktivität melden
```

---

## 📝 Kurzreferenz (Druck diesen Zettel!)

```
┌─────────────────────────────────────────────────────┐
│ ADMIN KURZREFERENZ - Am Monitor kleben!           │
├─────────────────────────────────────────────────────┤
│                                                     │
│ TÄGLICH (Nutzer):                                  │
│  ☐ Login test                                      │
│  ☐ Keine roten Fehler-Nachrichten                 │
│                                                     │
│ WÖCHENTLICH (Admin, Freitag):                     │
│  ☐ Dashboard öffnen                                │
│  ☐ Health Status: Alles grün?                     │
│  ☐ Backups letzte 7 Tage: Alle grün?             │
│  ☐ Neue Benutzer? Benutzer gelöscht?            │
│  ☐ Wochenreport ausfüllen                         │
│                                                     │
│ NOTFALL-HOTLINE:                                   │
│  Tel: +49 XXXX YYYY                                │
│  Email für Tickets: support@                       │
│                                                     │
│ WORAUF MUSST DU ACHTEN:                           │
│  🔴 RED in Dashboard → IT anrufen                 │
│  ⚠️  BACKUP fehlgeschlagen > 2 Tage              │
│  ⚠️  "Disk full" Fehler → Aufräumen             │
│  ⚠️  Viele Fehler in Logs → Trend?              │
│                                                     │
│ ADMIN-DASHBOARD:                                   │
│  Öffne: https://admin.frauenhaus.local           │
│  Username: admin                                   │
│  Password: [dein Admin-Passwort]                  │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

**Das wars! Einfach, oder?** 😊

Wenn du diese Checklisten befolgst, läuft die App 99.9% der Zeit smooth!
