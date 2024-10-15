package com.cliff.reflection

import android.app.Application
import android.content.Context
import com.cliff.reflection.common.Reflect
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger

class RApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        context = this
    }

    override fun onCreate() {
        super.onCreate()
        Logger.addLogAdapter(AndroidLogAdapter())
        Reflect.registerExecutor(HiddenExecutor())
    }

    companion object {
        lateinit var context: RApplication
    }
}