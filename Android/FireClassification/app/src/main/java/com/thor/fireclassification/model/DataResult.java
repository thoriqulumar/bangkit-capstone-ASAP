package com.thor.fireclassification.model;

public class DataResult {

    private String class_name;
    private float probability;
    private String date;
    private String latitude;
    private String longitude;
    private String imgUrl;


    public DataResult(String class_name, float probability, String date, String latitude, String longitude, String imgUrl) {
        this.class_name = class_name;
        this.probability = probability;
        this.date = date;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imgUrl = imgUrl;
    }

    public String getClass_name() {
        return class_name;
    }

    public void setClass_name(String class_name) {
        this.class_name = class_name;
    }

    public float getProbability() {
        return probability;
    }

    public void setProbability(float probability) {
        this.probability = probability;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}
