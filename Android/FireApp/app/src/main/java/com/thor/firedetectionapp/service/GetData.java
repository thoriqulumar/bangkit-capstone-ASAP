package com.thor.firedetectionapp.service;

import com.google.gson.JsonObject;
import com.thor.firedetectionapp.model.Data;
import com.thor.firedetectionapp.model.DataItem;
import com.thor.firedetectionapp.model.DataResult;

import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface GetData {
    @GET("/")
    Call<ArrayList<DataItem>> getData();
}
