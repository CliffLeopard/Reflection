package com.cliff.reflection.common.annotation
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class PField(val targetType:String = "")
