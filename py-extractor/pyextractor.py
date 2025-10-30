import tempfile
import os
from pdf_processor import process_file
from fastapi import FastAPI, UploadFile, File
from fastapi.responses import JSONResponse

app = FastAPI()

@app.get("/")
async def read_root():
    return "Py-extractor"

@app.post("/process")
def process_data(file: UploadFile = File(...)):
    if not file:
        return JSONResponse({"error": "No file provided"}, status_code=400)
    
    filepath = save_file(file)
    res = process_file(filepath)
    return JSONResponse(res)

def save_file(file):
    tempDir = tempfile.mkdtemp()
    tempDir = os.path.join(tempDir, file.filename)
    with open(tempDir, "wb") as buffer:
        buffer.write(file.file.read())
    return tempDir