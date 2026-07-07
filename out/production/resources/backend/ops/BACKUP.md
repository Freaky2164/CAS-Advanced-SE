# Backup & Recovery

Strategie: pgBackRest mit WAL-Archivierung → Point-in-Time-Recovery (PITR).
Die Postgres-Instanz archiviert jedes WAL-Segment (`archive_command`, siehe docker-compose.yml), pgBackRest hält verschlüsselte (AES-256), komprimierte Backups.

## Zeitplan (Cron auf dem DB-Host bzw. im Sidecar)

    # Vollbackup sonntags 02:00, differenzielles Backup täglich 02:00
    0 2 * * 0    pgbackrest --stanza=frauenhaus --type=full backup
    0 2 * * 1-6  pgbackrest --stanza=frauenhaus --type=diff backup

Retention (pgbackrest.conf): 4 Vollbackups, 14 differenzielle → ca. 1 Monat PITR-Fenster.

## Ersteinrichtung

    pgbackrest --stanza=frauenhaus stanza-create
    pgbackrest --stanza=frauenhaus check
    pgbackrest --stanza=frauenhaus --type=full backup

## Wiederherstellung

Letzter Stand:

    systemctl stop postgresql        # bzw. docker compose stop db
    pgbackrest --stanza=frauenhaus --delta restore
    systemctl start postgresql

Point-in-Time (z.B. vor einer versehentlichen Löschung):

    pgbackrest --stanza=frauenhaus --delta \
        --type=time --target="2026-07-04 10:00:00" restore

## 3-2-1-Regel

- `repo1-path` auf ein Volume legen, das NICHT auf derselben Platte wie `pgdata` liegt.
- Zweite Kopie extern: `repo2-*` auf S3-kompatiblen Speicher oder per rsync/rclone auf einen anderen Standort spiegeln (Backups sind bereits AES-256-verschlüsselt).
- `repo1-cipher-pass` getrennt vom Backup aufbewahren (Passwort-Manager/Safe) – ohne Passphrase ist das Backup wertlos, mit im selben Verzeichnis liegender Passphrase ist die Verschlüsselung wertlos.

## Restore-Test (Pflicht, quartalsweise)

1. Leeren Staging-Container starten (`postgres:16`, eigenes Volume).
2. `pgbackrest restore` gegen Staging ausführen.
3. Anwendung gegen Staging starten, Stichproben: Mitgliederzahl, Summen Spenden/Bußgelder des Vorjahres.
4. Ergebnis mit Datum in diesem Dokument protokollieren.

| Datum | Getestet von | Ergebnis |
|---|---|---|
|   |   |   |
