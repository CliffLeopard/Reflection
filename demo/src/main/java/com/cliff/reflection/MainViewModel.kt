package com.cliff.reflection

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cliff.hidden.HiddenApi
import com.cliff.reflection.example.DecorationClass
import com.cliff.reflection.example.DecorationClass_DecorationInsideClass__Functions.__instance__
import com.cliff.reflection.example.DecorationClass__Functions.__instance__
import com.cliff.reflection.example.DecorationHidden
import com.cliff.reflection.example.DecorationHidden__Functions.__instance__
import com.cliff.reflection.example.DecorationHidden__Functions.newHiddenClass
import com.cliff.reflection.example.DecorationHidden__Functions.newHiddenClass2
import com.cliff.reflection.example.DecorationHidden__Functions.newHiddenClass3
import com.cliff.reflection.example.DecorationHidden__Functions.newHiddenClass4
import com.cliff.reflection.example.HiddenClass
import com.cliff.reflection.example.TargetClass
import com.orhanobut.logger.Logger
import reflect.android.app.ActivityThread
import reflect.android.app.ActivityThread__Functions.currentActivityThread
import reflect.android.app.LoadedApk
import reflect.android.app.LoadedApk__Functions.__instance__
import reflect.android.view.DisplayAdjustments
import reflect.android.view.DisplayAdjustments__Functions.__instance__

class MainViewModel : ViewModel() {
    private val _sections = MutableLiveData(baseSections)
    val sections: LiveData<List<Section>> get() = _sections

