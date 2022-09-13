package com.xun.xxdemo003

import android.app.Activity
import android.util.Log
import android.view.View
import com.xun.xxdemo003.anno.InjectIntent
import com.xun.xxdemo003.anno.InjectView

object InjectUtils {
    fun injectView(activity: Activity) {
        activity.javaClass.declaredFields.forEach {
            Log.d("kkkkkkkkk", "it:$it")
            if (it.isAnnotationPresent(InjectView::class.java)) {
                val id = it.getAnnotation(InjectView::class.java).value
                val view = activity.findViewById<View>(id)
                try {
                    it.isAccessible = true
                    it.set(activity, view)
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun injectIntent(activity: Activity) {
        activity.javaClass.declaredFields.forEach {
            if (it.isAnnotationPresent(InjectIntent::class.java)) {
                val key = it.getAnnotation(InjectIntent::class.java).value
                val value = activity.intent.getStringExtra(key)
                try {
                    it.isAccessible = true
                    it.set(activity, value)
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
        }
    }
}