#main.py
from flask import Flask, jsonify, request

app = Flask(__name__)
predicts = [
    {
        "class_name": "fire",
        "date": "2021-05-11",
        "imgUrl": "https://storage.googleapis.com/asap80238image/1.jpg",
        "latitude": "1",
        "longtitude": "1",
        "probability": 1
    },
    {
        "class_name": "fire",
        "date": "2021-05-02",
        "imgUrl": "https://storage.googleapis.com/asap80238image/1.jpg",
        "latitude": "2",
        "longtitude": "2",
        "probability": 2
    },
   {
        "class_name": "smoke",
        "date": "2021-05-01",
        "imgUrl": "https://storage.googleapis.com/asap80238image/1.jpg",
        "latitude": "3",
        "longtitude": "3",
        "probability": 3
    }
]
@app.route('/get')
def home():
    return jsonify(predicts)

@app.route('/post', methods=['POST'])
def add_predicts():
    predict = request.get_json()
    predicts.append(predict)
    return jsonify(predicts)

if __name__ == '__main__':
  app.run(debug=True)
