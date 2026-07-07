# Datenübernahme aus dem MSSQL-Backup

Quelle: `Backup_MSSQL_FH_anonymisiert.bak` (SQL Server, DB `frauenhaus`, vgl. `fh.dsn`).
Die `fh_MA.accdb` ist nur ein Access-Frontend mit ODBC-Links auf dieselbe DB – keine eigenen Daten übernehmen.

## 1. SQL Server temporär in Docker starten (auch auf Apple Silicon via Rosetta)

    docker run -d --name mssql-migration --platform linux/amd64 \
      -e ACCEPT_EULA=Y -e MSSQL_SA_PASSWORD='Migration!2026' \
      -p 1433:1433 \
      -v "$PWD:/backup" \
      mcr.microsoft.com/mssql/server:2022-latest

## 2. Backup einspielen

    docker exec mssql-migration /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P 'Migration!2026' -Q "
      RESTORE FILELISTONLY FROM DISK='/backup/Backup_MSSQL_FH_anonymisiert.bak'"
    # logische Namen aus der Ausgabe unten bei MOVE eintragen:
    docker exec mssql-migration /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P 'Migration!2026' -Q "
      RESTORE DATABASE frauenhaus FROM DISK='/backup/Backup_MSSQL_FH_anonymisiert.bak'
      WITH MOVE 'frauenhaus'     TO '/var/opt/mssql/data/frauenhaus.mdf',
           MOVE 'frauenhaus_log' TO '/var/opt/mssql/data/frauenhaus_log.ldf'"

## 3. Kontrolle: echtes Schema dumpen und mit V1__baseline_schema.sql abgleichen

    docker exec mssql-migration /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P 'Migration!2026' -d frauenhaus -Q "
      SELECT t.name, c.name, ty.name, c.max_length, c.is_nullable
      FROM sys.tables t
      JOIN sys.columns c ON c.object_id = t.object_id
      JOIN sys.types ty ON ty.user_type_id = c.user_type_id
      WHERE SCHEMA_NAME(t.schema_id) = 'frauenhaus'
      ORDER BY t.name, c.column_id" -s ';' -W > alt-schema.txt

## 4. Daten nach PostgreSQL übertragen

Ziel-DB starten (`docker compose up -d db`), Flyway-Schema anlegen lassen (Backend einmal starten
oder `mvn flyway:migrate`), dann pro Tabelle in FK-Reihenfolge kopieren – am einfachsten mit pgloader:

    pgloader mssql://sa:'Migration!2026'@localhost/frauenhaus \
             pgsql://frauenhaus_app:$DB_PASSWORD@localhost/frauenhaus

Reihenfolge bei manueller Übernahme (CSV-Export je Tabelle → `\copy` in psql):
anrede, verein, bussgeldstatus, spendentyp, gericht, stichwort →
mitglied, spendenart → verein_mitglied, stichwort_person, spende, bussgeld → eingang.

Nur das Schema `frauenhaus` übernehmen – die `compucrash.*`-Tabellen (UI-Metadaten) und
`dbo.*`-Reste (dtproperties, tbl_AP_Mail_*) bleiben zurück.

## 5. Nachprüfungen

    SELECT count(*) FROM frauenhaus.mitglied;   -- muss der MSSQL-Zählung entsprechen
    SELECT sum(betrag) FROM frauenhaus.spende;  -- Summenvergleich mit MSSQL
    SELECT sum(betrag) FROM frauenhaus.bussgeld;

Danach den Migration-Container entsorgen: `docker rm -f mssql-migration`.
