# ADR-003: Technologie-Stack Frontend – Vaadin (Full-Stack Java)

## Status

**Akzeptiert** – Juli 2026

## Kontext

Es soll eine Modernisierung der grafischen Benutzeroberfläche (GUI) der bisherigen
Desktop-Verwaltungssoftware für Spenden erreicht werden. Anhand einer Benutzeranalyse sowie
einer Analyse des Funktionsumfangs wird ein Framework gewählt, welches die benötigten
Funktionen bestmöglich unterstützt und gleichzeitig eine geringe Komplexität sowie eine hohe
Wartbarkeit gewährleistet.

Bei der zuvor vorliegenden Anwendung handelt es sich um eine klassische Verwaltungssoftware,
deren Schwerpunkt auf Formularen, tabellarischen Daten, einer Benutzerverwaltung sowie der
Bearbeitung von Geschäftsdaten liegt. Im Gegensatz zu Maschinenbedienoberflächen und
Echtzeitanwendungen stehen hierbei eine hohe Wartbarkeit, eine einfache Entwicklung und eine
konsistente Benutzerführung im Vordergrund. Hohe Flexibilität, schnelle Reaktionszeiten und
hohe Übertragungsraten rücken in den Hintergrund.[1]

Auf eine erneute Desktopapplikation wurde verzichtet, um eine Integration auf unterschiedlichen
Endgeräten zu vereinfachen und Fehlerquellen durch fehlende Pakete oder Abhängigkeiten zu
minimieren. Für die Auswahl wurden vier webbasierte Frameworks betrachtet:

- Vaadin
- Angular
- React
- Next.js

Die Bewertung erfolgte anhand folgender Kriterien:

| Anforderung | Beschreibung |
|-------------|-------------|
| **Wiederverwendbarkeit** | Wiederverwendung von Services, Business-Logik, Datenzugriff, Domain-Modellen und Security der bestehenden Java-Desktopanwendung |
| **Entwicklungsaufwand** | Möglichst geringer Aufwand durch fertige UI-Komponenten (Formulare, Tabellen, Dialoge) |
| **Wartbarkeit** | Wenige Schnittstellen, geringe Komplexität, konsistente Benutzerführung |
| **Sicherheitsintegration** | Einfache Anbindung an das bestehende Spring-Boot-Backend (ADR-002) inkl. Spring Security |
| **Verfügbarkeit fertiger UI-Komponenten** | "Out of the box" verfügbare Komponenten statt Integration zusätzlicher Bibliotheken |

Diese Kriterien orientieren sich an den Anforderungen moderner Verwaltungssoftware und den
Qualitätsmerkmalen wartbarer Softwaresysteme (ISO/IEC 25010:2023).[2]

## Entscheidung

Wir entscheiden uns für **Vaadin** (Full-Stack Java Framework) als Frontend-Technologie. Die
Vaadin Web UI wird direkt gegen das bestehende Spring-Boot-Backend (ADR-002) integriert und
ersetzt die bisherige Desktop-GUI vollständig durch eine Web-Anwendung.

## Architekturvergleich

| Framework | UI-Schicht | Kommunikation | Backend | Datenbank |
|-----------|-----------|----------------|---------|-----------|
| **Vaadin** (Full-Stack Java) | Vaadin Components | Direkter Java-Methodenaufruf (kein REST notwendig) | Spring Boot (integriert) | PostgreSQL |
| Angular (TypeScript) | Angular Components | REST API | Spring Boot | PostgreSQL |
| React (JavaScript/TS) | React Components | REST API | Spring Boot | PostgreSQL |
| Next.js (React + SSR) | Next.js UI (SSR/SSG) | API Routes / REST API | Spring Boot | PostgreSQL |

Vaadin zeichnet sich durch eine enge Integration in das Java-Backend aus: weniger Schnittstellen,
weniger Komplexität und eine mögliche Wiederverwendbarkeit bestehender Codebasis.

## Betrachtete Alternativen

### Alternative A: Angular (TypeScript)

Angular wurde insbesondere für umfangreiche Single-Page-Anwendungen mit einer klaren Trennung
zwischen Frontend und Backend entwickelt. Dadurch können Frontend und Backend getrennt
voneinander entwickelt werden. Im Vergleich zu Vaadin erhöht dies jedoch den Entwicklungs- und
Wartungsaufwand, da beide Anwendungen über REST-Schnittstellen miteinander kommunizieren
müssen.[5]

**Bewertung**: ❌ Höherer Entwicklungsaufwand durch Trennung von Frontend/Backend, kein
Zugriff auf bestehende Java-Geschäftslogik ohne REST-Schicht.

### Alternative B: React (JavaScript/TypeScript)

