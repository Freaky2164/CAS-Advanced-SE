# Requirements

## 1. Zielumgebung & Einschränkungen
* **Betriebssystem:** Microsoft Windows.
* **Compliance:** Einhaltung der **DSGVO** bezüglich der Verarbeitung von personenbezogenen Daten.

---

## 2. Qualitätsmerkmale (in Anlehnung an ISO/IEC 25010)

Die Systemarchitektur und -implementierung muss die folgenden Qualitätsmerkmale erfüllen, unterteilt nach den jeweiligen Stakeholder-Rollen:

### 2.1 Qualitätsmerkmale aus Sicht Benutzer
* **Funktionale Eignung (Korrektheit):** Das System muss Spenden korrekt und zuverlässig speichern. Reports müssen korrekt erzeugt werden.
* **Benutzerfreundlichkeit (Usability):** Die Schnittstellen zur Eingabe von Spenden, zur Erstellung von Berichten sowie zur Datenansicht müssen auch für Benutzer mit nicht-technischem Hintergrund intuitiv bedienbar sein.
* **Sicherheit:** Benutzerdaten müssen nach DSGVO verarbeitet werden. 
* **Wartbarkeit (Maintainability)** (*Sekundäre Priorität für Endbenutzer*)

### 2.2 Qualitätsmerkmale aus Sicht Administrator
* **Zuverlässigkeit (Reliability):** Das System muss eine hohe Verfügbarkeit und Fehlertoleranz gewährleisten.
* **Sicherheit:** Die Einhaltung der DSGVO muss sichergestellt werden.
* **Wartbarkeit:** Das System muss einfach wartbar sein um den Administrationsaufwand zu minimieren.

### 2.3 Hard Requirements
Aus den zuvor festgelegten notwendigen Qualitätsmerkmalen der Stakeholder-Rollen folgen diese vier Säulen, die die architektonischen Rahmenbedingungen bilden:
1. **Funktionale Eignung (Korrektheit)**
2. **Zuverlässigkeit (Reliability)**
3. **Wartbarkeit (Maintainability)**
4. **Security (DSGVO)**

---

## 3. Funktionale Anforderungen

### FR-1: Benutzerverwaltung

#### FR-1.1: Benutzeranmeldung
Das System muss eine Authentifizierung mittels Benutzername und Passwort ermöglichen.

#### FR-1.2: Rollenverwaltung
Das System muss die unterschiedlichen Benutzerrollen Administrator und Sachbearbeiter unterstützen. Berechtigungen müssen rollenbasiert vergeben werden.

### FR-2: Mitglieder- und Adressverwaltung
Das System muss Stammdaten von Mitgliedern verwalten.

Der Benutzer muss Mitglieder

* anlegen
* bearbeiten
* löschen
* suchen
* filtern

können.

Zu speichern sind mindestens:

* Anrede
* Name
* Vorname
* Adresse
* Kontaktdaten
* Mitgliedsnummer

### FR-3: Verwaltung von Spenden
Das System muss Geldspenden erfassen.

Eine Spende besitzt:

* Spendendatum
* Spender
* Betrag
* Spendenart
* Verwendungszweck
* Verein
* Bemerkung

Das System muss Spenden

* anlegen
* ändern
* löschen
* anzeigen

können.

### FR-4: Verwaltung von Bußgeldern
Das System muss Bußgelder verwalten.

Ein Bußgeld besitzt mindestens:

* Datum
* Gericht
* Aktenzeichen
* Name
* Vorname
* Betrag
* Verein
* Status

Der Status eines Bußegelds muss mindestens enthalten:

* offen
* teilweise bezahlt
* bezahlt

### FR-5: Verwaltung von Zahlungseingängen
Zu jedem Bußgeld müssen beliebig viele Zahlungseingänge gespeichert werden.

Ein Zahlungseingang besteht aus

* Zahlungsdatum
* Betrag

Das System muss

* den eingegangenen Gesamtbetrag berechnen
* den Restbetrag automatisch berechnen

### FR-6: Gerichtsverwaltung
Gerichte müssen administrierbar sein.

Gerichte müssen

* angelegt
* bearbeitet
* gelöscht
* gesucht

werden können.

### FR-7: Verwaltung von Stammdaten
Administratoren müssen:

* Spendenarten
* Anreden
* Bußgeldstatus
* Vereine
* Gerichte

verwalten können.

### FR-8: Auswertungen
Das System muss Auswertungen erzeugen können.

Dazu gehören mindestens:

* Spendenauswertung
* Bußgeldauswertung
* Vereinsstatistiken

### FR-9: Ausstellung von Spendenbescheinigungen
Das System muss Spendenbescheinigungen automatisiert nach den aktuell geltenden gesetzlichen Vorgaben ausstellen können.

Zu den Arten der Spendenbescheinigungen gehören mindestens:
* Dauerspende
* dauer Geldspende
* einmalige Geldspende
* Mitgliedsbeitrag
* Sachspende

Dies muss für jeden Verein möglich sein.

Dazu müssen die definierten Vorlagen mit den notwendigen Daten ausgefüllt und generiert werden können.

### FR-10: Serienbriefe
Das System muss Serienbriefe erzeugen auf Grundlage von

* Kontaktdaten der Mitglieder
* Briefanrede
* Verein, in dem eine Person Mitglied ist

Dazu muss eine Anbindung an Outlook gegeben sein, die ein direktes Versenden von Serienbriefen ermöglicht.

### FR-11: Validierung
Pflichtfelder dürfen nicht leer sein und müssen einen gültigen Wer enthalten.

### FR-12: Datenkonsistenz
Das System darf keine inkonsistenten Daten zulassen.


---

## 4. Nicht-funktionale Anforderungen

* **NFR-1 Sicherheit/DSGVO:** Alle personenbezogenen Daten müssen gemäß DGVO verarbeitet werden.
* **NFR-2 Zuverlässigkeit:** Das System muss stabil sein, um den Wartungsaufwand gering zu halten.
* **NFR-3 Modularer Aufbau:** Das System muss modular aufgebaut sein, um eine einfache Wartbarkeit zu gewährleisten.
* **NFR-4 Backup & Recovery:** Daten müssen vollumfänglich und automatisiert gesichert werden und wiederherstellbar sein. Sicherungen müssen sicher und verschlüsselt sowie unter Einhaltung der DSGVO-Aufbewahrungsfristen abgelegt werden.
* **NFR-4 Bedienbarkeit:** Das System muss für Personen mit nicht-technischem Hintergrund bedienbar sein.

tbc