    companion object {
        private val context by lazy {
            RApplication.context
        }

        private val target = TargetClass()
        private val decoration = DecorationClass.__instance__(target)
//        private val target = TargetInsideClass()
//        private val decoration = DecorationClass.DecorationInsideClass.__instance__(target)

        val baseSections: List<Section> = listOf(
            Section("ActivityThread相关Hook") {
                val intent = Intent(context, ActivityThreadActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            },
            Section("访问静态属性:TAG") {
                Logger.i("访问静态属性:${decoration.TAG}")
                decoration.TAG = "ZZZQ"
                Logger.i("访问静态属性:修改后:${decoration.TAG}")
            },

            Section("访问静态隐藏类属性:hiddenField") {
                val hidden = DecorationHidden.__instance__(decoration.hiddenField!!)
                Logger.i("访问静态属性:${hidden.toStr()}")
                decoration.hiddenField = HiddenClass("LiSi", 98)
                val hidden2 = DecorationHidden.__instance__(decoration.hiddenField!!)
                Logger.i("访问静态属性:修改后:${hidden2.toStr()}")
            },

            Section("访问静态隐藏类属性:hiddenField2") {
                val hidden = decoration.hiddenField2
                Logger.i("访问静态属性:${hidden?.toStr()}")
                decoration.hiddenField2 = DecorationHidden.__instance__(HiddenClass("LiSi", 98))
                val hidden2 = decoration.hiddenField2
                Logger.i("访问静态属性:修改后:${hidden2?.toStr()}")
            },

            Section("访问静态内部隐藏类属性:hiddenInside") {
                val hidden = decoration.hiddenInside
                Logger.i("访问静态内部隐藏类属性:${hidden?.print()}")
                var newInside = TargetClass.TargetInsideClass()
                newInside.setStr("newStr12020")
                decoration.hiddenInside = DecorationClass.DecorationInsideClass.__instance__(newInside)
                val hidden2 = decoration.hiddenInside
                Logger.i("访问静态内部隐藏类属性:修改后:${hidden2?.print()}")
            },

            Section("访问静态方法:sMethod1") {
                Logger.i("访问静态方法:${decoration.sMethod1()}")
            },
            Section("访问对象属性:strField") {
                Logger.i("访问对象属性:${decoration.strField}")
                decoration.strField = "newField"
                Logger.i("访问对象属性:修改后:${decoration.strField}")
            },
            Section("访问对象属性:intField") {
                Logger.i("访问对象属性:${decoration.intField}")
                decoration.intField = 123456
                Logger.i("访问对象属性:修改后:${decoration.intField}")
            },
            Section("访问对象属性:longField") {
                Logger.i("访问对象属性:${decoration.longField}")
                decoration.longField = 19029384
                Logger.i("访问对象属性:修改后:${decoration.longField}")
            },
            Section("访问对象无参方法:method1") {
                Logger.i("访问对象无参方法:${decoration.method1()}")
            },
            Section("访问对象有参方法:method2") {
                val result = decoration.method2("CliffLeopard,Method2", 123)
                Logger.i("访问对象有参方法:$result")

            },
            Section("访问对象泛型参数方法:method3") {
                val map1 = mapOf("key1" to "value1", "key2" to "value2")
                val map2 = mapOf("key3" to "value3", "key4" to "value4")
                val list = listOf(map1, map2)
                val result = decoration.method3("CliffLeopard,Method3", 123, list)
                Logger.i("访问对象泛型参数方法: $result")
            },

            Section("访问对象隐藏参数类型方法:method4") {
                val hidden = HiddenClass("ZhangSan", 134)
                val result = decoration.method4("CliffLeopard,Method4", 984, hidden)
                Logger.i("访问对象隐藏参数类型方法:$result")

            },
            Section("访问对象隐藏参数类型方法:method42") {
                val hidden = HiddenClass("ZhangSan", 134)
                val decorationHidden = DecorationHidden.__instance__(hidden)
                val result = decoration.method42("CliffLeopard,Method4", 984, decorationHidden)
                Logger.i("访问对象隐藏参数类型方法:$result")

            },
            Section("访问对象有参，隐藏返回类型方法:method5") {
                val result = decoration.method5("CliffLeopard,Method5", 123) as HiddenClass
                Logger.i("访问对象有参，隐藏返回类型方法:${result.toStr()}")
            },

            Section("访问对象有参，隐藏返回类型方法:method6") {
                val result = decoration.method6("CliffLeopard,Method6", 123)
                Logger.i("访问对象有参，隐藏返回类型方法:${result?.toStr()}")
            },

            Section("内部类作为隐藏类返回:method7") {
                val result = decoration.method7()
                Logger.i("内部类作为隐藏类返回:${result.toString()}")
            },

            Section("隐藏类构造函数:") {
                val hidden1: Any = DecorationHidden.newHiddenClass("Cliff", 12)
                val hidden2: DecorationHidden? = DecorationHidden.newHiddenClass2("Leopard", 13)
                val hidden3: DecorationHidden? = DecorationHidden.newHiddenClass3()
                val hidden4: Any = DecorationHidden.newHiddenClass4()
                val hidden5: HiddenClass = DecorationHidden.newHiddenClass4() as HiddenClass
                Logger.i("内部类作为隐藏类返回: hidden1:${DecorationHidden.__instance__(hidden1).toStr()}")
                Logger.i("内部类作为隐藏类返回: hidden2:${hidden2?.toStr()}")
                Logger.i("内部类作为隐藏类返回: hidden3:${hidden3?.toStr()}")
                Logger.i("内部类作为隐藏类返回: hidden4:${DecorationHidden.__instance__(hidden4).toStr()}")
                Logger.i("内部类作为隐藏类返回: hidden5:${hidden5.toStr()}")
            },

            Section("HiddenAPi") {
                val activityThread = ActivityThread.currentActivityThread()!!
                val mPackages = activityThread.mPackages
                val nowLoadedApk = mPackages[context.packageName]!!.get()!!
                Logger.i("nowLoadedApk: ${nowLoadedApk.javaClass.canonicalName}")
                val reLoadedApk = LoadedApk.__instance__(nowLoadedApk)
                val mDisplayAdjustments = reLoadedApk.mDisplayAdjustments
                val mCompatInfo = DisplayAdjustments.__instance__(mDisplayAdjustments).mCompatInfo
                Logger.i("mCompatInfo: ${mCompatInfo.mCompatibilityFlags}")
            },

            Section("Java-Kotlin类型") {
                val any = Any()
                Logger.i("any :${any::class.java.canonicalName}")
                Logger.i("any :${any::class.java.name}")

                val array: Array<*> = arrayOf(10, 20)
                Logger.i("array :${array::class.java.canonicalName}")
                Logger.i("array :${array::class.java.name}")

                val double: Double = 123.3
                Logger.i("double :${double::class.java.canonicalName}")
                Logger.i("double :${double::class.java.name}")

                val str: String = "123.3"
                Logger.i("str :${str::class.java.canonicalName}")
                Logger.i("str :${str::class.java.name}")
            },
        )
    }
}