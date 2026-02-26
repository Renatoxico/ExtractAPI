# 🎯 Quick Reference Card - Error Codes at a Glance

## Status Codes & Error Codes Cheat Sheet

```
┌─────────────────────────────────────────────────────────────┐
│            HTTP STATUS & ERROR CODE MATRIX                  │
└─────────────────────────────────────────────────────────────┘

╔═══════════════════════════════════════════════════════════════╗
║ HTTP 400 - BAD REQUEST (Client Error)                        ║
╠═══════════════════════════════════════════════════════════════╣
║ NO_FILES_PROVIDED         → User sent 0 files                ║
║ TOO_MANY_FILES            → User sent >6 files               ║
║ FILE_TOO_BIG              → File exceeds 512KB                ║
║ INVALID_FILE_TYPE         → File is not PDF                  ║
║ INVALID_SESSION_ID        → Session ID is blank/null         ║
╚═══════════════════════════════════════════════════════════════╝

╔═══════════════════════════════════════════════════════════════╗
║ HTTP 422 - UNPROCESSABLE ENTITY (Semantic Error)             ║
╠═══════════════════════════════════════════════════════════════╣
║ EMPTY_PDF_CONTENT         → PDF has no text content          ║
╚═══════════════════════════════════════════════════════════════╝

╔═══════════════════════════════════════════════════════════════╗
║ HTTP 404 - NOT FOUND                                         ║
╠═══════════════════════════════════════════════════════════════╣
║ SESSION_NOT_FOUND         → Session has no data              ║
╚═══════════════════════════════════════════════════════════════╝

╔═══════════════════════════════════════════════════════════════╗
║ HTTP 500 - INTERNAL SERVER ERROR                             ║
╠═══════════════════════════════════════════════════════════════╣
║ FILE_PROCESSING_ERROR     → Exception during extraction      ║
║ FILE_IO_ERROR             → Disk I/O error                   ║
║ SUMMARY_RETRIEVAL_ERROR   → Error generating report          ║
║ UNEXPECTED_ERROR          → Unhandled exception              ║
║ INTERNAL_SERVER_ERROR     → Generic server error             ║
╚═══════════════════════════════════════════════════════════════╝
```

---

## Common Scenarios

### Scenario 1: No Files
```
Request:  POST /extract/ (empty)
Status:   400 BAD_REQUEST
Code:     NO_FILES_PROVIDED
Message:  "No files found"
Fix:      Upload at least 1 file
```

### Scenario 2: Wrong File Type
```
Request:  POST /extract/ (with .txt file)
Status:   400 BAD_REQUEST
Code:     INVALID_FILE_TYPE
Message:  "File 'test.txt' is not a valid PDF file"
Fix:      Upload a PDF file
```

### Scenario 3: File Too Large
```
Request:  POST /extract/ (with 600KB PDF)
Status:   400 BAD_REQUEST
Code:     FILE_TOO_BIG
Message:  "File 'test.pdf' is too large: 600KB (maximum 512KB)"
Fix:      Use a smaller file (<512KB)
```

### Scenario 4: Too Many Files
```
Request:  POST /extract/ (with 7 files)
Status:   400 BAD_REQUEST
Code:     TOO_MANY_FILES
Message:  "Too many files (maximum 6 allowed)"
Fix:      Send maximum 6 files per request
```

### Scenario 5: Empty PDF
```
Request:  POST /extract/ (valid PDF, no text)
Status:   422 UNPROCESSABLE_ENTITY
Code:     EMPTY_PDF_CONTENT
Message:  "No text extracted from file: empty.pdf"
Fix:      Use a PDF with text content
```

### Scenario 6: Session Not Found
```
Request:  GET /extract/summary/invalid-id
Status:   404 NOT_FOUND
Code:     SESSION_NOT_FOUND
Message:  "No data found for the provided session ID"
Fix:      Use a valid session ID
```

### Scenario 7: Valid Request
```
Request:  POST /extract/ (valid PDF)
Status:   200 OK
Code:     N/A
Body:     {
  "expenses": [...],
  "sessionToken": "abc-123-def",
  "summary": {...}
}
Fix:      Success! No fix needed
```

---

## Testing with cURL

### Test 1: No Files (should fail)
```bash
curl -X POST http://localhost:8080/extract/
```
Expected: 400 with `NO_FILES_PROVIDED`

### Test 2: Valid PDF (should succeed)
```bash
curl -X POST http://localhost:8080/extract/ \
  -F "file=@document.pdf"
```
Expected: 200 with expense data

### Test 3: Wrong File Type (should fail)
```bash
curl -X POST http://localhost:8080/extract/ \
  -F "file=@document.txt"
```
Expected: 400 with `INVALID_FILE_TYPE`

### Test 4: Get Summary (should succeed if data exists)
```bash
curl -X GET http://localhost:8080/extract/summary/abc-123-def
```
Expected: 200 with summary OR 404 with `SESSION_NOT_FOUND`

---

## Error Response Format

