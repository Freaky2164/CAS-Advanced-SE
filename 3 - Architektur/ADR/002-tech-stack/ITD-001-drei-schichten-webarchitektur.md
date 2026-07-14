# Ersetze den 2-Schichten-Fat-Client durch eine 3-Schichten-Webarchitektur (SPA → REST-Backend → PostgreSQL)

## Das Problem

Die Software ist eine JavaSwing Anwendung, die auf jedem Arbeitsplatz installiert werden muss und direkt auf die Datenbank zugreift und so nicht den Anforderungen an Sicherheit entspricht.

## Berücksichtigte Optionen

- Modernisierte Desktop-App
- **3-Schichten-Architektur: Webapp, REST-Backend, Datenbank**

## Begründung

Die Modularität erfüllt die Anforderungen an die Wart- und Erweiterbarkeit an die Softwarearchitektur. Zudem können durch eine Umstellung auf ein Dreischichtenmodell Sicherheitsbedenken reduziert werden, da das Frontend nicht direkt auf die Datenbank zugreifen kann.

## Anmerkungen

-