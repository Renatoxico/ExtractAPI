# Flyway database adoption

Flyway owns structural database changes from version 1 onward. Hibernate must
remain configured with `spring.jpa.hibernate.ddl-auto=validate`; it validates
the result after Flyway runs and must not create or update the schema.

## Version 1 contract

`javapi/src/main/resources/db/migration/V1__baseline.sql` is the complete
schema-only representation of production immediately before Flyway was
introduced. It intentionally contains no production rows.

The baseline was captured from the `extractorprod` archive whose SHA-256 is
`25E8B1F847AD2F8DB117A4A543E029227BB70B5088D149630F196CC4444D9254`.
It includes `app_user`, `expense_report`, `tb_expense`, and the legacy `users`
table. Legacy objects must remain in V1 so a new database reproduces the same
starting state; remove them only through a later versioned migration.

- An empty database runs V1 and is created from scratch.
- An existing database whose schema already matches V1 is recorded at version
  1 without executing V1.
- Every later structural change starts at V2 and is made only by a migration.

Do not recreate or edit V1 from the JPA model or from an older development
database. Its normalized schema has been verified against both the production
clone and a new PostgreSQL database created by Flyway.

## Clone production into a development database

The clone is intentionally a full schema-and-data copy. Run it only after the
source and target endpoints have been independently identified.

1. Stop application writers or otherwise choose a transactionally consistent
   copy window.
2. Create timestamped custom-format backups of both databases:

   ```powershell
   pg_dump --format=custom --file production-before-flyway.dump `
     --host <production-host> --port <production-port> `
     --username <production-user> <production-database>

   pg_dump --format=custom --file development-before-clone.dump `
     --host <development-host> --port <development-port> `
     --username <development-user> <development-database>
   ```

3. Confirm that both dumps can be listed with `pg_restore --list` and retain
   the development backup until the clone has been accepted.
4. Disconnect clients, drop and recreate the development database from
   `template0`, then restore the production dump. Recreating the database is
   required: `pg_restore --clean` only removes objects that also occur in the
   dump and can leave development-only objects behind.
5. Compare schema-only dumps and table row counts. Do not proceed to Flyway
   while either differs unexpectedly.

Database passwords must be supplied through a protected pgpass file or a
temporary `PGPASSWORD` environment variable. Never put them in a command,
tracked file, dump filename, or terminal transcript.

## Adopt an existing database at V1

Before adoption, compare the existing schema with V1 and take a fresh backup.
Then perform exactly one controlled application startup with:

```dotenv
FLYWAY_BASELINE_ON_MIGRATE=true
```

For a non-empty schema without `flyway_schema_history`, Flyway records a
baseline row at version 1 and skips `V1__baseline.sql`. Confirm the history row
and confirm that Hibernate validation succeeds. Immediately restore the normal
setting:

```dotenv
FLYWAY_BASELINE_ON_MIGRATE=false
```

Leaving automatic baselining enabled removes an important wrong-database
safety check and is not supported for normal application operation.

## Version 4 naming migration

`V4__standardize_report_and_expense_names.sql` migrates the application to the
canonical report/expense vocabulary. It renames `tb_expense` to `expense`,
uses `expense_report.id` and `expense.report_id`, and renames the expense
description, category, amount, and creation timestamp columns. It also adds
the expense primary key, mandatory report foreign key, and report index.

The migration deliberately stops when orphan expenses exist. Before applying
it to an existing database, verify that every `tb_expense.session_id` matches
an `expense_report.session_id`, take a backup, and record row counts and total
expense values for comparison after migration. Blank categories are normalized
to `NULL`; the legacy `users` table is intentionally preserved.

## Verification gates before later migrations

- A new empty PostgreSQL database reaches version 1 by executing V1.
- A production clone reaches version 1 through controlled baseline adoption.
- The normalized schema of both databases matches production.
- Hibernate validation and the backend test suite pass.
- `FLYWAY_BASELINE_ON_MIGRATE` is false in normal configuration.
- No manual schema-creation step remains in the runbook.
- V4 preserves report and expense row counts and monetary totals.
- `expense.report_id` is non-null, indexed, and references `expense_report.id`.
