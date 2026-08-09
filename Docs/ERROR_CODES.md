## Error Handling Summary

### File validation — 400

- `INVALID_FILE_TYPE` — only PDF files are accepted.
- `FILE_TOO_BIG` — file exceeds 512 KB.
- `NO_FILES_PROVIDED` — no file was sent.
- `TOO_MANY_FILES` — more than six files were sent.
- `VALIDATION_FAILED` — generic file validation failure.

### PDF processing — 422

- `EMPTY_PDF_CONTENT` — the PDF contains no extractable text.
- `PDF_EXTRACTION_FAILED` — PDF text extraction failed.
- `NO_TEXT_EXTRACTED` — none of the supplied files produced text.

### File processing — 500

- `FILE_PROCESSING_ERROR` — processing failed for a specific file.
- `FILE_IO_ERROR` — file I/O failed.
- `TEXT_EXTRACTION_ERROR` — text extraction failed during processing.

### Report contract v2

- `INVALID_REPORT_ID` — report ID is empty or invalid (400).
- `REPORT_NOT_FOUND` — report does not exist or belongs to another user (404).
- `REPORT_DATA_INVALID` — stored report data cannot satisfy the v2 contract, such as an invalid historical date (500).
- `REPORT_RETRIEVAL_ERROR` — unexpected report retrieval failure (500).
- `CSV_EXPORT_ERROR` — CSV export failed (500).

### Legacy contract v1

- `INVALID_SESSION_ID` — legacy session ID is empty or invalid (400).
- `SESSION_NOT_FOUND` — legacy session has no accessible report (404).
- `SUMMARY_RETRIEVAL_ERROR` — legacy summary retrieval failed (500).

### General — 500

- `UNEXPECTED_ERROR` — unexpected API error.
- `INTERNAL_SERVER_ERROR` — unhandled internal error.

### Error response

```json
{
  "errorCode": "REPORT_NOT_FOUND",
  "message": "No report found for the provided report ID",
  "details": null,
  "timestamp": "2026-08-09T20:30:00"
}
```
