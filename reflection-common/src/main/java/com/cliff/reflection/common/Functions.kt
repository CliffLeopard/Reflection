package com.cliff.reflection.common

// 如果是复合类，选取最外层类型
fun extractType(qualifiedName: String): String {
    val pattern = Regex("^(?<className>[\\w.$]+)(<(?<genericTypes>[^>]*)>)?(\\?)?$")
    val matchResult = pattern.find(qualifiedName)
    return if (matchResult != null) {
        val className = matchResult.groups["className"]?.value ?: ""
        val genericTypes = matchResult.groups["genericTypes"]?.value
        if (genericTypes != null) {
            className
        } else {
            className.replace("?", "").replace("`", "")
        }
    } else {
        qualifiedName.replace("`", "")
    }
}

fun arrayContentsEq(a1: Array<Any>?, a2: Array<Any>?): Boolean {
    if (a1.isNullOrEmpty()) {
        return a2.isNullOrEmpty()
    }
    if (a2.isNullOrEmpty())
        return false
    if (a1.size != a2.size) {
        return false
    }
    for (i in a1.indices) {
        if (a1[i] !== a2[i]) {
            return false
        }
    }
    return true
}