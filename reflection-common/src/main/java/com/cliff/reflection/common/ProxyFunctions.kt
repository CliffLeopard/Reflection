package com.cliff.reflection.common

//import com.cliff.reflection.common.annotation.PMethodParameter
//import com.cliff.reflection.common.annotation.PMethodType
//import com.cliff.reflection.common.annotation.ProxyClass
//import java.lang.reflect.Proxy

// 使用动态代理的方式来生成接口实现类，效率较低，不使用该方法
//inline fun <reified T> createProxy(): T {
//    val fieldGetPrefix = "_f_get_"
//    val fieldSetPrefix = "_f_set_"
//    val classLoader = T::class.java.classLoader
//    val interfaces = arrayOf<Class<*>>(T::class.java)
//    val clz = Class.forName(T::class.java.getAnnotation(ProxyClass::class.java)!!.value)
//    return Proxy.newProxyInstance(classLoader, interfaces) { _, method, args ->
//        val methodName = method.name
//        val parameterAnnotations = method.parameterAnnotations
//        val argTypes: Array<Class<*>> = method.parameterTypes.mapIndexed { index, arg ->
//            parameterAnnotations[index].firstOrNull {
//                it is PMethodParameter
//            }?.let {
//                Class.forName((it as PMethodParameter).value)
//            } ?: arg
//        }.toTypedArray()
//
//        if (methodName.startsWith(fieldGetPrefix)) {
//            val fieldName = methodName.substring(fieldGetPrefix.length)
//            val field = clz.getDeclaredField(fieldName)
//            field.isAccessible = true
//            if (args.isNullOrEmpty()) {
//                field.get(null)
//            } else {
//                field.get(args[0])
//            }
//        } else if (methodName.startsWith(fieldSetPrefix)) {
//            val fieldName = methodName.substring(fieldSetPrefix.length)
//            val field = clz.getDeclaredField(fieldName)
//            field.isAccessible = true
//            if (args.size == 2) {
//                field.set(args[0], args[1])
//            } else {
//                field.set(null, args[0])
//            }
//        } else {
//            //
//            val methodType = method.getAnnotation(PMethodType::class.java)!!
//            if (methodType.type == MethodType.METHOD) {
//                val realArgs = argTypes.slice(1..<argTypes.size).toTypedArray()
//                val originMethod = clz.getDeclaredMethod(methodName, *realArgs)
//                if (args.size > 1) {
//                    originMethod.invoke(args[0], args.slice(1..<args.size))
//                } else {
//                    originMethod.invoke(args[0])
//                }
//            } else {
//                val originMethod = clz.getDeclaredMethod(methodName, *argTypes)
//                if (args.isNullOrEmpty()) {
//                    originMethod.invoke(null)
//                } else {
//                    originMethod.invoke(null, args)
//                }
//            }
//        }
//    } as T
//}