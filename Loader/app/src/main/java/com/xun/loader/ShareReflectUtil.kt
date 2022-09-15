package com.xun.loader

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*

object ShareReflectUtil {

    /**
     * 从 instance 到其父类 找 name 属性
     */
    fun findField(instance: Any?, name: String): Field {
        var clazz: Class<*>? = instance?.javaClass
        while (clazz != null) {
            try {
                //查找当前类的 属性(不包括父类)
                val field = clazz.getDeclaredField(name)
                if (!field.isAccessible) {
                    field.isAccessible = true
                }
                return field
            } catch (e: NoSuchFieldException) {
                // ignore and search next
            }
            clazz = clazz.superclass
        }
        throw NoSuchFieldException("Field " + name + " not found in " + instance?.javaClass)
    }

    fun findMethod(instance: Any, name: String, vararg parameterTypes: Class<*>): Method {
        var clazz: Class<*>? = instance.javaClass
        while (clazz != null) {
            try {
                val method = clazz.getDeclaredMethod(name, *parameterTypes)
                if (!method.isAccessible) {
                    method.isAccessible = true
                }
                return method
            } catch (e: NoSuchMethodException) {
                // ignore and search next
            }
            clazz = clazz.superclass
        }
        throw NoSuchMethodException(
            "Method "
                    + name
                    + " with parameters "
                    + Arrays.asList(*parameterTypes)
                    + " not found in " + instance.javaClass
        )
    }

    @Throws(
        NoSuchFieldException::class,
        IllegalArgumentException::class,
        IllegalAccessException::class
    )
    fun expandFieldArray(instance: Any?, fieldName: String?, patchElements: Array<Any?>) {
        //拿到 classloader中的dexelements 数组
        val dexElementsField = findField(
            instance,
            fieldName!!
        )
        //old Element[]
        val dexElements = dexElementsField[instance] as Array<Any>


        //合并后的数组
        val newElements = java.lang.reflect.Array.newInstance(
            dexElements.javaClass.componentType,
            dexElements.size + patchElements.size
        ) as Array<Any>

        // 先拷贝新数组
        System.arraycopy(patchElements, 0, newElements, 0, patchElements.size)
        System.arraycopy(dexElements, 0, newElements, patchElements.size, dexElements.size)

        //修改 classLoader中 pathList的 dexelements
        dexElementsField[instance] = newElements
    }
}