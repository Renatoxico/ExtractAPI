## Error Handling Summary

### Códigos de Erro Padronizados da API

#### **FILE VALIDATION (400 BAD_REQUEST)**
- `INVALID_FILE_TYPE` - Tipo de arquivo inválido (apenas PDF aceito)
- `FILE_TOO_BIG` - Arquivo excede o tamanho máximo permitido (512KB)
- `NO_FILES_PROVIDED` - Nenhum arquivo foi enviado
- `TOO_MANY_FILES` - Mais de 6 arquivos foram enviados

#### **PDF PROCESSING (422 UNPROCESSABLE_ENTITY)**
- `EMPTY_PDF_CONTENT` - PDF não contém texto ou está vazio
- `PDF_EXTRACTION_FAILED` - Falha ao extrair texto do PDF

#### **FILE PROCESSING (500 INTERNAL_SERVER_ERROR)**
- `FILE_PROCESSING_ERROR` - Erro ao processar um arquivo específico
- `FILE_IO_ERROR` - Erro de entrada/saída ao manipular arquivo

#### **SESSION MANAGEMENT (400/404)**
- `INVALID_SESSION_ID` - SessionID nulo ou vazio (400 BAD_REQUEST)
- `SESSION_NOT_FOUND` - SessionID válido mas sem dados (404 NOT_FOUND)

#### **REPORTING (500 INTERNAL_SERVER_ERROR)**
- `SUMMARY_RETRIEVAL_ERROR` - Erro ao gerar relatório de despesas

#### **GENERAL (500 INTERNAL_SERVER_ERROR)**
- `UNEXPECTED_ERROR` - Erro inesperado na API
- `INTERNAL_SERVER_ERROR` - Erro interno do servidor

---

### Estrutura de Resposta de Erro

```json
{
  "errorCode": "FILE_VALIDATION_FAILED",
  "message": "File validation failed: Invalid file type",
  "details": "Optional details about the error",
  "timestamp": "2024-02-20T10:30:45.123456"
}
```