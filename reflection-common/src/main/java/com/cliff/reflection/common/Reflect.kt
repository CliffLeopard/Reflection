package com.cliff.reflection.common

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

object Reflect : IExecutor {
    private var executor: IExecutor = IExecutor.DefaultExecutor()

    fun registerExecutor(exe: IExecutor) {
        executor = exe
    }

    override fun accessField(clazz: Class<*>, isStatic: Boolean, fieldName: String): Field {
        return executor.accessField(clazz, isStatic, fieldName)
    }

    override fun setField(obj: Any?, clazz: Class<*>, isStatic: Boolean, fieldName: String, newValue: Any?) {
        executor.setField(obj, clazz, isStatic, fieldName, newValue)
    }

    override fun getField(obj: Any?, clazz: Class<*>, isStatic: Boolean, fieldName: String): Any? {
        return executor.getField(obj, clazz, isStatic, fieldName)
    }

    override fun invokeMethod(obj: Any?, clazz: Class<*>, methodName: String, isStatic: Boolean, vararg sections: Section): Any? {
        return executor.invokeMethod(obj, clazz, methodName, isStatic, *sections)
    }

    override fun newInstance(clazz: Class<*>, vararg sections: Section): Any? {
        return executor.newInstance(clazz, *sections)
    }

    override fun findField(clazz: Class<*>, isStatic: Boolean, fieldName: String): Field {
        return executor.findField(clazz, isStatic, fieldName)
    }

    override fun findMethod(clazz: Class<*>, methodName: String, isStatic: Boolean, vararg parameterTypes: Class<*>): Method {
        return executor.findMethod(clazz, methodName, isStatic, *parameterTypes)
    }

    override fun findConstructor(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*> {
        return executor.findConstructor(clazz, *parameterTypes)
    }
}