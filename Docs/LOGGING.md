# Backend logging

The backend writes to the console and `extract-api.log`. Docker mounts
`./logs/backend` (relative to the Compose project) at `/app/logs`. Files survive
container recreation. Local Java runs write to `./logs/extract-api.log`, relative
to the working directory.

## Docker setup

After pulling or building the backend image, prepare the directory before the
first start. The CI/CD deployment runs these same commands automatically:

```sh
docker compose run --rm --no-deps --user root --entrypoint sh backend-java -c 'chown appuser:appuser /app/logs && chmod 0750 /app/logs'
docker compose run --rm --no-deps --entrypoint sh backend-java -c 'test -w /app/logs && touch /app/logs/.write-check && rm /app/logs/.write-check'
docker compose up -d
```

The preparation container sets ownership using the image's actual `appuser`
UID/GID. The backend itself still runs as `appuser`. The host directory has mode
0750; use sudo or an appropriately authorized group to read logs on Linux. Do
not make it world-writable. If an existing log file was created by another user,
correct that file's ownership before starting the application.

## Configuration

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `LOG_FILE` | `./logs/extract-api.log` | Local log path; Compose fixes this to `/app/logs/extract-api.log` |
| `LOG_LEVEL` | `INFO` | Root logging level |
| `LOG_MAX_FILE_SIZE` | `10MB` | Size rollover threshold |
| `LOG_MAX_HISTORY` | `14` | Days of archived logs retained |
| `LOG_TOTAL_SIZE_CAP` | `200MB` | Total archive size cap, excluding the active file |

Logs roll daily and at the size threshold to `extract-api.log.YYYY-MM-DD.N.gz`.
Expired archives are cleaned at startup and rollover. Cleanup is asynchronous,
so the size cap is not a strict instantaneous disk quota. Standard Spring Boot
`LOGGING_LEVEL_*` settings can enable DEBUG for individual application packages.
The console continues to use Docker's logging driver and its retention settings.

```sh
sudo tail -F logs/backend/extract-api.log
sudo grep -F 'reportId=REPORT_ID' logs/backend/extract-api.log
sudo zgrep -F 'Expense date validation failed' logs/backend/*.gz
docker compose logs --tail=100 backend-java
```

## Diagnostic events

A successful file produces one parsing summary with its report ID, filename,
candidate count, and saved expense count. Saving happens inside the enclosing
transaction; a later file failure can still roll back the report. Routine
scheduler invocations, empty work checks, and parsing progress are DEBUG logs.

Invalid dates produce one warning after all year candidates have been tried.
Fields include `filename`, `reportId`, `lineNumber`, `sourceLine`, `candidate`,
`originalDate`, `attemptedYears`, and `reason`. Line numbers refer to PDFBox's
extracted text, not printed PDF line numbers. Two expenses on one text line
share that line number and original text but have separate candidate fragments.
Short `dd/MM` dates and successful year fallback do not produce warnings.
Existing date acceptance and persistence behavior is unchanged.

Source text is only logged for problems; it can contain transaction details.
Control characters are escaped and each field is limited to 4,096 characters,
with `...[truncated]` marking truncation. Source context lives only in memory
while processing and is never added to API responses or stored in the database.
Older stored reports can only provide report/expense IDs and the failing date.
Terminal request failures are logged once by the exception handler; ordinary
4xx errors use WARN without stack traces, and 5xx errors include their cause.

## Verification

Run `./javapi/mvnw verify -f javapi/pom.xml` with Java 21 and Docker available.
Logging tests exercise contextual diagnostics, response isolation, file output,
rollover, and persistence across separate JVM starts. PostgreSQL integration
tests require Docker. Deployment's preparation/write checks exercise non-root
access to the mounted host directory.
