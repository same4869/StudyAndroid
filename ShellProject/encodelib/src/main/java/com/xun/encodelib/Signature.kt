package com.xun.encodelib

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

object Signature {
    fun zipalign(sourceApk: File, targetApk: File) {
        val cmd = arrayOf(
            "/Users/xun.wang/Library/Android/sdk/build-tools/30.0.2/zipalign",
            "-p",
            "-f",
            "-v",
            "4",
            sourceApk.absolutePath,
            targetApk.absolutePath
        )
        val process = Runtime.getRuntime().exec(cmd)
        println("start zipalign")
        try {
            val waitResult = process.waitFor()
            println("waitResult: $waitResult")
        } catch (e: InterruptedException) {
            e.printStackTrace()
            throw e
        }
        println("process.exitValue() " + process.exitValue())
        println("finish zipalign")
        process.destroy()
    }

    @Throws(InterruptedException::class, IOException::class)
    fun signature(unsignedApk: File, signedApk: File) {
        val cmd = arrayOf(
            "jarsigner", "-sigalg", "MD5withRSA",
            "-digestalg", "SHA1",
            "-keystore", "/Users/xun.wang/Desktop/mihoyo_android_debug.jks",
            "-storepass", "mihoyo",
            "-keypass", "mihoyo",
            "-signedjar", signedApk.absolutePath,
            unsignedApk.absolutePath,
            "android"
        )
        val process = Runtime.getRuntime().exec(cmd)
        println("start sign")
        try {
            val waitResult = process.waitFor()
            println("waitResult: $waitResult")
        } catch (e: InterruptedException) {
            e.printStackTrace()
            throw e
        }
        println("process.exitValue() " + process.exitValue())
        if (process.exitValue() != 0) {
            val inputStream: InputStream = process.errorStream
            var len: Int
            val buffer = ByteArray(2048)
            val bos = ByteArrayOutputStream()
            while (inputStream.read(buffer).also { len = it } != -1) {
                bos.write(buffer, 0, len)
            }
            throw RuntimeException("?????????")
        }
        println("finish signed")
        process.destroy()
    }

    @Throws(InterruptedException::class, IOException::class)
    fun signatureWithApksigner(unsignedApk: File, signedApk: File){
        val cmd = arrayOf(
            "/Users/xun.wang/Library/Android/sdk/build-tools/30.0.2/apksigner", "sign", "--ks",
            "/Users/xun.wang/Desktop/mihoyo_android_debug.jks",
            "--ks-key-alias", "android",
            "--ks-pass", "pass:mihoyo",
            "--key-pass", "pass:mihoyo",
            "--out", signedApk.absolutePath,
            unsignedApk.absolutePath
        )
        val process = Runtime.getRuntime().exec(cmd)
        println("start sign")
        try {
            val waitResult = process.waitFor()
            println("waitResult: $waitResult")
        } catch (e: InterruptedException) {
            e.printStackTrace()
            throw e
        }
        println("process.exitValue() " + process.exitValue())
        if (process.exitValue() != 0) {
            val inputStream: InputStream = process.errorStream
            var len: Int
            val buffer = ByteArray(2048)
            val bos = ByteArrayOutputStream()
            while (inputStream.read(buffer).also { len = it } != -1) {
                bos.write(buffer, 0, len)
            }
            throw RuntimeException("?????????")
        }
        println("finish signed")
        process.destroy()
    }
}