package com.xun.loader

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.Log
import dalvik.system.DexFile
import dalvik.system.PathClassLoader
import java.io.File

object NewClassLoaderInjector {

    class DispatchClassLoader(applicationClassName: String, oldClassLoader: ClassLoader) :
        ClassLoader(getSystemClassLoader()) {
        private val mApplicationClassName: String
        private val mOldClassLoader: ClassLoader

        private var mNewClassLoader: ClassLoader? = null

        init {
            mApplicationClassName = applicationClassName
            mOldClassLoader = oldClassLoader
        }

        private val mCallFindClassOfLeafDirectly: ThreadLocal<Boolean> =
            object : ThreadLocal<Boolean>() {
                override fun initialValue(): Boolean {
                    return false
                }
            }

        fun setNewClassLoader(classLoader: ClassLoader) {
            mNewClassLoader = classLoader
        }

        override fun findClass(name: String): Class<*>? {
            Log.d("kkkkkkkk", "find:$name")

            if (mCallFindClassOfLeafDirectly.get() == true) {
                return null
            }
            // 1、Application类不需要修复，使用原本的类加载器获得
            if (name == mApplicationClassName) {
                return findClass(mOldClassLoader, name)
            }
            // 2、加载热修复框架的类 因为不需要修复，就用原本的类加载器获得
            if (name.startsWith("com.xun.patch.")) {
                return findClass(mOldClassLoader, name)
            }
            return try {
                findClass(mNewClassLoader!!, name)
            } catch (e: ClassNotFoundException) {
                findClass(mOldClassLoader, name)
            }
        }

        private fun findClass(classLoader: ClassLoader, name: String): Class<*> {
            try {
                //双亲委托，所以可能会stackoverflow死循环，防止这个情况
                mCallFindClassOfLeafDirectly.set(true)
                return classLoader.loadClass(name)
            } finally {
                mCallFindClassOfLeafDirectly.set(false)
            }
        }
    }

    fun inject(
        application: Application,
        oldClassLoader: ClassLoader,
        patchs: List<File>
    ): ClassLoader {
        // 分发加载任务的加载器，作为我们自己的加载器的父加载器
        val dispatchClassLoader = DispatchClassLoader(application::class.java.name, oldClassLoader)
        //创建我们自己的加载器
        val newClassLoader =
            createNewClassLoader(application, oldClassLoader, dispatchClassLoader, patchs)
        dispatchClassLoader.setNewClassLoader(newClassLoader)
        doInject(application, newClassLoader)
        return newClassLoader
    }

    private fun createNewClassLoader(
        context: Context,
        oldClassLoader: ClassLoader,
        dispatchClassLoader: ClassLoader,
        patchs: List<File>
    ): ClassLoader {
        //得到pathList
        val pathListField = ShareReflectUtil.findField(oldClassLoader, "pathList")
        val oldPathList = pathListField[oldClassLoader]

        //dexElements
        val dexElementsField = ShareReflectUtil.findField(oldPathList, "dexElements")
        val oldDexElements = dexElementsField[oldPathList] as Array<Any>

        //从Element上得到 dexFile
        val dexFileField = ShareReflectUtil.findField(oldDexElements[0], "dexFile")
        // 获得原始的dexPath用于构造classloader

        // 获得原始的dexPath用于构造classloader
        val dexPathBuilder = StringBuilder()
        val packageName = context.packageName
        var isFirstItem = true
        for (patch in patchs) {
            if (isFirstItem) {
                isFirstItem = false
            } else {
                dexPathBuilder.append(File.pathSeparator)
            }
            dexPathBuilder.append(patch.absolutePath)
        }
        for (oldDexElement in oldDexElements) {
            var dexPath: String? = null
            val dexFile = dexFileField[oldDexElement] as DexFile
            if (dexFile != null) {
                dexPath = dexFile.name
            }
            if (dexPath == null || dexPath.isEmpty()) {
                continue
            }
            if (!dexPath.contains("/$packageName")) {
                continue
            }
            if (isFirstItem) {
                isFirstItem = false
            } else {
                dexPathBuilder.append(File.pathSeparator)
            }
            dexPathBuilder.append(dexPath)
        }
        val combinedDexPath = dexPathBuilder.toString()

        //  app的native库（so） 文件目录 用于构造classloader
        val nativeLibraryDirectoriesField =
            ShareReflectUtil.findField(oldPathList, "nativeLibraryDirectories")
        val oldNativeLibraryDirectories = nativeLibraryDirectoriesField[oldPathList] as List<File>


        val libraryPathBuilder = StringBuilder()
        isFirstItem = true
        for (libDir in oldNativeLibraryDirectories) {
            if (libDir == null) {
                continue
            }
            if (isFirstItem) {
                isFirstItem = false
            } else {
                libraryPathBuilder.append(File.pathSeparator)
            }
            libraryPathBuilder.append(libDir.absolutePath)
        }

        val combinedLibraryPath = libraryPathBuilder.toString()

        //创建自己的类加载器

        //创建自己的类加载器
        val result: ClassLoader =
            PathClassLoader(combinedDexPath, combinedLibraryPath, dispatchClassLoader)
        ShareReflectUtil.findField(oldPathList, "definingContext")[oldPathList] = result
        ShareReflectUtil.findField(result, "parent")[result] = dispatchClassLoader
        return result
    }

    private fun doInject(application: Application, classLoader: ClassLoader) {
        Thread.currentThread().contextClassLoader = classLoader

        val baseContext = ShareReflectUtil.findField(application, "mBase")[application] as Context
        val basePackageInfo = ShareReflectUtil.findField(baseContext, "mPackageInfo")[baseContext]
        ShareReflectUtil.findField(basePackageInfo, "mClassLoader")[basePackageInfo] = classLoader

        if (Build.VERSION.SDK_INT < 27) {
            val res: Resources = application.resources
            try {
                ShareReflectUtil.findField(res, "mClassLoader")[res] = classLoader
                val drawableInflater = ShareReflectUtil.findField(res, "mDrawableInflater")[res]
                if (drawableInflater != null) {
                    ShareReflectUtil.findField(drawableInflater, "mClassLoader")[drawableInflater] =
                        classLoader
                }
            } catch (ignored: Throwable) {
                // Ignored.
            }
        }
    }

}