#db.py
import os
import pymysql
from flask import jsonify

db_user = os.environ.get('CLOUD_SQL_USERNAME')
db_password = os.environ.get('CLOUD_SQL_PASSWORD')
db_name = os.environ.get('CLOUD_SQL_DATABASE_NAME')
db_connection_name = os.environ.get('CLOUD_SQL_CONNECTION_NAME')


def open_connection():
    unix_socket = '/cloudsql/{}'.format(db_connection_name)
    try:
        if os.environ.get('GAE_ENV') == 'standard':
            conn = pymysql.connect(user=db_user, password=db_password,
                                unix_socket=unix_socket, db=db_name,
                                cursorclass=pymysql.cursors.DictCursor
                                )
    except pymysql.MySQLError as e:
        print(e)

    return conn


def get_predict():
    conn = open_connection()
    with conn.cursor() as cursor:
        result = cursor.execute('SELECT * FROM predict;')
        image = cursor.fetchall()
        if result > 0:
            got_image = jsonify(image)
        else:
            got_image = 'No image in DB'
    conn.close()
    return got_image

def add_predict(predict):
    conn = open_connection()
    with conn.cursor() as cursor:
        cursor.execute('INSERT INTO predict (class_name, date, imgUrl, latitude, longtitude, probability) VALUES(%s, %s, %s, %s, %s, %s)', (predict["class_name"], predict["date"], predict["imgUrl"], predict["latitude"], predict["longtitude"], predict["probability"]))
    conn.commit()
    conn.close()
