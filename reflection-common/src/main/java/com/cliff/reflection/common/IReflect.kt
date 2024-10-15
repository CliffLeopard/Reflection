package com.cliff.reflection.common

/**
 * @Author CliffLeopard
 * @Email  precipiceleopard@gmail.com
 * @Time   2024/10/15 11:57
 */
interface IReflect {
    fun _getOriginClass_(): Class<*>
    fun _getOriginObject_(): Any?
}