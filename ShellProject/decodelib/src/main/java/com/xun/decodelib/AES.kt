package com.xun.decodelib

import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec

/**
 * AES加密算法
 */
object AES {
    private const val algorithmStr = "AES/ECB/PKCS5Padding"

    private lateinit var encryptCipher: Cipher
    private lateinit var decryptCipher: Cipher

    fun init(password:String){
        try {
            // 生成一个实现指定转换的 Cipher 对象。
            encryptCipher = Cipher.getInstance(algorithmStr)
            decryptCipher = Cipher.getInstance(algorithmStr) // algorithmStr
            val keyStr = password.toByteArray()
            val key = SecretKeySpec(keyStr, "AES")
            encryptCipher.init(Cipher.ENCRYPT_MODE, key)
            decryptCipher.init(Cipher.DECRYPT_MODE, key)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        }
    }

    fun encrypt(content: ByteArray?): ByteArray? {
        try {
            return encryptCipher.doFinal(content)
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        }
        return null
    }

    fun decrypt(content: ByteArray?): ByteArray? {
        try {
            return decryptCipher.doFinal(content)
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        }
        return null
    }
}