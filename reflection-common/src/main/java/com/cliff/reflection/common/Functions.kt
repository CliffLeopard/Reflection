package com.cliff.reflection.common

fun String.toJavaType():String {
    // 如果是复合类，选取最外层类型
    val index = this.indexOfFirst { ch -> ch == '<' }
    return if (index != -1) {
        this.substring(0, index)
    } else {
        this
    }.removeSuffix("?")
}

// Kotlin类型字符串 转换为 Java类型字符串
fun String.kotlinToJava(): String {
    val simpleName = this.toJavaType()
    return kotlinBuiltIns[simpleName] ?: simpleName
}

// 根据类名获取类
fun String.toClass(): Class<*> {
    return Class.forName(this)
}

// Kotlin与Java类型对
val kotlinBuiltIns: Map<String, String> = mapOf(
    "kotlin.Annotation" to "java.lang.annotation.Annotation",
    "kotlin.Any" to "java.lang.Object",
    "kotlin.Array" to "java.lang.Integer[]",
    "kotlin.Boolean" to "boolean",
    "kotlin.BooleanArray" to "boolean[]",
    "kotlin.Byte" to "java.lang.Byte",
    "kotlin.ByteArray" to "java.lang.Byte",
    "kotlin.Char" to "char",
    "kotlin.CharArray" to "char[]",
    "kotlin.CharSequence" to "java.lang.CharSequence",
    "kotlin.Comparable" to "java.lang.Comparable",
    "kotlin.Double" to "double",
    "kotlin.DoubleArray" to "double[]",
    "kotlin.Enum" to "java.lang.Enum",
    "kotlin.Float" to "float",
    "kotlin.FloatArray" to "float[]",
    "kotlin.Int" to "int",
    "kotlin.IntArray" to "int[]",
    "kotlin.Long" to "long",
    "kotlin.LongArray" to "long[]",
    "kotlin.Nothing" to "java.lang.Object",
    "kotlin.Number" to "java.lang.Number",
    "kotlin.Short" to "short",
    "kotlin.ShortArray" to "short[]",
    "kotlin.String" to "java.lang.String",
    "kotlin.Throwable" to "java.lang.Throwable",

    "kotlin.collections.Collection" to "java.util.Collection",
    "kotlin.collections.Iterable" to "java.lang.Iterable",
    "kotlin.collections.List" to "java.util.List",
    "kotlin.collections.Map" to "java.util.Map",
    "kotlin.collections.MutableCollection" to "java.util.Collection",
    "kotlin.collections.MutableIterable" to "java.lang.Iterable",
    "kotlin.collections.MutableList" to "java.util.List",
    "kotlin.collections.MutableMap" to "java.util.Map",
    "kotlin.collections.MutableSet" to "java.util.Set",
    "kotlin.collections.Set" to "java.util.Set"
)