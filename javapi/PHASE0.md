# Phase 0 safety net

Synthetic text fixtures and golden JSON cover Nubank, Itau, Bradesco,
Banco do Brasil, Inter and Santander. They are invented examples of common
statement tables, not a claim of complete support for these banks. Tests also
create tiny PDFs in memory and run extraction and parsing against the goldens.

## Behavior changes

- Monetary tokens have numeric boundaries and accept grouped or ungrouped
  thousands. Leading minus signs survive parsing and validation. Zero amounts
  remain excluded. Descriptions containing CREDITO are no longer excluded.
- The existing `amount` field now retains the source sign. Existing SQL sums
  (report, merchant groups and categories) and daily highlights therefore use
  net amounts; transaction counts include credits. CSV/API retain the sign.
  There are no new direction/type fields in this phase. Positive credits remain
  positive with their description intact; interpreting direction comes later.
- Descriptions retain internal whitespace, punctuation, long digit sequences,
  full length and installment markers; only surrounding whitespace is trimmed.
  This also allows the existing PAG*STEAM rule to match before classification.
  HostGator uses the prompt's Investimentos / Assinaturas profissionais category.
- Every date, including explicit years, is parsed strictly before persistence.
  Invalid rows are discarded. Short dates use the latest valid explicit
  transaction date as an anchor, with a month gap over six months treated as a
  year boundary in either direction. Without an explicit date they use the
  clock's latest nonfuture year. This is a heuristic: historical uploads with
  no years cannot be dated reliably until statement-period parsing exists.
  The response mapper also rejects invalid dates rather than coercing them.
- Candidates with no surviving transactions fail with HTTP 422,
  `NO_VALID_EXPENSES`, before persistence/report lookup. No candidate lines
  still yield `NO_EXPENSES_EXTRACTED`. Multi-file failures roll back the report.
- PDFBox documents close with try-with-resources. Application upload validation
  uses Spring's configured max-file-size (currently 1 MiB), inclusive, in bytes.

## Error recording

V10 creates only `app_error`. Fingerprints are SHA-256 of allowlisted error code
and enum stage, independent of users and financial data. Atomic PostgreSQL
upserts increment count, retain first_seen, and update last_seen and the latest
opaque correlation/report identifiers and structural metadata. Report IDs have
no foreign key so telemetry can survive report rollback.

ErrorRecorder permits only bounded integer fileCount/candidateCount/acceptedCount
metadata. It drops unknown keys/values, source text, filenames, PDF bytes,
amounts and exception messages; unknown codes become EXTRACTION_FAILED. Writes
run with REQUIRES_NEW through a TransactionTemplate. Begin, SQL and commit
failures are caught and logged without exception details. The original failure
continues unchanged. The facade records validation, creation, extraction,
parsing, reporting and catalog failures; the raw-text endpoint records its own
extraction failures. Correlation IDs are generated on the server per operation.

## Verification

Normal Java 21 command, from javapi:

```sh
./mvnw test
```

This machine has only Java 26. The unmodified invocation encounters unsupported
JaCoCo instrumentation and Mockito self-attachment issues. A Java 21 download
was attempted but DNS is unavailable. The suite passes here using cached deps:

```sh
./mvnw test -Djacoco.skip=true -Dnet.bytebuddy.experimental=true \
  -DargLine="-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar"
```

Result: 150 tests, 0 failures, 0 errors, 15 existing Docker-dependent tests
skipped (135 executed). Coverage generation is disabled only for this local
command. H2 slices verify signed aggregation, CSV and rollback; unit tests
verify recorder sanitization, fingerprint stability, transaction isolation mode
and swallowing begin/write/commit failures. MVC verifies the specific 422 body.
V10 and its PostgreSQL upsert have not been executed against PostgreSQL here;
Docker/PostgreSQL are unavailable. Existing database integration tests are
unchanged. Actual concurrent PostgreSQL upsert behavior still needs verification
on a machine with PostgreSQL.

Later phases must cover layout/period metadata, positive-credit direction,
trailing minus and parentheses, foreign currency columns, wrapped rows,
future-installment sections, and anonymized real bank PDFs. No Jev integration
or frontend changes are included.
