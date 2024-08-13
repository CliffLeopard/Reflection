/*
 * Copyright (C) 2021-2023 LSPosed
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cliff.hidden

import sun.misc.Unsafe
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandleInfo
import java.lang.invoke.MethodHandles
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import com.cliff.hidden.Helper.NeverCall
import com.cliff.hidden.Helper.HandleInfo
import com.cliff.hidden.Helper.InvokeStub

@Suppress("DiscouragedPrivateApi")
object HiddenApi {
    private val unsafe: Unsafe by lazy {
        Unsafe::class.java.getDeclaredMethod("getUnsafe").invoke(null) as Unsafe
    }
    private val methodOffset: Long by lazy {
        unsafe.objectFieldOffset(Helper.Executable::class.java.getDeclaredField("artMethod"))
    }
    private val classOffset: Long by lazy {
        unsafe.objectFieldOffset(Helper.Executable::class.java.getDeclaredField("declaringClass"))
    }
    private val artOffset: Long by lazy {
        unsafe.objectFieldOffset(Helper.MethodHandle::class.java.getDeclaredField("artFieldOrMethod"))
    }
    private val infoOffset: Long by lazy {
        unsafe.objectFieldOffset(Helper.MethodHandleImpl::class.java.getDeclaredField("info"))
    }
    private val methodsOffset: Long by lazy {
        unsafe.objectFieldOffset(Helper.Class::class.java.getDeclaredField("methods"))
    }
    private val iFieldOffset: Long by lazy {
        unsafe.objectFieldOffset(Helper.Class::class.java.getDeclaredField("iFields"))
    }
    private val sFieldOffset: Long by lazy {
        unsafe.objectFieldOffset(Helper.Class::class.java.getDeclaredField("sFields"))
    }
    private val memberOffset: Long by lazy {
        unsafe.objectFieldOffset(HandleInfo::class.java.getDeclaredField("member"))
    }

    private val artMethodSize: Long
    private val artMethodBias: Long
    private val artFieldSize: Long
    private val artFieldBias: Long
    private val signaturePrefixes: MutableSet<String> = HashSet()

    init {
        try {
            val mA = NeverCall::class.java.getDeclaredMethod("a")
            val mB = NeverCall::class.java.getDeclaredMethod("b")
            mA.isAccessible = true
            mB.isAccessible = true
            val mhA = MethodHandles.lookup().unreflect(mA)
            val mhB = MethodHandles.lookup().unreflect(mB)
            val aAddr = unsafe.getLong(mhA, artOffset)
            val bAddr = unsafe.getLong(mhB, artOffset)
            val aMethods = unsafe.getLong(NeverCall::class.java, methodsOffset)
            artMethodSize = bAddr - aAddr

            artMethodBias = aAddr - aMethods - artMethodSize
            val fI = NeverCall::class.java.getDeclaredField("i")
            val fJ = NeverCall::class.java.getDeclaredField("j")
            fI.isAccessible = true
            fJ.isAccessible = true
            val mhI = MethodHandles.lookup().unreflectGetter(fI)
            val mhJ = MethodHandles.lookup().unreflectGetter(fJ)
            val iAddr = unsafe.getLong(mhI, artOffset)
            val jAddr = unsafe.getLong(mhJ, artOffset)
            val iFields = unsafe.getLong(NeverCall::class.java, iFieldOffset)
            artFieldSize = jAddr - iAddr

            artFieldBias = iAddr - iFields
        } catch (e: ReflectiveOperationException) {
            throw ExceptionInInitializerError(e)
        }
    }

    private fun checkArgsForInvokeMethod(params: Array<Class<*>>, args: Array<out Any?>): Boolean {
        if (params.size != args.size) return false
        for (i in params.indices) {
            if (params[i].isPrimitive) {
                if (params[i] == Int::class.javaPrimitiveType && args[i] !is Int) return false
                else if (params[i] == Byte::class.javaPrimitiveType && args[i] !is Byte) return false
                else if (params[i] == Char::class.javaPrimitiveType && args[i] !is Char) return false
                else if (params[i] == Boolean::class.javaPrimitiveType && args[i] !is Boolean) return false
                else if (params[i] == Double::class.javaPrimitiveType && args[i] !is Double) return false
                else if (params[i] == Float::class.javaPrimitiveType && args[i] !is Float) return false
                else if (params[i] == Long::class.javaPrimitiveType && args[i] !is Long) return false
                else if (params[i] == Short::class.javaPrimitiveType && args[i] !is Short) return false
            } else if (args[i] != null && !params[i].isInstance(args[i])) return false
        }
        return true
    }

    @Throws(
        NoSuchMethodException::class,
        IllegalAccessException::class,
        InvocationTargetException::class,
        InstantiationException::class
    )
    fun newInstance(clazz: Class<*>, vararg initargs: Any?): Any {
        val stub = InvokeStub::class.java.getDeclaredMethod("invoke", Array<Any>::class.java)
        val ctor: Constructor<*> = InvokeStub::class.java.getDeclaredConstructor(
            Array<Any>::class.java
        )
        ctor.isAccessible = true
        val methods = unsafe.getLong(clazz, methodsOffset)
        if (methods == 0L) throw NoSuchMethodException("Cannot find matching constructor")
        val numMethods = unsafe.getInt(methods)
        for (i in 0 until numMethods) {
            val method = methods + i * artMethodSize + artMethodBias
            unsafe.putLong(stub, methodOffset, method)


            if ("<init>" == stub.name) {
                unsafe.putLong(ctor, methodOffset, method)
                unsafe.putObject(ctor, classOffset, clazz)
                val params = ctor.parameterTypes
                if (checkArgsForInvokeMethod(params, initargs)) return ctor.newInstance(*initargs)
            }
        }
        throw NoSuchMethodException("Cannot find matching constructor")
    }

    @Throws(
        NoSuchMethodException::class,
        InvocationTargetException::class,
        IllegalAccessException::class
    )
    fun invoke(clazz: Class<*>, obj: Any?, methodName: String, vararg args: Any?): Any? {
        require(!(obj != null && !clazz.isInstance(obj))) { "this object is not an instance of the given class" }
        val stub = InvokeStub::class.java.getDeclaredMethod("invoke", Array<Any>::class.java)
        stub.isAccessible = true
        val methods = unsafe.getLong(clazz, methodsOffset)
        if (methods == 0L) throw NoSuchMethodException("Cannot find matching method")
        val numMethods = unsafe.getInt(methods)
        for (i in 0 until numMethods) {
            val method = methods + i * artMethodSize + artMethodBias
            unsafe.putLong(stub, methodOffset, method)
            if (methodName == stub.name) {
                val params = stub.parameterTypes
                if (checkArgsForInvokeMethod(params, args)) return stub.invoke(obj, *args)
            }
        }
        throw NoSuchMethodException("Cannot find matching method")
    }

    private fun getDeclaredMethods(clazz: Class<*>): List<Executable> {
        val list = ArrayList<Executable>()
        if (clazz.isPrimitive || clazz.isArray) return list
        val mh: MethodHandle
        try {
            val mA = NeverCall::class.java.getDeclaredMethod("a")
            mA.isAccessible = true
            mh = MethodHandles.lookup().unreflect(mA)
        } catch (e: NoSuchMethodException) {
            return list
        } catch (e: IllegalAccessException) {
            return list
        }
        val methods = unsafe.getLong(clazz, methodsOffset)
        if (methods == 0L) return list
        val numMethods = unsafe.getInt(methods)
        for (i in 0 until numMethods) {
            val method = methods + i * artMethodSize + artMethodBias
            unsafe.putLong(mh, artOffset, method)
            unsafe.putObject(mh, infoOffset, null)
            try {
                MethodHandles.lookup().revealDirect(mh)
            } catch (ignored: Throwable) {
            }
            val info = unsafe.getObject(mh, infoOffset) as MethodHandleInfo
            val member = unsafe.getObject(info, memberOffset) as Executable
            list.add(member)
        }
        return list
    }

    @Throws(NoSuchMethodException::class)
    fun getDeclaredMethod(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method {
        val methods = getDeclaredMethods(clazz)
        allMethods@ for (method in methods) {
            if (method.name != methodName) continue
            if (method !is Method) continue
            val expectedTypes = method.getParameterTypes()
            if (expectedTypes.size != parameterTypes.size) continue
            for (i in parameterTypes.indices) {
                if (parameterTypes[i] != expectedTypes[i]) continue@allMethods
            }
            return method
        }
        throw NoSuchMethodException("Cannot find matching method")
    }

    @Throws(NoSuchMethodException::class)
    fun getDeclaredConstructor(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*> {
        val methods = getDeclaredMethods(clazz)
        allMethods@ for (method in methods) {
            if (method !is Constructor<*>) continue
            val expectedTypes = method.getParameterTypes()
            if (expectedTypes.size != parameterTypes.size) continue
            for (i in parameterTypes.indices) {
                if (parameterTypes[i] != expectedTypes[i]) continue@allMethods
            }
            return method
        }
        throw NoSuchMethodException("Cannot find matching constructor")
    }

    fun getInstanceFiled(fieldName: String, clazz: Class<*>): Field? {
        return getInstanceFields(clazz).firstOrNull {
            it.name == fieldName
        }
    }

    fun getInstanceFields(clazz: Class<*>): List<Field> {
        val list = ArrayList<Field>()
        if (clazz.isPrimitive || clazz.isArray) return list
        val mh: MethodHandle
        try {
            val fI = NeverCall::class.java.getDeclaredField("i")
            fI.isAccessible = true
            mh = MethodHandles.lookup().unreflectGetter(fI)
        } catch (e: IllegalAccessException) {
            return list
        } catch (e: NoSuchFieldException) {
            return list
        }
        val fields = unsafe.getLong(clazz, iFieldOffset)
        if (fields == 0L) return list
        val numFields = unsafe.getInt(fields)
        for (i in 0 until numFields) {
            val field = fields + i * artFieldSize + artFieldBias
            unsafe.putLong(mh, artOffset, field)
            unsafe.putObject(mh, infoOffset, null)
            try {
                MethodHandles.lookup().revealDirect(mh)
            } catch (ignored: Throwable) {
            }
            val info = unsafe.getObject(mh, infoOffset) as MethodHandleInfo
            val member = unsafe.getObject(info, memberOffset) as Field
            list.add(member)
        }
        return list
    }

    fun getStaticFiled(fieldName: String, clazz: Class<*>): Field? {
        return getStaticFields(clazz).firstOrNull() {
            it.name == fieldName
        }
    }

    fun getStaticFields(clazz: Class<*>): List<Field> {
        val list = ArrayList<Field>()
        if (clazz.isPrimitive || clazz.isArray) return list
        val mh: MethodHandle
        try {
            val fS = NeverCall::class.java.getDeclaredField("s")
            fS.isAccessible = true
            mh = MethodHandles.lookup().unreflectGetter(fS)
        } catch (e: IllegalAccessException) {
            return list
        } catch (e: NoSuchFieldException) {
            return list
        }
        val fields = unsafe.getLong(clazz, sFieldOffset)
        if (fields == 0L) return list
        val numFields = unsafe.getInt(fields)
        for (i in 0 until numFields) {
            val field = fields + i * artFieldSize + artFieldBias
            unsafe.putLong(mh, artOffset, field)
            unsafe.putObject(mh, infoOffset, null)
            try {
                MethodHandles.lookup().revealDirect(mh)
            } catch (ignored: Throwable) {
            }
            val info = unsafe.getObject(mh, infoOffset) as MethodHandleInfo
            val member = unsafe.getObject(info, memberOffset) as Field

            list.add(member)
        }
        return list
    }

    fun setHiddenApiExemptions(vararg signaturePrefixes: String): Boolean {
        try {
            val runTimeClz = Class.forName("dalvik.system.VMRuntime")
            val runtime = invoke(runTimeClz, null, "getRuntime")
            invoke(
                runTimeClz,
                runtime,
                "setHiddenApiExemptions",
                signaturePrefixes as Any
            )
            return true
        } catch (e: Throwable) {
            return false
        }
    }

    fun addHiddenApiExemptions(vararg signaturePrefixes: String): Boolean {
        HiddenApi.signaturePrefixes.addAll(signaturePrefixes)
        return setHiddenApiExemptions(*signaturePrefixes)
    }

    fun clearHiddenApiExemptions(): Boolean {
        signaturePrefixes.clear()
        return setHiddenApiExemptions()
    }
}
