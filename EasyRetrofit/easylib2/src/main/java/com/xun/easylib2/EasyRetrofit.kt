package com.xun.easylib2

import android.util.Log
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class EasyRetrofit {
    var baseUrl: HttpUrl
    var callFactory: Call.Factory

    constructor(callFactoryP: Call.Factory, baseUrlP: HttpUrl) {
        baseUrl = baseUrlP
        callFactory = callFactoryP
    }

    companion object {
        class Builder {
            private var baseUrl: HttpUrl? = null

            fun baseUrl(baseUrlP: String): Builder {
                baseUrl = baseUrlP.toHttpUrlOrNull()
                return this
            }

            fun build(): EasyRetrofit {
                if (baseUrl == null) {
                    throw IllegalStateException("BaseUrl cannot be null")
                }
                return EasyRetrofit(OkHttpClient(), baseUrl!!)
            }
        }
    }

    /**
     * 动态代理接口api，解析对应的注解参数，生成对应的代理对象
     */
    fun <T> create(service: Class<T>): T {
        return Proxy.newProxyInstance(
            service.classLoader, arrayOf<Class<*>>(service), object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method, args: Array<out Any>): Any {
                    Log.d("kkkkkkkk", "method:${method} args:${args}")
                    return loadServiceMethod(method).invoke(args)
                }
            }
        ) as T
    }

    fun loadServiceMethod(method: Method): ServiceMethod {
        return ServiceMethod.Companion.Builder(this, method).build()
    }
}