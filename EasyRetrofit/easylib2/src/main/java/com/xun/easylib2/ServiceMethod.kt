package com.xun.easylib2

import android.util.Log
import com.xun.easylib2.anno.GET
import com.xun.easylib2.anno.Query
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.Request
import java.lang.reflect.Method

class ServiceMethod private constructor(builder: Builder) {

    private var callFactory: Call.Factory
    private var relativeUrl: String
    private var parameterHandler: Array<ParameterHandler?>

    var httpMethod: String

    private var urlBuilder: HttpUrl.Builder? = null
    private var baseUrl: HttpUrl? = null


    init {
        baseUrl = builder.enjoyRetrofit.baseUrl
        callFactory = builder.enjoyRetrofit.callFactory

        httpMethod = builder.httpMethod
        relativeUrl = builder.relativeUrl
        parameterHandler = builder.parameterHandler
    }

    /**
     * 根据已经收集好的信息，调用对应的网络框架
     */
    fun invoke(args: Array<out Any>): Call {
        /**
         * 1  处理请求的地址与参数
         */
        for (i in parameterHandler.indices) {
            val handlers: ParameterHandler? = parameterHandler[i]
            //handler内本来就记录了key,现在给到对应的value
            handlers?.apply(this, args[i].toString())
        }

        //获取最终请求地址

        //获取最终请求地址
        if (urlBuilder == null) {
            urlBuilder = baseUrl!!.newBuilder(relativeUrl)
        }
        val url: HttpUrl = urlBuilder!!.build()

        Request.Builder().url(url).method(httpMethod, null).build().let {
            return callFactory.newCall(it)
        }
    }

    fun addQueryParameter(key: String, value: String) {
        if (urlBuilder == null) {
            urlBuilder = baseUrl?.newBuilder(relativeUrl)!!
        }
        urlBuilder?.addQueryParameter(key, value)
    }

    companion object {
        class Builder(enjoyRetrofitP: EasyRetrofit, method: Method) {
            private var methodAnnotations: Array<Annotation>
            private var parameterAnnotations: Array<Array<Annotation>>
            var enjoyRetrofit: EasyRetrofit
            lateinit var httpMethod: String
            lateinit var relativeUrl: String
            lateinit var parameterHandler: Array<ParameterHandler?>

            init {
                enjoyRetrofit = enjoyRetrofitP
                methodAnnotations = method.annotations
                parameterAnnotations = method.parameterAnnotations
            }

            fun build(): ServiceMethod {
                methodAnnotations.forEach {
                    if (it is GET) {
                        httpMethod = "GET"
                        relativeUrl = it.relativeUrl
                    }
                }

                Log.d("kkkkkkkk", "httpMethod:$httpMethod relativeUrl:$relativeUrl")

                parameterHandler = Array(parameterAnnotations.size) { null }
                //所有参数上的所有注解
                parameterAnnotations.forEach { annotations ->
                    //一个参数上的所有注解
                    annotations.forEachIndexed { index, annotation ->
                        if (annotation is Query) {
                            parameterHandler[index] =
                                ParameterHandler.Companion.QueryParameterHandler(annotation.value)
                            Log.d("kkkkkkkk", "parameterHandler$index : ${parameterHandler[index]}")
                        }
                    }
                }

                return ServiceMethod(this)
            }
        }
    }
}