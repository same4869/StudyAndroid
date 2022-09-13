package com.xun.easylib2.anno

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GET(
    val relativeUrl: String
)