# Use an official Python runtime as a parent image
FROM python:3.14.0rc3-slim-trixie

WORKDIR /app

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 9000

CMD ["uvicorn", "pyextractor:app", "--host", "0.0.0.0", "--port", "9000"]
