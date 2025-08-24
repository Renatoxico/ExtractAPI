import os
#from docling.document_converter import DocumentConverter
import pdfplumber

def validate_file(filepath):
    # Check if the file exists
    if not os.path.exists(filepath):
        return "File does not exist.: " + filepath
    
    # Check if the file is empty
    if os.path.getsize(filepath) == 0:
        return "File is empty."
    
    # Check if the file is bigger than 3MB
    # 1MB = 1024 * 1024 bytes
    if os.path.getsize(filepath) > 1 * 1024 * 1024:
        return "File is larger than 3MB."
    
    return "File is valid."

def process_file(source):
    #source = "C:\\Users\\diasr\\OneDrive\\Documents\\wrk sht\\fevererio25.pdf"  # document per local path or URL
    # Validate the file
    validation_result = validate_file(source)
    if validation_result != "File is valid.":
        return(f"Validation failed: {validation_result}")
    #converter = DocumentConverter()
    #result = converter.convert(source)
    with pdfplumber.open(source) as pdf:
      #text = "\n".join(page.extract_text() for page in pdf.pages)
        texts = []
        for page in pdf.pages:
            texts.append(page.extract_text())          
        text = "\n".join(texts)
    #return(result.document.export_to_markdown())  # output: "## Docling Technical Report[...]"
    return text