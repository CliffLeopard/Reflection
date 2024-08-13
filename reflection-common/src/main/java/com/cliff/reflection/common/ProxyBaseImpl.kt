package com.cliff.reflection.common

open class ProxyBaseImpl protected constructor(originCls: String) {
    private val clz: Class<*> by lazy {
        Class.forName(originCls)
    }

    fun setFiled(target: Any?, fieldName: String, `value`: Any?, type: String, isStatic: Boolean) {
        Reflect.setField(target, clz, isStatic, fieldName, `value`)
    }

    fun <T> getFiled(target: Any?, fieldName: String, type: String, isStatic: Boolean): T? {
        return Reflect.getField(target, clz, isStatic, fieldName) as T?
    }

    fun <T> invokeMethod(target: Any?, methodName: String, isStatic: Boolean, vararg sections: Section): T {
        return Reflect.invokeMethod(target, clz, methodName, isStatic, *sections) as T
    }

    fun <T> invokeConstructor(classType: String, vararg sections: Section): T {
        return Reflect.newInstance(Class.forName(classType), *sections) as T
    }
}