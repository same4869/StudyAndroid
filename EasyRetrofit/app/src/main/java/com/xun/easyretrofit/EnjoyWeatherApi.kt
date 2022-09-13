package com.xun.easyretrofit

import com.xun.easylib2.anno.GET
import com.xun.easylib2.anno.Query
import okhttp3.Call

interface EnjoyWeatherApi {
    @GET("/v3/weather/weatherInfo")
    fun getWeather(@Query("city") city: String?, @Query("key") key: String?): Call
}