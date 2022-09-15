package com.xun.loader

import android.app.Application
import android.os.Environment
import android.util.Log
import java.io.File

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        //执行热修复。 插入补丁dex
        val pathDirStr = cacheDir.absolutePath + "/patch.dex"
        Log.d("kkkkkkkk", "pathDirStr:$pathDirStr")
        HotFix.installPatch(this, File(pathDirStr))
    }
}