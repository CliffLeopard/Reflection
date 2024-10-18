package com.cliff.reflection.common

import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

interface IExecutor {
    @Throws(NoSuchFieldException::class)
    fun accessField(clazz: Class<*>, isStatic: Boolean, fieldName: String): Field

    fun setField(obj: Any?, clazz: Class<*>, isStatic: Boolean, fieldName: String, newValue: Any?)

    fun getField(obj: Any?, clazz: Class<*>, isStatic: Boolean, fieldName: String): Any?

    fun invokeMethod(obj: Any?, clazz: Class<*>, methodName: String, isStatic: Boolean = false, vararg sections: Section): Any?

    fun newInstance(clazz: Class<*>, vararg sections: Section): Any?

    fun findField(clazz: Class<*>, isStatic: Boolean, fieldName: String): Field
    fun findMethod(clazz: Class<*>, methodName: String, isStatic: Boolean = false, vararg parameterTypes: Class<*>): Method
    fun findConstructor(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*>

    abstract class BaseExecutor : IExecutor {
        private val methods = mutableMapOf<String, WeakReference<Method?>>()
        private val fields = mutableMapOf<String, WeakReference<Field?>>()
        private val constructors = mutableMapOf<String, WeakReference<Constructor<*>?>>()

        @Throws(NoSuchFieldException::class)
        override fun accessField(clazz: Class<*>, isStatic: Boolean, fieldName: String): Field {
            val fieldKey = clazz.canonicalName + isStatic + fieldName
            if (fields[fieldKey] == null || fields[fieldKey]!!.get() == null) {
                fields[fieldKey] = WeakReference(findField(clazz, isStatic, fieldName))
            }
            return fields[fieldKey]!!.get()!!
        }

        @Throws(NoSuchFieldException::class)
        override fun setField(obj: Any?, clazz: Class<*>, isStatic: Boolean, fieldName: String, newValue: Any?) {
            accessField(clazz, isStatic, fieldName).set(if (isStatic) null else obj, newValue)
        }

        @Throws(NoSuchFieldException::class)
        override fun getField(obj: Any?, clazz: Class<*>, isStatic: Boolean, fieldName: String): Any? {
            return accessField(clazz, isStatic, fieldName).get(if (isStatic) null else obj)
        }

        @Throws(NoSuchMethodException::class)
        override fun invokeMethod(obj: Any?, clazz: Class<*>, methodName: String, isStatic: Boolean, vararg sections: Section): Any? {
            val parameterTypes = sections.map { it.type }.toTypedArray()
            val parameters = sections.map { it.data }.toTypedArray()
            val methodKey = clazz.canonicalName + methodName + isStatic + parameterTypes.joinToString()
            if (methods[methodKey] == null || methods[methodKey]!!.get() == null) {
                methods[methodKey] = WeakReference(findMethod(clazz, methodName, isStatic, *parameterTypes))
            }
            return methods[methodKey]!!.get()!!.invoke(if (isStatic) null else obj, *parameters)
        }

        @Throws(NoSuchMethodException::class)
        override fun newInstance(clazz: Class<*>, vararg sections: Section): Any? {
            val parameterTypes = sections.map { it.type }.toTypedArray()
            val parameters = sections.map { it.data }.toTypedArray()
            val constructorKey = clazz.canonicalName + parameterTypes.joinToString()
            return try {
                if (constructors[constructorKey] == null || constructors[constructorKey]!!.get() == null) {
                    constructors[constructorKey] = WeakReference(findConstructor(clazz, *parameterTypes))
                }
                constructors[constructorKey]!!.get()!!.newInstance(*parameters)
            } catch (exp: Exception) {
                throw NoSuchMethodException()
            }
        }
    }

    class DefaultExecutor : BaseExecutor() {
        override fun findField(clazz: Class<*>, isStatic: Boolean, fieldName: String): Field {
            return clazz.getDeclaredField(fieldName).apply { isAccessible = true }
        }

        override fun findMethod(clazz: Class<*>, methodName: String, isStatic: Boolean, vararg parameterTypes: Class<*>): Method {
            return clazz.getDeclaredMethod(methodName, *parameterTypes).apply {
                isAccessible = true
            }
        }

        override fun findConstructor(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*> {
            return clazz.getConstructor(*parameterTypes).apply { isAccessible = true }
        }
    }
}