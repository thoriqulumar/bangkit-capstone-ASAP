package com.thor.fireclassification.model;

public class DataItem{
	private String date;
	private String imgUrl;
	private double probability;
	private String latitude;
	private String longtitude;
	private String class_name;

	public DataItem(String date, String imgUrl, double probability, String latitude, String longtitude, String class_name) {
		this.date = date;
		this.imgUrl = imgUrl;
		this.probability = probability;
		this.latitude = latitude;
		this.longtitude = longtitude;
		this.class_name = class_name;
	}

	public String getDate(){
		return date;
	}

	public String getImgUrl(){
		return imgUrl;
	}

	public double getProbability(){
		return probability;
	}

	public String getLatitude(){
		return latitude;
	}

	public String getLongtitude(){
		return longtitude;
	}

	public String getClass_name(){
		return class_name;
	}
}
