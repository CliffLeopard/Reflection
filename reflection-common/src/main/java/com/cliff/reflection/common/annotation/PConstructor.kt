package com.cliff.reflection.common.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class PConstructor(val targetType: String)
