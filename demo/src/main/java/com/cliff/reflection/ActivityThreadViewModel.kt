package com.cliff.reflection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import reflect.android.app.ActivityThread
import reflect.android.app.ActivityThread__Functions.__instance__
import reflect.android.app.ActivityThread__Functions.currentActivityThread
import reflect.android.app.ActivityThread__Functions.getSPackageManager

/**
 * @Author CliffLeopard
 * @Email  precipiceleopard@gmail.com
 * @Time   2024/10/15 10:44
 */
class ActivityThreadViewModel : ViewModel() {
    private val _sections = MutableLiveData(baseSections)
    val sections: LiveData<List<Section>> get() = _sections

    companion object {
        private val context by lazy {
            RApplication.context
        }

        private val activityThread by lazy {
            ActivityThread.currentActivityThread()!!
        }
        val baseSections: List<Section> = listOf(
            Section("ActivityThread获取") {
                val sPackageManager = ActivityThread.getSPackageManager()
                Logger.e("sPackageManager:${sPackageManager == null}")


                val mAppThread = activityThread.getApplicationThread()

                Logger.e("sPackageManager:${mAppThread == null}")


                val processName = activityThread.getProcessName()
                Logger.e("processName:$processName")


                val packageManager = context.packageManager
                val appInfo = packageManager.getApplicationInfo(context.packageName, 0)
            }
        )
    }
}