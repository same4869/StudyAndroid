package com.xun.encodelib

import com.xun.encodelib.Zip.unZip
import java.io.*

object Dx {
    @Throws(IOException::class, InterruptedException::class)
    fun jar2Dex(aarFile: File): File {
        val fakeDex = File(aarFile.parent + File.separator.toString() + "temp")
        println("jar2Dex: aarFile.getParent(): " + aarFile.getParent())
        unZip(aarFile, fakeDex)
        val files: Array<File> = fakeDex.listFiles(object : FilenameFilter {
            override fun accept(file: File?, s: String): Boolean {
                return s == "classes.jar"
            }
        })
        if (files.isEmpty()) {
            throw RuntimeException("the aar is invalidate")
        }
        val classes_jar: File = files[0]
        val aarDex = File(classes_jar.parentFile, "classes.dex")

        dxCommand(aarDex, classes_jar)
        return aarDex
    }

    @Throws(IOException::class, InterruptedException::class)
    fun dxCommand(aarDex: File, classes_jar: File) {
        val runtime = Runtime.getRuntime()
        val process = runtime.exec(
            "/Users/xun.wang/Library/Android/sdk/build-tools/30.0.2/dx --dex --output=" + aarDex.absolutePath
                .toString() + " " +
                    classes_jar.absolutePath
        )
        println(
            ("------> dx --dex --output=" + aarDex.absolutePath.toString() + " " +
                    classes_jar.absolutePath)
        )
        try {
            process.waitFor()
        } catch (e: InterruptedException) {
            e.printStackTrace()
            throw e
        }
        if (process.exitValue() != 0) {
            val inputStream: InputStream = process.errorStream
            var len: Int
            val buffer = ByteArray(2048)
            val bos = ByteArrayOutputStream()
            while ((inputStream.read(buffer).also { len = it }) != -1) {
                bos.write(buffer, 0, len)
            }
            throw RuntimeException("dx run failed")
        }
        process.destroy()
    }
}