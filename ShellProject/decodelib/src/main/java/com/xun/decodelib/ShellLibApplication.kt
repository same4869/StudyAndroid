package com.xun.decodelib

import android.app.Application
import android.content.Context

class ShellLibApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        ApplicationHelper.attachBaseContext(base)
    }
}