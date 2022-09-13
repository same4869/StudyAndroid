package com.xun.easylib2

abstract class ParameterHandler {
    abstract fun apply(serviceMethod: ServiceMethod, value: String)

    companion object {
        class QueryParameterHandler(keyP: String) : ParameterHandler() {
            private var key: String = ""

            init {
                key = keyP
            }

            override fun apply(serviceMethod: ServiceMethod, value: String) {
                serviceMethod.addQueryParameter(key, value)
            }
        }
    }
}