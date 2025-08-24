import tempfile
import os
from flask import Flask, jsonify,request
from pdf_processor import validate_file, process_file
#import logging

# Set up basic configuration for logging
#logging.basicConfig(level=logging.DEBUG)

app = Flask(__name__)
port = int(os.environ.get('PORT', 9000))

@app.route("/")
def home():
    return "Hello, this is a Flask Microservice"

@app.route('/process', methods=['POST'])
def process():
   #data = request.form
   #logging.debug(data)
   #filepath = data.get('filepath')
    file = request.files.get('file')
    if not file:
        return jsonify({"error": "No file provided"}), 400
    
    filepath = save_file(file)

    result = process_file(filepath)
    return jsonify(result)

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=port)
#    app.run(debug=True, host="

def save_file(file):
    # Criando um diretório temporário
    temp_dir = tempfile.mkdtemp()
    
    # Definindo o caminho completo para o arquivo temporário
    temp_filepath = os.path.join(temp_dir, file.filename)
    
    # Salvando o arquivo no diretório temporário
    file.save(temp_filepath)
    
    return temp_filepath