package com.cliff.reflection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import reflect.android.app.ActivityThread
import reflect.android.app.ActivityThread__Functions.reflectInstance

class MainViewModel : ViewModel() {
    private val _sections = MutableLiveData(baseSections)
    val sections: LiveData<List<Section>> get() = _sections

    companion object {
        private val context by lazy {
            RApplication.context
        }
        val baseSections: List<Section> = listOf(
            Section("ActivityThread获取") {
                val activityThread:ActivityThread = ActivityThread.reflectInstance(null).currentActivityThread()!!
                val processName = activityThread.getProcessName()
                val applicationThread = activityThread.getApplicationThread()
                Logger.e("processName:$processName")
                Logger.e("activityThreadClass:${activityThread::class.java.name}")
                Logger.e("applicationThread:${applicationThread::class.java.name}")

                val packageManager = context.packageManager
                val appInfo = packageManager.getApplicationInfo(context.packageName,0)

            },
        )
    }
}