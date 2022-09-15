package com.xun.loader

import android.app.Application
import android.os.Build
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException

object HotFix {
    fun installPatch(application: Application, patch: File) {
        //获得classloader
        val classLoader = application.classLoader

        val files = arrayListOf<File>()
        if (patch.exists()) {
            files.add(patch)
        }
        val dexOptDir = application.cacheDir
        //首先是N以上的处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                NewClassLoaderInjector.inject(application, classLoader, files)
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
        } else {
            try {
                //23 6.0及以上
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    V23.install(classLoader, files, dexOptDir)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    V19.install(classLoader, files, dexOptDir) //4.4以上
                } else {  // >= 14
                    V14.install(classLoader, files, dexOptDir)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    object V23 {
        @Throws(
            IllegalArgumentException::class,
            IllegalAccessException::class,
            NoSuchFieldException::class,
            InvocationTargetException::class,
            NoSuchMethodException::class,
            IOException::class
        )
        fun install(
            loader: ClassLoader, additionalClassPathEntries: List<File>,
            optimizedDirectory: File
        ) {
            //找到 pathList
            val pathListField = ShareReflectUtil.findField(loader, "pathList")
            val dexPathList = pathListField[loader]
            val suppressedExceptions = ArrayList<IOException>()
            // 从 pathList找到 makePathElements 方法并执行
            // 得到补丁创建的 Element[]
            val patchElements = makePathElements(
                dexPathList,
                ArrayList(additionalClassPathEntries), optimizedDirectory,
                suppressedExceptions
            )

            //将原本的 dexElements 与 makePathElements生成的数组合并
            ShareReflectUtil.expandFieldArray(dexPathList, "dexElements", patchElements)
            if (suppressedExceptions.size > 0) {
                for (e in suppressedExceptions) {
                    throw e
                }
            }
        }

        /**
         * 把dex转化为Element数组
         */
        @Throws(
            IllegalAccessException::class,
            InvocationTargetException::class,
            NoSuchMethodException::class
        )
        private fun makePathElements(
            dexPathList: Any, files: ArrayList<File>, optimizedDirectory: File,
            suppressedExceptions: ArrayList<IOException>
        ): Array<Any?> {
            //通过阅读android6、7、8、9源码，都存在makePathElements方法
            val makePathElements = ShareReflectUtil.findMethod(
                dexPathList, "makePathElements",
                MutableList::class.java, File::class.java,
                MutableList::class.java
            )
            return makePathElements.invoke(
                dexPathList, files, optimizedDirectory,
                suppressedExceptions
            ) as Array<Any?>
        }
    }

    object V19 {
        @Throws(
            IllegalArgumentException::class,
            IllegalAccessException::class,
            NoSuchFieldException::class,
            InvocationTargetException::class,
            NoSuchMethodException::class,
            IOException::class
        )
        fun install(
            loader: ClassLoader, additionalClassPathEntries: List<File>,
            optimizedDirectory: File
        ) {
            val pathListField = ShareReflectUtil.findField(loader, "pathList")
            val dexPathList = pathListField[loader]
            val suppressedExceptions = ArrayList<IOException>()
            ShareReflectUtil.expandFieldArray(
                dexPathList, "dexElements",
                makeDexElements(
                    dexPathList,
                    ArrayList(additionalClassPathEntries), optimizedDirectory,
                    suppressedExceptions
                )
            )
            if (suppressedExceptions.size > 0) {
                for (e in suppressedExceptions) {
                    throw e
                }
            }
        }

        @Throws(
            IllegalAccessException::class,
            InvocationTargetException::class,
            NoSuchMethodException::class
        )
        private fun makeDexElements(
            dexPathList: Any, files: ArrayList<File>, optimizedDirectory: File,
            suppressedExceptions: ArrayList<IOException>
        ): Array<Any?> {
            val makeDexElements = ShareReflectUtil.findMethod(
                dexPathList, "makeDexElements",
                ArrayList::class.java, File::class.java,
                ArrayList::class.java
            )
            return makeDexElements.invoke(
                dexPathList, files, optimizedDirectory,
                suppressedExceptions
            ) as Array<Any?>
        }
    }

    /**
     * 14, 15, 16, 17, 18.
     */
    object V14 {
        @Throws(
            IllegalArgumentException::class,
            IllegalAccessException::class,
            NoSuchFieldException::class,
            InvocationTargetException::class,
            NoSuchMethodException::class
        )
        fun install(
            loader: ClassLoader, additionalClassPathEntries: List<File>,
            optimizedDirectory: File
        ) {
            val pathListField = ShareReflectUtil.findField(loader, "pathList")
            val dexPathList = pathListField[loader]
            ShareReflectUtil.expandFieldArray(
                dexPathList, "dexElements",
                makeDexElements(
                    dexPathList,
                    ArrayList(additionalClassPathEntries), optimizedDirectory
                )
            )
        }

        @Throws(
            IllegalAccessException::class,
            InvocationTargetException::class,
            NoSuchMethodException::class
        )
        private fun makeDexElements(
            dexPathList: Any, files: ArrayList<File>, optimizedDirectory: File
        ): Array<Any?> {
            val makeDexElements = ShareReflectUtil.findMethod(
                dexPathList, "makeDexElements",
                ArrayList::class.java,
                File::class.java
            )
            return makeDexElements.invoke(dexPathList, files, optimizedDirectory) as Array<Any?>
        }
    }
}