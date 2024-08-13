package com.cliff.reflection.common.annotation
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class PStaticField(val value:String = "")
