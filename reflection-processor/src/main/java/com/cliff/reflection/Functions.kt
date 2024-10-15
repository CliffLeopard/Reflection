package com.cliff.reflection

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

fun String.toTypeName(): TypeName {
    val parts = this.replace("`", "").split(".")
    val simpleName = parts.last()
    val packageName = parts.dropLast(1).joinToString(".")
    return ClassName(packageName, simpleName)
}

fun String.getPackageName(): String {
    val parts = this.replace("`", "").split(".")
    return parts.dropLast(1).joinToString(".")
}

fun String.getSimpleName(): String {
    val parts = this.replace("`", "").split(".")
    return parts.last()
}