package com.nicoqueijo.android.currencyconverter.kotlin.services

import com.nicoqueijo.android.currencyconverter.kotlin.models.ApiEndPoint
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// https://openexchangerates.org/api/latest.json?app_id={app_id}

interface ExchangeRatesService {

    @GET("api/latest.json")
    fun getExchangeRates(@Query("app_id") app_id: String): Call<ApiEndPoint>

}