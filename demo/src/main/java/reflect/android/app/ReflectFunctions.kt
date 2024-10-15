package reflect.android.app

import android.annotation.SuppressLint
import com.cliff.reflection.common.Reflect
import com.orhanobut.logger.Logger
import java.lang.reflect.Proxy

//object ReflectFunctions {
//    private fun wrapResult(originResult: Any?, returnType:String, annotationValue:String?): Any? {
//        return if (originResult == null || annotationValue == null) {
//            null
//        } else if (originResult::class.java.name != returnType) {
////            returnType.reflectInstance(originResult)
//            ActivityThread.reflectInstance(originResult)
//        } else { originResult }
//    }
//
//    fun ActivityThread.Companion.reflectInstance(obj: Any?): ActivityThread {
//        Logger.e("reflectInstance")
//        return Proxy.newProxyInstance(
//            ActivityThread::class.java.classLoader,
//            arrayOf(ActivityThread::class.java)
//        ) { _, method, args ->
//            val realArgs = args ?: arrayOf()
//            val parameterTypes = method.parameterTypes.joinToString { it.canonicalName!!.toString() }
//            val returnType = method.returnType
//            val methodName = method.name
//
////            val result = when {
////                methodName == "currentActivityThread" && parameterTypes.contentEquals() ->
////            }
//
//
//
//            val result = when (method.name) {
////                // 当是PField
////                "getMAppThread" -> Reflect.accessField(obj, originClass(), "mAppThread")
////                //当是PStaticField
////                "sPackageManager" -> Reflect.accessField(null, originClass(), "sPackageManager")
////                // PStaticMethod
////                "currentActivityThread" -> Reflect.invokeMethod(null, originClass(), method, *realArgs)
////                // PMethod
////                "getApplicationThread"  -> Reflect.invokeMethod(obj, originClass(), method, *realArgs)
////
////                "_getOriginClass_" -> originClass()
////                "_getOriginObject_" -> obj
//                else -> Unit
//            }
//            wrapResult(result, method.returnType.canonicalName!!,"")
//        } as ActivityThread
//    }
//
//
//    @SuppressLint("PrivateApi")
//    fun ActivityThread.Companion.originClass(): Class<*> {
//        val className = "android.app.ActivityThread"
//        return if (className.contains("kotlin."))
//            Int::class.java
//        else Class.forName(className)
//    }
//
//}


