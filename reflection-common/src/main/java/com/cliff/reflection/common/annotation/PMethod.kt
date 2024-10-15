package com.cliff.reflection.common.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class PMethod(val targetType: String = "")
