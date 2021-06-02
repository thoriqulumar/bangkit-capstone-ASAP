package com.thor.fireclassification.service;

import com.thor.fireclassification.model.DataItem;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SendData {
    @POST("/")
    Call<String> sendData(@Body DataItem body);
}
