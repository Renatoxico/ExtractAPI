# Phase 1: report expense editing

Implements Phase 1 and the settled 2026-11-02 decisions: repeatable category
corrections, hard deletion, and manual expenses. No frontend or Jev changes.

## Authentication and ownership

All endpoints require `Authorization: Bearer <Firebase ID token>`, using the
existing Firebase filter and local user principal. Send/accept `application/json`.
Only the authenticated user's report can be edited. Missing and other users'
reports both return 404 `REPORT_NOT_FOUND`. Expense lookup is additionally scoped
to the report in the path; a foreign expense ID (including one from another
report belonging to the same user) returns 404 `EXPENSE_NOT_FOUND`.

## Endpoints

| Method | Path | Success |
| --- | --- | --- |
| PATCH | `/v2/reports/{reportId}/expenses/category` | 200, refreshed ReportResponse |
| DELETE | `/v2/reports/{reportId}/expenses/{expenseId}` | 200, refreshed ReportResponse |
| POST | `/v2/reports/{reportId}/expenses` | 201, refreshed ReportResponse |

### Correct category

```json
{"expenseName":"SHOP","newCategory":"Supermercado"}
```

`expenseName` must be nonblank and match the stored name exactly (case and
whitespace included). Following the plan's grouped-expense endpoint, **every row
with that exact name in this report** is corrected, including manual rows. At
least one row must match, otherwise 404 `EXPENSE_NOT_FOUND`. `newCategory` must
match an allowed category below, otherwise 400 `INVALID_EXPENSE`.

Each successful request appends one audit row per matching expense, including
repeated requests with the same category. There is no edit limit or uniqueness
constraint. Old category can be null. The latest category and `categorySource =
USER` stay on the expense. Updates and audit inserts share one transaction;
expense row locks serialize competing corrections. The endpoint never writes to
the global category catalog. Both catalog registration queries exclude USER and
MANUAL expenses so a later scheduled scan cannot publish a personal correction.

### Remove expense

No request body. `expenseId` must be a positive integer; invalid IDs return 400.
A missing expense or an expense outside the path report returns 404
`EXPENSE_NOT_FOUND`. Deletion is permanent. Its audit history is deleted by the
foreign key's `ON DELETE CASCADE`; audit rows cannot block deletion. Other
expenses and their audit rows remain intact.

Deleting the last expense returns the existing report with empty `expenses`,
`expenseGroups`, and `categorySummaries`; all three highlight values are null.
The report remains readable at `GET /v2/extract/summary/{reportId}`, appears in
`GET /v2/extract/reports` with total/count zero, and exports a header-only CSV.
A second delete of the same expense returns 404.

### Add manual expense

```json
{"name":"Groceries","amount":42.50,"date":"2026-11-03","category":"Supermercado"}
```

All four fields are required. `name` must be nonblank; surrounding whitespace is
trimmed, while internal text is preserved. `amount` is a JSON decimal number and
must be greater than zero. `date` must be a real calendar date in `yyyy-MM-dd`
format with a positive four-digit year (no short dates or rollover inference).
`category` must exactly match an allowed category. Invalid fields return 400
`INVALID_EXPENSE`. The selected category is used directly, without merchant
rules or AI. The new expense has `source = MANUAL`, `categorySource = USER`, and
a server-generated `expenseId`. It can subsequently be corrected or deleted.

### Shared response body

All three endpoints return the same ReportResponse as the existing v2 summary:

```json
{
  "reportId":"report-id",
  "createdAt":"2026-11-03T10:00:00Z",
  "expenses":[{
    "expenseId":123,
    "expenseName":"Groceries",
    "amount":42.50,
    "date":"2026-11-03",
    "category":"Supermercado",
    "categorySource":"USER",
    "source":"MANUAL"
  }],
  "expenseGroups":[{
    "expenseName":"Groceries","totalAmount":42.50,"occurrenceCount":1,
    "category":"Supermercado"
  }],
  "categorySummaries":[{
    "category":"Supermercado","totalAmount":42.50,"occurrenceCount":1
  }],
  "highlights":{
    "largestExpense":{
      "expenseId":123,"expenseName":"Groceries","amount":42.50,
      "date":"2026-11-03","category":"Supermercado",
      "categorySource":"USER","source":"MANUAL"
    },
    "mostActiveDay":{"date":"2026-11-03","transactionCount":1,"totalAmount":42.50},
    "highestSpendingDay":{"date":"2026-11-03","transactionCount":1,"totalAmount":42.50}
  }
}
```

