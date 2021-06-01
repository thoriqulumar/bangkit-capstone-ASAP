#main.py
from flask import Flask, jsonify, request
from db import get_predict, add_predict

app = Flask(__name__)

@app.route('/', methods=['POST', 'GET'])
def image():
    if request.method == 'POST':
        if not request.is_json:
            return jsonify({"msg": "Missing JSON in request"}), 400  

        add_predict(request.get_json())
        return 'Image Added'

    return get_predict()    

if __name__ == '__main__':
    app.run()
