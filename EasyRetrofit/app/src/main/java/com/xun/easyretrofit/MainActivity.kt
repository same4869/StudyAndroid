package com.xun.easyretrofit

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.xun.easylib2.EasyRetrofit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val enjoyWeatherApi =
            EasyRetrofit.Companion.Builder().baseUrl("https://restapi.amap.com").build()
                .create(EnjoyWeatherApi::class.java)

        findViewById<Button>(R.id.getWeatherBtn).setOnClickListener {
            val call = enjoyWeatherApi.getWeather("110101", "ae6c53e2186f33bbf240a12d80672d1b")
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.i("kkkkkkkk", "onFailure get: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.i("kkkkkkkk", "onResponse get: ${response.body?.string()}")
                }

            })
        }
    }
}