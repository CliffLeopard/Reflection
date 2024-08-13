package com.cliff.reflection.common.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ProxyClass(val value: String, val isKotlin: Boolean = false)