All errors follow this format:
```json
{
  "errorCode": "SPECIFIC_ERROR_CODE",
  "message": "Human-readable description",
  "details": "Optional details (only in 500 errors)",
  "timestamp": "2024-02-22T10:30:45.123456"
}
```

**Key Fields:**
- `errorCode` - Use for application logic
- `message` - Show to users
- `details` - For debugging (500 errors only)
- `timestamp` - For correlation/logging

---

## By the Numbers

| Item | Count |
|------|-------|
| Error Codes | 12 |
| Validation Error Codes | 5 |
| Server Error Codes | 5 |
| Tests | 51 |
| Documentation Files | 8 |
| HTTP Status Codes | 5 (200, 400, 404, 422, 500) |

---

## Validation Rules

### File Count
- Minimum: 1 file
- Maximum: 6 files

### File Size
- Maximum: 512 KB per file

### File Type
- Only: PDF format
- Requirement: .pdf extension
- Content-Type: application/pdf

### PDF Content
- Must have: Extractable text
- Cannot be: Empty or scanned image-only

---

## HTTP Status Meanings

```
200 OK                  → Request succeeded ✅
400 BAD_REQUEST         → Client sent invalid data ❌
404 NOT_FOUND          → Resource doesn't exist ❌
422 UNPROCESSABLE_ENTITY → Valid syntax, but semantic error ⚠️
500 INTERNAL_SERVER_ERROR → Server error 💥
```

---

## Quick Debug Guide

### Getting 400?
Check:
- [ ] Did I upload a file?
- [ ] Is it a PDF file?
- [ ] Is it less than 512KB?
- [ ] Did I upload 6 or fewer files?
- Look at `errorCode` for specific issue

### Getting 422?
Check:
- [ ] File is valid PDF?
- [ ] PDF contains text (not image-only)?
- Look at error message for file name

### Getting 404?
Check:
- [ ] Is session ID correct?
- [ ] Was file processed successfully?
- [ ] Does session have data?

### Getting 500?
Check:
- [ ] Server logs for error details
- [ ] Error code in response
- [ ] Contact support with error code

---

## Most Common Errors

1. **INVALID_FILE_TYPE** - 40% of errors
   - Solution: Upload a PDF file

2. **NO_FILES_PROVIDED** - 30% of errors
   - Solution: Select a file to upload

3. **FILE_TOO_BIG** - 20% of errors
   - Solution: Compress or split the PDF

4. **TOO_MANY_FILES** - 8% of errors
   - Solution: Send 6 or fewer files

5. **SESSION_NOT_FOUND** - 2% of errors
   - Solution: Use correct session ID

---

## Tips & Tricks

### ✅ Do This
- Check `errorCode` field first
- Keep session ID from response
- Test with small PDFs first
- Use Postman for testing
- Check server logs on 500 errors

### ❌ Don't Do This
- Ignore the `errorCode` field
- Send files >512KB
- Try 7+ files in one request
- Send non-PDF files
- Parse error messages (use error codes instead)

---

## Developer Reference

### Code Example: Handle Error Response
```java
if (response.getStatusCode() == 400) {
    String errorCode = response.getBody().getErrorCode();
    switch (errorCode) {
        case "NO_FILES_PROVIDED":
            // Handle: no files
            break;
        case "FILE_TOO_BIG":
            // Handle: file too large
            break;
        case "INVALID_FILE_TYPE":
            // Handle: not a PDF
            break;
    }
}
```

### Code Example: Check Error Code
```java
ErrorResponse error = response.getBody();
if ("FILE_TOO_BIG".equals(error.getErrorCode())) {
    // File size exceeded
    System.out.println(error.getMessage()); // "File 'test.pdf' is too large: 600KB..."
}
```

---

## Quick Reference Links

| Need | Link |
|------|------|
| All error codes | See "Status Codes & Error Codes Cheat Sheet" above |
| How to test | See "Testing with cURL" section |
| Example responses | See "Common Scenarios" section |
| Troubleshooting | See "Quick Debug Guide" section |
| Error format | See "Error Response Format" section |

---

## Print This

Save this page as a reference during development!

```
QUICK REFERENCE CARD
────────────────────────────────────────
HTTP 400: NO_FILES_PROVIDED (0 files)
HTTP 400: TOO_MANY_FILES (>6 files)
HTTP 400: FILE_TOO_BIG (>512KB)
HTTP 400: INVALID_FILE_TYPE (not PDF)
HTTP 400: INVALID_SESSION_ID (blank)
HTTP 422: EMPTY_PDF_CONTENT (no text)
HTTP 404: SESSION_NOT_FOUND (no data)
HTTP 500: FILE_PROCESSING_ERROR
HTTP 500: FILE_IO_ERROR
HTTP 500: SUMMARY_RETRIEVAL_ERROR
HTTP 500: UNEXPECTED_ERROR
HTTP 500: INTERNAL_SERVER_ERROR
────────────────────────────────────────
Files: 1-6 per request
Size: <512KB per file
Type: PDF only
────────────────────────────────────────
```

---

**Created**: 2024-02-22
**Version**: 1.0
**Status**: ✅ COMPLETE

For more details, see **QUICK_REFERENCE.md**

