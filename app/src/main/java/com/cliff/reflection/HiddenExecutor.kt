package com.cliff.reflection

import com.cliff.hidden.HiddenApi
import com.cliff.reflection.common.IExecutor
import com.cliff.reflection.common.Section
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

class HiddenExecutor : IExecutor.BaseExecutor() {
    private val defaultExecutor = IExecutor.DefaultExecutor()
    override fun findField(clazz: Class<*>, isStatic: Boolean, fieldName: String): Field {
        return if (isStatic) {
            HiddenApi.getStaticFiled(fieldName, clazz)?.apply { isAccessible = true }!!
        } else {
            HiddenApi.getInstanceFiled(fieldName, clazz)?.apply { isAccessible = true }!!
        }
    }

    override fun findMethod(clazz: Class<*>, methodName: String, isStatic: Boolean, vararg parameterTypes: Class<*>): Method {
        return HiddenApi.getDeclaredMethod(clazz, methodName, *parameterTypes).apply { isAccessible = true }
    }

    override fun findConstructor(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*> {
        return HiddenApi.getDeclaredConstructor(clazz, *parameterTypes).apply { isAccessible = true }
    }

    override fun setField(obj: Any?, clazz: Class<*>, isStatic: Boolean, fieldName: String, newValue: Any?) {
        try {
            defaultExecutor.setField(obj, clazz, isStatic, fieldName, newValue)
        } catch (exp: NoSuchFieldException) {
            super.setField(obj, clazz, isStatic, fieldName, newValue)
        }
    }

    override fun getField(obj: Any?, clazz: Class<*>, isStatic: Boolean, fieldName: String): Any? {
        return try {
            defaultExecutor.getField(obj, clazz, isStatic, fieldName)
        } catch (exp: NoSuchFieldException) {
            super.getField(obj, clazz, isStatic, fieldName)
        }
    }

    override fun invokeMethod(obj: Any?, clazz: Class<*>, methodName: String, isStatic: Boolean, vararg sections: Section): Any? {
        return try {
            defaultExecutor.invokeMethod(obj, clazz, methodName, isStatic, *sections)
        } catch (exp: NoSuchMethodException) {
            super.invokeMethod(obj, clazz, methodName, isStatic, *sections)
        }
    }

    override fun newInstance(clazz: Class<*>, vararg sections: Section): Any? {
        return try {
            defaultExecutor.newInstance(clazz, *sections)
        } catch (exp: NoSuchMethodException) {
            super.newInstance(clazz, *sections)
        }
    }
}