# Quick Reference: Validation Flow & Error Handling

## API Response Codes at a Glance

| Status | Code | Trigger | When |
|--------|------|---------|------|
| **400** | `NO_FILES_PROVIDED` | User sends no files | POST /extract/ with no files |
| **400** | `TOO_MANY_FILES` | User sends >6 files | POST /extract/ with 7+ files |
| **400** | `FILE_TOO_BIG` | File exceeds 512KB | Any file larger than 512KB |
| **400** | `INVALID_FILE_TYPE` | File is not PDF | Non-PDF file submitted |
| **400** | `INVALID_SESSION_ID` | Session ID blank/null | GET /extract/summary/ (blank) |
| **422** | `EMPTY_PDF_CONTENT` | PDF has no text | Valid PDF but empty content |
| **404** | `SESSION_NOT_FOUND` | Session has no data | GET /extract/summary/{id} - no data |
| **500** | `FILE_PROCESSING_ERROR` | Error processing file | Exception during extraction |
| **500** | `FILE_IO_ERROR` | Disk I/O error | File read/write failure |
| **500** | `SUMMARY_RETRIEVAL_ERROR` | Database error | Error generating report |
| **500** | `UNEXPECTED_ERROR` | Unhandled exception | Any uncaught exception |
| **500** | `INTERNAL_SERVER_ERROR` | Generic server error | Fallback for unknown errors |

---

## Common Scenarios & Expected Responses

### Scenario 1: User Uploads Valid PDF
```
Request: POST /extract/ with valid.pdf (100KB)
Response: HTTP 200
Body: {
  "expenses": [...],
  "sessionToken": "abc-123-def"
}
```

### Scenario 2: User Uploads Non-PDF File
```
Request: POST /extract/ with spreadsheet.xlsx
Response: HTTP 400
Body: {
  "errorCode": "INVALID_FILE_TYPE",
  "message": "File 'spreadsheet.xlsx' is not a valid PDF file",
  "timestamp": "2024-02-22T10:30:45.123456"
}
```

### Scenario 3: User Uploads Large File
```
Request: POST /extract/ with large.pdf (600KB)
Response: HTTP 400
Body: {
  "errorCode": "FILE_TOO_BIG",
  "message": "File 'large.pdf' is too large: 600KB (maximum 512KB)",
  "timestamp": "2024-02-22T10:30:45.234567"
}
```

### Scenario 4: User Uploads Empty PDF
```
Request: POST /extract/ with empty.pdf (valid file, but no text)
Response: HTTP 422
Body: {
  "errorCode": "EMPTY_PDF_CONTENT",
  "message": "No text extracted from file: empty.pdf",
  "timestamp": "2024-02-22T10:30:45.345678"
}
```

### Scenario 5: User Retrieves Non-Existent Session
```
Request: GET /extract/summary/invalid-session-id
Response: HTTP 404
Body: {
  "errorCode": "SESSION_NOT_FOUND",
  "message": "No data found for the provided session ID",
  "timestamp": "2024-02-22T10:30:45.456789"
}
```

### Scenario 6: User Sends Blank Session ID
```
Request: GET /extract/summary/ (blank/null)
Response: HTTP 400
Body: {
  "errorCode": "INVALID_SESSION_ID",
  "message": "SessionId is required and cannot be empty",
  "timestamp": "2024-02-22T10:30:45.567890"
}
```

---

## Testing the Validation Flow

### Using cURL

#### Test 1: No Files
```bash
curl -X POST http://localhost:8080/extract/
```
Expected: 400 with `NO_FILES_PROVIDED`

#### Test 2: Invalid File Type
```bash
curl -X POST http://localhost:8080/extract/ \
  -F "file=@document.txt"
```
Expected: 400 with `INVALID_FILE_TYPE`

#### Test 3: Valid PDF
```bash
curl -X POST http://localhost:8080/extract/ \
  -F "file=@document.pdf"
```
Expected: 200 with session token

