package com.thor.fireclassification.service;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    public static Retrofit retrofit;
    private static final String BASE_URL = "https://jovial-light-312714.et.r.appspot.com";

    public static Retrofit getRetrofitInstance(){
        if (retrofit == null){
            retrofit =  new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return  retrofit;
    }
}
