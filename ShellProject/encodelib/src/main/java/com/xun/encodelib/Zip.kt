package com.xun.encodelib

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.*

object Zip {
    fun unZip(zip: File?, dir: File) {
        try {
            dir.delete()
            val zipFile = ZipFile(zip)
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val zipEntry = entries.nextElement()
                val name = zipEntry.name
                if (name == "META-INF/CERT.RSA" || name == "META-INF/CERT.SF" || (name
                            == "META-INF/MANIFEST.MF")
                ) {
                    continue
                }
                if (!zipEntry.isDirectory) {
                    val file = File(dir, name)
                    if (!file.parentFile.exists()) file.parentFile.mkdirs()
                    val fos = FileOutputStream(file)
                    val `is` = zipFile.getInputStream(zipEntry)
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (`is`.read(buffer).also { len = it } != -1) {
                        fos.write(buffer, 0, len)
                    }
                    `is`.close()
                    fos.close()
                }
            }
            zipFile.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    fun zip(dir: File, zip: File) {
        zip.delete()
        // 对输出文件做CRC32校验
        val cos = CheckedOutputStream(
            FileOutputStream(
                zip
            ), CRC32()
        )
        val zos = ZipOutputStream(cos)
        compress(dir, zos, "")
        zos.flush()
        zos.close()
    }

    @Throws(Exception::class)
    private fun compress(
        srcFile: File, zos: ZipOutputStream,
        basePath: String
    ) {
        if (srcFile.isDirectory) {
            compressDir(srcFile, zos, basePath)
        } else {
            compressFile(srcFile, zos, basePath)
        }
    }

    @Throws(Exception::class)
    private fun compressDir(
        dir: File, zos: ZipOutputStream,
        basePath: String
    ) {
        val files = dir.listFiles()
        // 构建空目录
        if (files.isEmpty()) {
            val entry = ZipEntry(basePath + dir.name + "/")
            zos.putNextEntry(entry)
            zos.closeEntry()
        }
        for (file in files) {
            // 递归压缩
            compress(file, zos, basePath + dir.name + "/")
        }
    }

    @Throws(Exception::class)
    private fun compressFile(file: File, zos: ZipOutputStream, dir: String) {
        val dirName = dir + file.name
        val dirNameNew = dirName.split("/".toRegex()).toTypedArray()
        val buffer = StringBuffer()
        if (dirNameNew.size > 1) {
            for (i in 1 until dirNameNew.size) {
                buffer.append("/")
                buffer.append(dirNameNew[i])
            }
        } else {
            buffer.append("/")
        }
        val entry = ZipEntry(buffer.toString().substring(1))
        zos.putNextEntry(entry)
        val bis = BufferedInputStream(
            FileInputStream(
                file
            )
        )
        var count: Int
        val data = ByteArray(1024)
        while (bis.read(data, 0, 1024).also { count = it } != -1) {
            zos.write(data, 0, count)
        }
        bis.close()
        zos.closeEntry()
    }
}