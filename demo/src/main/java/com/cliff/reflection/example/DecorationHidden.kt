package com.cliff.reflection.example

import com.cliff.reflection.common.IReflect
import com.cliff.reflection.common.annotation.PConstructor
import com.cliff.reflection.common.annotation.PField
import com.cliff.reflection.common.annotation.PMethod
import com.cliff.reflection.common.annotation.ProxyClass

/**
 * @Author CliffLeopard
 * @Email  precipiceleopard@gmail.com
 * @Time   2024/10/6 18:53
 */
@ProxyClass("com.cliff.reflection.example.HiddenClass")
interface DecorationHidden: IReflect {
    companion object

    @PField
    val name: String

    @PField
    val age: Int

    @PMethod
    fun print()

    @PMethod
    fun toStr(): String

    @PConstructor("com.cliff.reflection.example.HiddenClass")
    fun newHiddenClass(name: String, age: Int): Any
}