React bietet die höchste Flexibilität der betrachteten Frameworks. Dabei müssen jedoch viele
Funktionen wie Routing, Formulare oder Tabellen über zusätzliche Bibliotheken integriert werden
und können – analog zu Angular – nicht wie in Vaadin direkt "out of the box" genutzt werden.
Außerdem entsteht durch die Trennung von Frontend und Backend ebenfalls ein höherer
Entwicklungsaufwand.

**Bewertung**: ❌ Zusätzliche Bibliotheken für Routing, Formulare und State-Management
erforderlich, höherer Integrationsaufwand.

### Alternative C: Next.js (React + SSR)

Next.js kann als eine Erweiterung von React verstanden werden. Es erweitert React um Funktionen
wie serverseitiges Rendering (SSR) und Routing. Diese Vorteile spielen vor allem bei öffentlichen
Webanwendungen eine Rolle, wo eine hohe Flexibilität bei Routenänderungen durch das
Zusammenspiel mehrerer Komponenten erforderlich ist. Für eine interne Verwaltungssoftware
bietet dies nur einen geringen Mehrwert, da nach dem Deployment eine Änderung der Route
selten ist.[6]

**Bewertung**: ⚠️ Höherer Funktionsumfang (SSR/SSO, Routing), aber kaum Mehrwert für eine
interne Verwaltungsanwendung; zusätzliche Komplexität nicht gerechtfertigt.

### Alternative D: Vaadin (Full-Stack Java) – gewählt ✅

Vaadin wurde gezielt für Business- und Verwaltungsanwendungen entwickelt. Anders als die
anderen Frameworks benötigt Vaadin keinen separaten Webserver bzw. keine eigenständige
Frontend-Anwendung. Dies schränkt die Trennung von Backend und Frontend etwas ein, wird
jedoch durch zahlreiche Komponenten kompensiert, die Vaadin bereits "out of the box" anbietet –
etwa Formulare, Tabellen, Dialoge und Navigationselemente.[3] Dies minimiert den
Entwicklungsaufwand deutlich.[4]

Durch die vollständige Java-Integration ist es zudem möglich, Funktionen und Klassen aus dem
alten Softwarestand teilweise direkt zu übernehmen.

**Bewertung**: ✅ Beste Kombination aus Produktivität, Wartbarkeit und einfacher Integration in
das bestehende Java-Backend.

## Code-Beispiel: Formular

**Vaadin (Java)**

```java
VerticalLayout layout = new VerticalLayout();
TextField name = new TextField("Name");
TextField email = new TextField("Email");
Button save = new Button("Save", e -> save());
layout.add(name, email, save);
```

**Angular (TypeScript)**

```typescript
// component.ts
form = this.fb.group({
  name: [''],
  email: ['']
});
```
```html
<!-- template.html -->
<form [formGroup]="form">
  <input formControlName="name" />
  <input formControlName="email" />
  <button (click)="save()" type="submit">Save</button>
</form>
```

**React (TypeScript)**

```tsx
const [form, setForm] = useState({ name: "", email: "" });

return (
  <form onSubmit={save}>
    <input value={form.name} onChange={...} />
    <input value={form.email} onChange={...} />
    <button type="submit">Save</button>
  </form>
);
```

**Next.js (TypeScript)**

```tsx
"use client";
const [form, setForm] = useState({ name: "", email: "" });

return (
  <form onSubmit={save}>
    <input value={form.name} onChange={...} />
    <input value={form.email} onChange={...} />
    <button type="submit">Save</button>
  </form>
);
```

Das Vaadin-Beispiel zeigt deutlich weniger Boilerplate-Code: Es ist keine separate
Template-Datei, kein State-Management und keine explizite Formularbindung notwendig, da
Komponenten direkt in Java instanziiert und miteinander verknüpft werden.

## Begründung

### 1. Vollständige Java-Integration und Wiederverwendbarkeit

Ein erheblicher Vorteil von Vaadin ist die Möglichkeit zur Wiederverwendung von Programmelementen
der bereits vorhandenen Desktopanwendung. Vorhandene Klassen, Services und Geschäftslogik
können integriert werden und müssen nicht in eine andere Programmiersprache überführt oder über
eine zusätzliche REST-Schnittstelle weitergeleitet werden. Dies betrifft insbesondere:

- Services / Business-Logik
- Datenzugriff
- Domain-Modelle
- Validierungen
- Security (Rollen, Rechte)
- Konfigurationen
- weitere nützliche Funktionen oder Klassen

### 2. Geringer Boilerplate-Code und umfangreiche UI-Komponenten "out of the box"