#### Test 4: Get Summary
```bash
curl -X GET http://localhost:8080/extract/summary/session-abc-123
```
Expected: 200 with expense data (or 404 if session doesn't exist)

### Using Postman

1. Create a new POST request to `http://localhost:8080/extract/`
2. Go to "Body" tab and select "form-data"
3. Add a key "file" (type: File) and select a PDF
4. Send the request
5. Check the response status and error code

---

## Development Reference

### Throwing an Error in Code

```java
// In ValidationService
ValidationResponse response = new ValidationResponse(
    false,                      // status
    "File is too large",        // message
    "FILE_TOO_BIG",            // errorCode
    HttpStatus.BAD_REQUEST      // httpStatus
);

// In Controller
if (!isValid.getStatus()) {
    throw new ProcessingException(
        isValid.getMessage(),
        isValid.getHttpStatus(),
        isValid.getErrorCode()
    );
}
```

### Adding a New Error Code

1. Add to `ERROR_CODES.md` documentation
2. Add constant to appropriate service/controller
3. Return proper HTTP status code
4. Create unit tests for the new error code
5. Update this quick reference guide

---

## Error Message Format

All error responses follow this format:
```json
{
  "errorCode": "SPECIFIC_ERROR_CODE",
  "message": "Human-readable description",
  "details": "Optional additional context",
  "timestamp": "ISO8601 timestamp"
}
```

**Important**: 
- `errorCode` is programmatic (use for routing/logic)
- `message` is user-friendly (can be displayed to users)
- `details` contains exception details (only in 500 errors)
- `timestamp` helps with debugging and correlation

---

## Validation Rules

### File Validation
- **Count**: Must have 1-6 files
- **Type**: Must be PDF (content-type: application/pdf)
- **Extension**: Must end with .pdf
- **Size**: Maximum 512KB per file

### Expense Validation
- **Value**: Must be positive (>0)
- **Name**: Must not be empty
- **Name**: Limited to 70 characters
- **Dates**: Extracted and normalized (DD/MM/YYYY format)
- **Classification**: Auto-classified by keywords (UBER→Transport, IFOOD→Food, etc.)

---

## Troubleshooting

### "NO_FILES_PROVIDED"
- ✅ Solution: Upload at least one file

### "INVALID_FILE_TYPE"
- ✅ Solution: Ensure file is PDF format
- ✅ Solution: Check file extension is .pdf

### "FILE_TOO_BIG"
- ✅ Solution: Use files smaller than 512KB
- ✅ Solution: Compress PDF if possible

### "EMPTY_PDF_CONTENT"
- ✅ Solution: PDF contains no extractable text
- ✅ Solution: Try OCR on scanned PDFs
- ✅ Solution: Use a valid, text-based PDF

### "TOO_MANY_FILES"
- ✅ Solution: Maximum 6 files per request
- ✅ Solution: Split into multiple requests

### "SESSION_NOT_FOUND"
- ✅ Solution: Session ID is incorrect
- ✅ Solution: Session may have expired
- ✅ Solution: Process new files to generate a session

### "INVALID_SESSION_ID"
- ✅ Solution: Session ID cannot be blank
- ✅ Solution: Provide a non-empty session ID

---

## Performance Expectations

| Operation | Time | Notes |
|-----------|------|-------|
| File Validation | <100ms | Quick check on file metadata |
| PDF Text Extraction | 1-5s | Depends on PDF complexity |
| Expense Extraction | 100-500ms | Regex matching on text |
| Classification | 50-200ms | Keyword-based auto-classification |
| DB Save | 100-300ms | Batch insert for 10-50 expenses |
| Report Generation | 200-500ms | Aggregation and grouping |
| **Total Request** | **2-7s** | All steps combined |

---

## Monitoring & Debugging

### Key Metrics to Monitor
- Count of each error code by day/week
- Average response times for each endpoint
- Number of 4xx vs 5xx errors
- Session generation rate
- File processing success rate

### Logging
Enable debug logging to see:
```log
2024-02-22 10:30:45 INFO  Validating file: document.pdf (150KB, type: application/pdf)
2024-02-22 10:30:45 INFO  All 1 files passed validation
2024-02-22 10:30:46 INFO  Extracted 12 expenses from document for session: abc-123-def
2024-02-22 10:30:46 INFO  Successfully processed 12 expenses for session: abc-123-def
```

---

## HTTP Status Code Reference

| Code | Meaning | Use Case |
|------|---------|----------|
| **200** | OK | Request succeeded, data returned |
| **400** | Bad Request | Client error (invalid input) |
| **404** | Not Found | Resource doesn't exist |
| **422** | Unprocessable Entity | Valid syntax but semantic error (empty PDF) |
| **500** | Internal Server Error | Server-side error |

---

## Summary

✅ **Validation**: Files are checked before processing
✅ **Error Codes**: Specific, actionable error messages
✅ **HTTP Status**: Correct status codes for each scenario
✅ **Testing**: 51 comprehensive tests
✅ **Documentation**: Clear error handling flow

For more details, see:
- `ERROR_CODES.md` - Complete error code documentation
- `IMPLEMENTATION_SUMMARY.md` - Implementation details and test coverage
- `ValidationFlow_Review_and_Tests.md` - Analysis and recommendations

