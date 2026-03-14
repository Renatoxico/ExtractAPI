# Extract API - Complete Documentation

## Overview

The **Extract API** is a sophisticated service designed to process PDF files containing expense data. It leverages both Java (PDFBox) and Python (AI-based) processing to intelligently extract, categorize, and organize expense information from unstructured PDF documents.

### Base URL
```
http://localhost:9090
```

### Key Features
- ✅ **Multi-file Processing**: Upload and process multiple PDF files in a single request
- ✅ **Intelligent Extraction**: AI-powered text extraction and expense categorization
- ✅ **Session Management**: Unique session tokens for tracking and retrieving results
- ✅ **Comprehensive Reporting**: Grouped expense reports by category and date
- ✅ **Error Handling**: Detailed error responses with specific error codes
- ✅ **Data Validation**: Robust file validation and content verification

---

## Technology Stack

### Backend Framework
- **Spring Boot 3.5.3**: Modern Java framework for RESTful API development
- **Spring Data JPA**: ORM layer for database operations
- **PostgreSQL 42.7.7**: Robust relational database for persistent storage

### Processing & Utilities
- **Apache PDFBox 3.0.3**: Industry-standard Java library for PDF text extraction
- **Python Integration**: AI-powered expense categorization and processing
- **JSON Smart 2.5.2**: High-performance JSON parsing and manipulation

### Architecture
The application uses a layered architecture with the following services:

| Service | Responsibility |
|---------|-----------------|
| `ExtractorService` | Java-based PDF text extraction |
| `ObjectifierService` | Convert text to structured objects |
| `PythonProcessingService` | Integration with Python AI service |
| `AiProcessorService` | AI analysis and categorization |
| `ExpenseReportingService` | Report generation and session management |
| `ValidationService` | File and data validation |

---

## API Endpoints

### 1. GET /extract/
Returns API documentation and service information.

**Description**: The main entry point that serves comprehensive API documentation in HTML format.

**Request**: No parameters required

**Response** (200 OK):
```
Content-Type: text/html
Returns: HTML documentation page
```

**Example**:
```bash
curl -X GET "http://localhost:9090/extract/"
```

---

### 2. POST /extract/
Upload and process one or multiple PDF files.

**Description**: The main processing endpoint. Extracts expenses from PDFs, categorizes them, and returns a grouped report.

**Request Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | MultipartFile[] | Yes | One or multiple PDF files (max 1MB each) |

**Response** (200 OK):
```json
{
  "sessionToken": "uuid-string",
  "categories": {
    "Groceries": [
      {
        "expenseName": "Weekly groceries",
        "value": 125.50,
        "date": "2024-03-08",
        "category": "Groceries"
      }
    ]
  },
  "summary": {
    "totalExpenses": 500.00,
    "categoryCount": 5
  }
}
```

**Error Responses**:
- `400 Bad Request`: No files provided or validation failed
- `422 Unprocessable Entity`: Empty PDF content
- `500 Internal Server Error`: Processing error

**Example**:
```bash
curl -X POST "http://localhost:9090/extract/" \
  -F "file=@expenses.pdf" \
  -F "file=@receipts.pdf"
```

---

### 3. GET /extract/summary/{sessionId}
Retrieve expense summary for a previously processed session.

**Description**: Fetch the expense report for a specific session using the session token.

**Request Parameters**:
| Parameter | Type | Location | Required | Description |
|-----------|------|----------|----------|-------------|
| `sessionId` | String | Path | Yes | Session token from POST response |

**Response** (200 OK):
```json
{
  "sessionToken": "uuid-string",
  "categories": { ... },
  "summary": { ... }
}
```

**Error Responses**:
- `400 Bad Request`: Empty or invalid sessionId
- `404 Not Found`: No data for the provided session ID
- `500 Internal Server Error`: Retrieval error

**Example**:
```bash
curl -X GET "http://localhost:9090/extract/summary/550e8400-e29b-41d4-a716-446655440000"
```

---

## Error Codes

### Error Response Format
```json
{
  "errorCode": "ERROR_CODE_NAME",
  "message": "Human-readable error message",
  "details": "Optional detailed information",
  "timestamp": "2024-03-08T15:30:00"
}
```

### Error Code Reference

| Error Code | HTTP Status | Description |
|-----------|-------------|-------------|
| `INVALID_SESSION_ID` | 400 | Session ID is null, empty, or invalid |
| `EMPTY_PDF_CONTENT` | 422 | PDF exists but no extractable text found |
| `FILE_PROCESSING_ERROR` | 500 | Error during file processing |
| `SESSION_NOT_FOUND` | 404 | No data exists for the session ID |
| `VALIDATION_FAILED` | 400 | File validation failed |
| `SUMMARY_RETRIEVAL_ERROR` | 500 | Error retrieving summary |
| `UNEXPECTED_ERROR` | 500 | Unexpected server error |

### HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Request succeeded |
| 400 | Bad Request | Invalid parameters or validation failed |
| 404 | Not Found | Resource does not exist |
| 422 | Unprocessable Entity | Valid but cannot be processed |
| 500 | Internal Server Error | Server-side error |

---

## Data Models

### Expense Entity
```java
public class Expense {
    private Long id;                    // Auto-generated primary key
    private BigDecimal value;           // Monetary value
    private String transactionName;     // Name/description
    private String transactionType;     // Type of transaction
    private String date;                // Transaction date
    private String sessionId;           // Associated session
}
```

### ExpenseDTO
```java
public class ExpenseDTO {
    private String expenseName;         // Display name
    private BigDecimal value;           // Amount spent
    private String date;                // Expense date
    private String category;            // Category (Groceries, etc.)
}
```

---

## Configuration

| Property | Value |
|----------|-------|
| Server Port | 9090 |
| Max File Size | 1MB |
| Max Request Size | 1MB |
| Database | PostgreSQL with JPA/Hibernate |
| Temp Directory | {user.dir}/tmp/ |

### Environment Variables
- `DB_HOST` - PostgreSQL host
- `DB_PORT` - PostgreSQL port
- `DB_NAME` - Database name
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password

---

## Usage Examples

### JavaScript/Fetch
```javascript
// Upload files
const formData = new FormData();
formData.append('file', document.getElementById('file1').files[0]);
formData.append('file', document.getElementById('file2').files[0]);

fetch('http://localhost:9090/extract/', {
  method: 'POST',
  body: formData
})
.then(response => response.json())
.then(data => {
  console.log('Session Token:', data.sessionToken);
  console.log('Expenses:', data.categories);
})
.catch(error => console.error('Error:', error));

// Retrieve summary
fetch('http://localhost:9090/extract/summary/{sessionId}')
  .then(response => response.json())
  .then(data => console.log('Summary:', data));
```

### Python/Requests
```python
import requests

# Upload files
files = [
    ('file', open('expenses.pdf', 'rb')),
    ('file', open('receipts.pdf', 'rb'))
]

response = requests.post(
    'http://localhost:9090/extract/',
    files=files
)
data = response.json()
session_id = data['sessionToken']

# Get summary
summary = requests.get(
    f'http://localhost:9090/extract/summary/{session_id}'
).json()
print(summary)
```

### cURL
```bash
# Upload and process
curl -X POST "http://localhost:9090/extract/" \
  -F "file=@expenses.pdf" \
  -F "file=@receipts.pdf"

# Get summary
curl -X GET "http://localhost:9090/extract/summary/SESSION_ID"
```

---

## Getting Started

### Prerequisites
- Java 21 or higher
- PostgreSQL database
- Python service (for AI processing)

### Building & Running
```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Or run JAR
java -jar target/API-0.0.1-SNAPSHOT.jar
```

### Docker Deployment
```bash
# Using docker-compose
docker-compose up -d

# Custom Docker image
docker build -t extract-api .
docker run -p 9090:9090 extract-api
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| PDF extraction fails | Ensure PDF is not corrupted and contains extractable text |
| Session not found | Verify session ID is correct and data hasn't expired |
| Database connection error | Check PostgreSQL is running and credentials are correct |
| File size exceeded | Maximum is 1MB per file, 1MB total request size |
| Documentation not loading | Check file is at `src/main/resources/static/api-docs.html` |

---

## Response Examples

### Successful POST /extract/
```json
{
  "sessionToken": "550e8400-e29b-41d4-a716-446655440000",
  "categories": {
    "Groceries": [
      {
        "expenseName": "Whole Foods",
        "value": 125.50,
        "date": "2024-03-08",
        "category": "Groceries"
      }
    ],
    "Transportation": [
      {
        "expenseName": "Uber",
        "value": 18.75,
        "date": "2024-03-08",
        "category": "Transportation"
      }
    ]
  },
  "summary": {
    "totalExpenses": 144.25,
    "categoryCount": 2,
    "transactionCount": 2
  }
}
```

### Error Response
```json
{
  "errorCode": "EMPTY_PDF_CONTENT",
  "message": "No text extracted from file: document.pdf",
  "details": null,
  "timestamp": "2024-03-08T15:30:45"
}
```

---

## Support

For issues, questions, or feature requests, please contact the development team or check the application logs for detailed error messages.

**Last Updated**: March 2024  
**API Version**: 0.0.1  
**Java Version**: 21  
**Spring Boot Version**: 3.5.3

