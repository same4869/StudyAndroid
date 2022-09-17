package com.xun.decodelib

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*

object ApplicationHelper {

    //解包与解密
    fun attachBaseContext(application: Context) {
        AES.init(getPassword())
        application.apply {
            val apkFile = File(applicationInfo.sourceDir)
            //data/data/包名/files/fake_apk/
            val unZipFile: File = getDir("fake_apk", Context.MODE_PRIVATE)
            val app = File(unZipFile, "app")
            if (!app.exists()) {
                Zip.unZip(apkFile, app)
                val files = app.listFiles()
                for (file in files) {
                    val name = file.name
                    if (name == "classes.dex") {
                        //壳dex，无需解密
                    } else if (name.endsWith(".dex")) {
                        //源dex，做文件级别的解密后重写入
                        try {
                            val bytes: ByteArray = getBytes(file)
                            val fos = FileOutputStream(file)
                            val decrypt = AES.decrypt(bytes)
                            //                        fos.write(bytes);
                            fos.write(decrypt)
                            fos.flush()
                            fos.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            val list: MutableList<File> = mutableListOf()
            Log.d("kkkkkkkk", "attachBaseContext applist ->${Arrays.toString(app.listFiles())}")
            for (file in app.listFiles()) {
                if (file.name.endsWith(".dex")) {
                    list.add(file)
                }
            }

            Log.d("kkkkkkkk", "list ->$list")
            try {
                //需要重新组装dex相关格式
                V19.install(classLoader, list, unZipFile)
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            }
        }

    }

    @Throws(NoSuchFieldException::class)
    private fun findField(instance: Any, name: String): Field {
        var clazz: Class<*>? = instance.javaClass
        while (clazz != null) {
            try {
                val e = clazz.getDeclaredField(name)
                if (!e.isAccessible) {
                    e.isAccessible = true
                }
                return e
            } catch (var4: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        throw NoSuchFieldException("Field " + name + " not found in " + instance.javaClass)
    }

    @Throws(NoSuchMethodException::class)
    private fun findMethod(instance: Any, name: String, vararg parameterTypes: Class<*>): Method {
        var clazz: Class<*>? = instance.javaClass
        //        Method[] declaredMethods = clazz.getDeclaredMethods();
//        System.out.println("  findMethod ");
//        for (Method m : declaredMethods) {
//            System.out.print(m.getName() + "  : ");
//            Class<?>[] parameterTypes1 = m.getParameterTypes();
//            for (Class clazz1 : parameterTypes1) {
//                System.out.print(clazz1.getName() + " ");
//            }
//            System.out.println("");
//        }
        while (clazz != null) {
            try {
                val e = clazz.getDeclaredMethod(name, *parameterTypes)
                if (!e.isAccessible) {
                    e.isAccessible = true
                }
                return e
            } catch (var5: NoSuchMethodException) {
                clazz = clazz.superclass
            }
        }
        throw NoSuchMethodException("Method " + name + " with parameters " + Arrays.asList(*parameterTypes) + " not found in " + instance.javaClass)
    }

    @Throws(
        NoSuchFieldException::class,
        java.lang.IllegalArgumentException::class,
        IllegalAccessException::class
    )
    private fun expandFieldArray(instance: Any, fieldName: String, extraElements: Array<Any>) {
        val jlrField = findField(instance, fieldName)
        val original = jlrField[instance] as Array<Any?>
        val combined = java.lang.reflect.Array.newInstance(
            original.javaClass
                .componentType, original.size + extraElements.size
        ) as Array<Any?>
        System.arraycopy(original, 0, combined, 0, original.size)
        System.arraycopy(extraElements, 0, combined, original.size, extraElements.size)
        jlrField[instance] = combined
    }


    private fun getPassword(): String {
        return "abcdefghijklmnop"
    }

    @Throws(java.lang.Exception::class)
    private fun getBytes(file: File): ByteArray {
        val r = RandomAccessFile(file, "r")
        val buffer = ByteArray(r.length().toInt())
        r.readFully(buffer)
        r.close()
        return buffer
    }

    object V19 {
        @Throws(
            IllegalArgumentException::class,
            IllegalAccessException::class,
            NoSuchFieldException::class,
            InvocationTargetException::class,
            NoSuchMethodException::class
        )
        fun install(
            loader: ClassLoader, additionalClassPathEntries: List<File?>,
            optimizedDirectory: File
        ) {
            val pathListField: Field = findField(loader, "pathList")
            val dexPathList = pathListField[loader]
            val suppressedExceptions: ArrayList<IOException> = arrayListOf()
            if (Build.VERSION.SDK_INT >= 23) {
                expandFieldArray(
                    dexPathList, "dexElements", makePathElements(
                        dexPathList,
                        ArrayList(additionalClassPathEntries),
                        optimizedDirectory,
                        suppressedExceptions
                    )
                )
            } else {
                expandFieldArray(
                    dexPathList, "dexElements", makeDexElements(
                        dexPathList,
                        ArrayList(additionalClassPathEntries),
                        optimizedDirectory,
                        suppressedExceptions
                    )
                )
            }
            if (suppressedExceptions.size > 0) {
                val suppressedExceptionsField: Iterator<*> = suppressedExceptions.iterator()
                while (suppressedExceptionsField.hasNext()) {
                    val dexElementsSuppressedExceptions =
                        suppressedExceptionsField.next() as IOException
                    Log.w(
                        "MultiDex", "Exception in makeDexElement",
                        dexElementsSuppressedExceptions
                    )
                }
                val suppressedExceptionsField1: Field = findField(
                    loader,
                    "dexElementsSuppressedExceptions"
                )
                var dexElementsSuppressedExceptions1 =
                    suppressedExceptionsField1[loader] as Array<IOException?>
                dexElementsSuppressedExceptions1 = if (dexElementsSuppressedExceptions1 == null) {
                    suppressedExceptions
                        .toTypedArray() as Array<IOException?>
                } else {
                    val combined = arrayOfNulls<IOException>(
                        suppressedExceptions.size +
                                dexElementsSuppressedExceptions1.size
                    )
                    suppressedExceptions.toArray(combined)
                    System.arraycopy(
                        dexElementsSuppressedExceptions1, 0, combined,
                        suppressedExceptions.size, dexElementsSuppressedExceptions1.size
                    )
                    combined
                }
                suppressedExceptionsField1[loader] = dexElementsSuppressedExceptions1
            }
        }

        @Throws(
            IllegalAccessException::class,
            InvocationTargetException::class,
            NoSuchMethodException::class
        )
        fun makeDexElements(
            dexPathList: Any,
            files: ArrayList<File?>, optimizedDirectory: File,
            suppressedExceptions: ArrayList<IOException>
        ): Array<Any> {
            val makeDexElements: Method = findMethod(
                dexPathList, "makeDexElements", *arrayOf<Class<*>>(
                    ArrayList::class.java,
                    File::class.java,
                    ArrayList::class.java
                )
            )
            return makeDexElements.invoke(
                dexPathList, *arrayOf<Any>(
                    files,
                    optimizedDirectory, suppressedExceptions
                )
            ) as Array<Any>
        }
    }

    /**
     * A wrapper around
     * `private static final dalvik.system.DexPathList#makePathElements`.
     */
    @Throws(
        IllegalAccessException::class,
        InvocationTargetException::class,
        NoSuchMethodException::class
    )
    private fun makePathElements(
        dexPathList: Any, files: ArrayList<File?>, optimizedDirectory: File,
        suppressedExceptions: ArrayList<IOException>
    ): Array<Any> {
        val makePathElements: Method = try {
            findMethod(
                dexPathList, "makePathElements",
                MutableList::class.java,
                File::class.java,
                MutableList::class.java
            )
        } catch (e: NoSuchMethodException) {
            Log.e(
                "kkkkkkkk",
                "NoSuchMethodException: makePathElements(List,File,List) failure"
            )
            try {
                findMethod(
                    dexPathList, "makePathElements",
                    ArrayList::class.java,
                    File::class.java,
                    ArrayList::class.java
                )
            } catch (e1: NoSuchMethodException) {
                Log.e(
                    "kkkkkkkk",
                    "NoSuchMethodException: makeDexElements(ArrayList,File,ArrayList) failure"
                )
                return try {
                    Log.e("kkkkkkkk", "NoSuchMethodException: try use v19 instead")
                    V19.makeDexElements(
                        dexPathList,
                        files,
                        optimizedDirectory,
                        suppressedExceptions
                    )
                } catch (e2: NoSuchMethodException) {
                    Log.e(
                        "kkkkkkkk",
                        "NoSuchMethodException: makeDexElements(List,File,List) failure"
                    )
                    throw e2
                }
            }
        }
        return makePathElements.invoke(
            dexPathList,
            files,
            optimizedDirectory,
            suppressedExceptions
        ) as Array<Any>
    }
}