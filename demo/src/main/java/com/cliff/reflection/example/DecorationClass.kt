package com.cliff.reflection.example

import com.cliff.reflection.common.IReflect
import com.cliff.reflection.common.annotation.PField
import com.cliff.reflection.common.annotation.PMethod
import com.cliff.reflection.common.annotation.PMethodParameter
import com.cliff.reflection.common.annotation.PStaticField
import com.cliff.reflection.common.annotation.PStaticMethod
import com.cliff.reflection.common.annotation.ProxyClass

/**
 * @Author CliffLeopard
 * @Email  precipiceleopard@gmail.com
 * @Time   2024/10/6 03:44
 */
@ProxyClass(targetType = "com.cliff.reflection.example.TargetClass")
interface DecorationClass : IReflect {
    companion object

    @PStaticField
    var TAG: String?

    @PStaticField("com.cliff.reflection.example.HiddenClass")
    var hiddenField: Any?

    @PStaticField("com.cliff.reflection.example.HiddenClass")
    var hiddenField2: DecorationHidden?

    @PStaticField("com.cliff.reflection.example.TargetClass\$TargetInsideClass")
    var hiddenInside: DecorationInsideClass?


    @PStaticMethod
    fun sMethod1(): String


    @PField
    var strField: String

    @PField
    var intField: Int

    @PField
    var longField: Long

    @PMethod
    fun print()

    @PMethod
    fun method1(): Int

    @PMethod
    fun method2(name: String?, age: Int): String

    @PMethod
    fun method3(name: String, age: Int, list: List<Map<String, String>>): String

    @PMethod
    fun method4(name: String, age: Int, @PMethodParameter("com.cliff.reflection.example.HiddenClass") hiddenClass: Any): String

    @PMethod
    fun method42(name: String, age: Int, @PMethodParameter("com.cliff.reflection.example.HiddenClass") hiddenClass: DecorationHidden): String

    @PMethod("com.cliff.reflection.example.HiddenClass")
    fun method5(name: String, age: Int): Any

    @PMethod("com.cliff.reflection.example.HiddenClass")
    fun method6(name: String, age: Int): DecorationHidden?

    @PMethod("com.cliff.reflection.example.TargetClass\$TargetInsideClass")
    fun method7(): DecorationInsideClass


    @ProxyClass(targetType = "com.cliff.reflection.example.TargetClass\$TargetInsideClass")
    interface DecorationInsideClass : IReflect {
        companion object

        @PStaticField
        var TAG: String?

        @PStaticMethod
        fun sMethod1(): String


        @PField
        var strField: String

        @PField
        var intField: Int

        @PField
        var longField: Long

        @PMethod
        fun print()

        @PMethod
        fun method1(): Int

        @PMethod
        fun method2(name: String?, age: Int): String

        @PMethod
        fun method3(name: String, age: Int, list: List<Map<String, String>>): String

        @PMethod
        fun method4(name: String, age: Int, @PMethodParameter("com.cliff.reflection.example.HiddenClass") hiddenClass: Any): String

        @PMethod
        fun method42(name: String, age: Int, @PMethodParameter("com.cliff.reflection.example.HiddenClass") hiddenClass: DecorationHidden): String

        @PMethod("com.cliff.reflection.example.HiddenClass")
        fun method5(name: String, age: Int): Any
    }
}