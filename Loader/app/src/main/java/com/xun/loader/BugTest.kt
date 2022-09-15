package com.xun.loader

import android.util.Log
import android.widget.Toast

object BugTest {
    fun testCrash() {
        val a = 1
        Log.d("kkkkkkkk","------------------------------ it must be crash")
        throw UnsupportedOperationException("hahahahaha")
    }
}