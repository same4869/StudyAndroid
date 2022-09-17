package com.xun.encodelib

import java.io.File
import java.io.RandomAccessFile

object Utils {
    @Throws(Exception::class)
    fun getBytes(dexFile: File?): ByteArray {
        val fis = RandomAccessFile(dexFile, "r")
        val buffer = ByteArray(fis.length().toInt())
        fis.readFully(buffer)
        fis.close()
        return buffer
    }
}