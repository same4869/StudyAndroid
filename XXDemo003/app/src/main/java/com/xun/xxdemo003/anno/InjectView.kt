package com.xun.xxdemo003.anno

import androidx.annotation.IntegerRes

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class InjectView(@IntegerRes val value: Int)