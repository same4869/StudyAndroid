package com.xun.encodelib

import com.xun.encodelib.Utils.getBytes
import com.xun.encodelib.Zip.zip
import java.io.File
import java.io.FileOutputStream


fun main() {
    //先做一些清理工作
    val tempFileApk = File("source/apk/temp")
    if (tempFileApk.exists()) {
        val files: Array<File> = tempFileApk.listFiles()
        for (file in files) {
            if (file.isFile) {
                file.delete()
            }
        }
    }

    val tempFileAar = File("source/aar/temp")
    if (tempFileAar.exists()) {
        val files: Array<File> = tempFileAar.listFiles()
        for (file in files) {
            if (file.isFile) {
                file.delete()
            }
        }
    }

    AES.init(AES.DEFAULT_PWD)

    val apkFile = File("source/apk/app-debug.apk")
    val newApkFile = File(apkFile.parent + File.separator + "temp")
    if (!newApkFile.exists()) {
        newApkFile.mkdirs()
    }
    //可以解压了
    val mainDexFile: File? = AES.encryptAPKFile(apkFile, newApkFile)
    if (newApkFile.isDirectory) {
        val listFiles = newApkFile.listFiles()
        for (file in listFiles) {
            if (file.isFile) {
                if (file.name.endsWith("classes.dex")) {
                    val name = file.name
                    println("rename step1:$name")
                    val cursor = name.indexOf(".dex")
                    val newName =
                        file.parent + File.separator + name.substring(0, cursor) + "_" + ".dex"
                    println("rename step2:$newName")
                    file.renameTo(File(newName))
                }
            }
        }
    }

    //把解密的aar打进去，当做主dex，里面需要有application
    val aarFile = File("source/aar/decodelib-debug.aar")
    val aarDex: File = Dx.jar2Dex(aarFile)


    val tempMainDex = File(newApkFile.path + File.separator + "classes.dex")
    if (!tempMainDex.exists()) {
        tempMainDex.createNewFile()
    }

    val fos = FileOutputStream(tempMainDex)
    val fbytes = getBytes(aarDex)
    fos.write(fbytes)
    fos.flush()
    fos.close()

    //合并之后生成目标apk
    val unsignedApk = File("result/apk-unsigned.apk")
    unsignedApk.parentFile.mkdirs()
    zip(newApkFile, unsignedApk)

    //生成目标apk之后对齐，但是小米10上还是会报没有对齐的错（关闭miui优化，不然直接报的无效apk），安卓9可以正常安装
    val unzipApk = File("result/apk-zipped.apk")
    Signature.zipalign(unsignedApk, unzipApk)

    //签名，可以用jarsigner，这里用的apksigner
    val signedApk = File("result/apk-signed.apk")
    Signature.signatureWithApksigner(unzipApk, signedApk)

}