#import requests
import os
from flask import Flask, jsonify,request
from pdf_processor import validate_file, process_file
#import logging

# Set up basic configuration for logging
#logging.basicConfig(level=logging.DEBUG)

app = Flask(__name__)
port = int(os.environ.get('PORT', 5000))

@app.route("/")
def home():
    return "Hello, this is a Flask Microservice"

@app.route('/process', methods=['POST'])
def process():
   data = request.form
   #logging.debug(data)
   filepath = data.get('filepath')
   result = process_file(filepath)
   return jsonify(result)

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=port)
