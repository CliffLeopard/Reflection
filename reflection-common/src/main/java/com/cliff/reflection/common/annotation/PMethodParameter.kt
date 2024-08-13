package com.cliff.reflection.common.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class PMethodParameter(val value: String)