Vaadin stellt bereits zahlreiche Komponenten für Formulare, Tabellen, Dialoge und
Benutzerverwaltung bereit, welche sich direkt integrieren lassen. Diese direkte Integration führt
zu einer deutlichen Zeitersparnis, da viele Funktionen ohne zusätzliche Bibliotheken oder das
Verändern von Konfigurationsdateien umgesetzt werden können – im Gegensatz zu Angular und
React, wo Routing, Formulare und Tabellen über zusätzliche Bibliotheken nachgerüstet werden
müssen.

### 3. Einfache Sicherheitsintegration mit Spring Security

Eine integrierte Unterstützung für Spring Security, wodurch Authentifizierung und rollenbasierte
Zugriffssteuerungen mit geringem Aufwand realisiert werden können, bildet ein weiteres
Entscheidungskriterium für Vaadin. Dies fügt sich nahtlos in die bereits für das Backend
getroffene Entscheidung (ADR-002, Spring Boot) ein.

### 4. Geringere Komplexität gegenüber Angular, React und Next.js

Da Vaadin UI-Komponenten direkt im Java-Backend rendert, entfällt die Notwendigkeit einer
separaten REST-Schnittstelle zwischen Frontend und Backend für UI-Zustände. Dies reduziert die
Anzahl der Schnittstellen, den Kommunikationsaufwand sowie potenzielle Fehlerquellen (z. B.
inkonsistente DTOs, CORS-Konfiguration, doppelte Validierungslogik in Frontend und Backend).

## Integration in die bestehende Softwarearchitektur

| Bisherige Architektur (Desktop) | Neue Architektur mit Vaadin (Web) |
|---|---|
| Desktop GUI (aktuelle Client-Anwendung) | Vaadin Web UI (Grid, Form, Dialog, …) |
| Geschäftslogik (Services, Manager, fachliche Regeln) | Spring Boot (interne REST API, Services, Logiken, Spring Security) |
| Datenbank (Datenspeicherung) | Datenbank (Datenspeicherung, PostgreSQL gemäß ADR-002) |

Die Geschäftslogik-Schicht sowie die Datenbankanbindung bleiben strukturell erhalten; lediglich
die Präsentationsschicht wird von der Desktop-GUI auf Vaadin Web UI migriert. Services,
Datenzugriff, Domain-Modelle, Validierungen, Security sowie Konfigurationen aus der bestehenden
Architektur werden wiederverwendet.

## Konsequenzen

### Positiv
- Vollständige Integration in das bestehende Java/Spring-Boot-Backend – keine zusätzliche
  REST-Schicht für die UI notwendig
- Wiederverwendung bestehender Geschäftslogik, Datenzugriffsschicht und Domain-Modelle
- Geringer Boilerplate-Code und schnellere Entwicklung durch fertige UI-Komponenten
  (Formulare, Tabellen, Dialoge, Navigation) "out of the box"
- Einfache und konsistente Sicherheitsintegration über Spring Security (Authentifizierung,
  rollenbasierte Zugriffssteuerung)
- Geringere Komplexität durch Wegfall einer separaten Frontend-Backend-Schnittstelle
- Web-basierte Bereitstellung vereinfacht Zugriff über verschiedene Endgeräte ohne
  Installation zusätzlicher Pakete oder Abhängigkeiten

### Negativ
- Keine strikte Trennung von Frontend und Backend – Vaadin benötigt keinen separaten
  Webserver, was die Architektur enger an Java bindet
- Geringere gestalterische Flexibilität im Vergleich zu React/Next.js, da UI-Komponenten
  serverseitig in Java definiert werden
- Team benötigt Einarbeitung in die Vaadin-spezifische Komponenten-API

### Neutral
- Backend-Technologie ist bereits mit Spring Boot / Java 21 festgelegt (ADR-002); Vaadin baut
  direkt darauf auf
- Serverseitiges Rendering (SSR) und clientseitiges Routing, wie bei Next.js, sind für eine
  interne Verwaltungssoftware nicht erforderlich, da öffentliche Erreichbarkeit und SEO keine
  Rolle spielen

## Quellen

[1] „Response Time Limits: Article by Jakob Nielsen“, Nielsen Norman Group. Verfügbar unter:
https://www.nngroup.com/articles/response-times-3-important-limits/

[2] „ISO/IEC 25010:2023 - Digital | PDF (EN) - VDE VERLAG“. Verfügbar unter:
https://www.vde-verlag.de/p/normen/iso-iec-25010-2023/252317-EN-PDF

[3] „How to use Components | Vaadin components“. Verfügbar unter:
https://vaadin.com/docs/latest/components

[4] „Build Stunning Java Web Applications with Ease | Vaadin“. Verfügbar unter:
https://vaadin.com/benefits

[5] A. Team, „What is Angular? • Angular“. Verfügbar unter: https://angular.dev/

[6] „Next.js Docs | Next.js“. Verfügbar unter: https://nextjs.org/docs