`categorySource` and `source` are additive expense fields (also in the existing
summary/upload responses and `highlights.largestExpense`). All existing fields
retain their names and meanings. Clients should tolerate absent/unknown source
values. Report totals/counts, merchant groups, category summaries, and highlights
are recomputed from DB queries after edits; there are no cached totals to repair.
The summary-list endpoint supplies the report's overall `total` and `countExpenses`.

Allowed categories (shared with the existing AI response validator):

- `Roupas / Acessórios`
- `E-commerce / Compras online`
- `Restaurante / Lanches`
- `Investimentos / Assinaturas profissionais`
- `Saúde / Farmácia / Bem-estar`
- `Transporte / Auto`
- `Lazer / Entretenimento / Pets`
- `Supermercado`
- `Outros / Transferências`
- `Moradia / Contas`

## Error contract

Errors use the existing `ErrorResponse` and `GlobalExceptionHandler`, for example:

```json
{
  "errorCode":"EXPENSE_NOT_FOUND",
  "message":"No expense found in the provided report",
  "details":null,
  "timestamp":"2026-11-03T10:00:00"
}
```

| Status | Code | Case |
| --- | --- | --- |
| 400 | `INVALID_EXPENSE` | Missing/blank fields, unsupported category, invalid date/amount, nonpositive expense ID |
| 400 | `INVALID_REQUEST` | Missing/null/malformed JSON, invalid JSON field types, nonnumeric or overflowing expense ID |
| 401 | Existing `AUTH_TOKEN_*` codes | Missing/invalid/expired authentication (unchanged security filter) |
| 404 | `REPORT_NOT_FOUND` | Report missing or owned by someone else |
| 404 | `EXPENSE_NOT_FOUND` | No matching name or expense in this report |
| 500 | `INTERNAL_SERVER_ERROR` | Unexpected failure; the edit transaction rolls back |

Phase 0's empty-after-validation error **already used the normal format**:
`ProcessingException` reaches the existing handler with HTTP 422,
`errorCode = NO_VALID_EXPENSES`, message, and timestamp. No Phase 0 behavior fix
was needed; its MVC regression now checks message/timestamp too. Malformed JSON
and path conversion failures previously used Spring's default response; this
phase overrides those two handlers to return the normal ErrorResponse with 400.

## Migration and classification notes

Exactly one new migration: `V11__expense_editing.sql`. V10 remains `app_error`.
Run Flyway before deploying the updated application.

- `expense.category_source`: non-null varchar(16), default/backfill `RULE`.
  This is a compatibility label for legacy rows, not reconstructed provenance.
  Existing Java-created extracted expenses also start with this default.
- `expense.source`: non-null varchar(16), default/backfill `EXTRACTED`.
- `expense_category_change`: identity ID, expense/report/user foreign keys,
  nullable old category, required new category and changed_at timestamp; indexes
  on each foreign key. All three foreign keys cascade on deletion, including
  report/account deletion. No unique report/expense/name constraint.
- Manual additions and corrections use USER. Both async propagation candidate
  selection and the actual SQL UPDATE exclude USER. The synchronous report
  catalog application has the same UPDATE guard. Thus even a queued AI result
  cannot overwrite USER, including a hypothetical USER row with null category.
  Categories copied from the global catalog use CATALOG, including catalog
  suggestions that originally came from Gemini; the catalog retains its own AI
  provenance. USER data is excluded from both catalog population paths.
- PATCH is added to the existing CORS allowed methods.

## Verification

From `javapi`, using the requested Java 26 workaround:

```sh
./mvnw test -Djacoco.skip=true -Dnet.bytebuddy.experimental=true \
  -DargLine="-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar"
```

Final result: **189 tests, 0 failures, 0 errors, 17 skipped** (172 executed).
The 17 skips are Docker-dependent PostgreSQL tests (15 existing plus two new).
No offline fallback was needed. H2/MockMvc tests exercise real
editing services/repositories, validation, ownership/report scoping, repeated
audit history, cascading deletion, empty reports, and refreshed aggregates.
Separate MVC tests exercise Firebase authentication and PATCH CORS. H2 in
PostgreSQL mode executes the actual worker and report-catalog UPDATE guards.
PostgreSQL tests additionally cover catalog privacy and worker guards; the
pre-existing latest-schema assertion is updated from stale V9 to V11.

Docker is unavailable here, so PostgreSQL/Testcontainers tests are skipped.
The production Flyway chain, PostgreSQL cascade behavior, and concurrent row
locking still require execution on a Docker/PostgreSQL-enabled machine. H2
schema generation verifies the modeled audit cascade, not Flyway migration SQL.
No external AI call or frontend/iOS integration was exercised.
