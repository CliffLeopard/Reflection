package reflect.android.app

import android.content.pm.ApplicationInfo
import com.cliff.reflection.common.IReflect
import com.cliff.reflection.common.annotation.PConstructor
import com.cliff.reflection.common.annotation.PField
import com.cliff.reflection.common.annotation.PMethodParameter
import com.cliff.reflection.common.annotation.ProxyClass

/**
 * @Author CliffLeopard
 * @Email  precipiceleopard@gmail.com
 * @Time   2024/10/16 15:32
 */
@ProxyClass("android.app.LoadedApk")
interface LoadedApk : IReflect {
    companion object

    @PField
    val mApplicationInfo: ApplicationInfo

    @PField("android.view.DisplayAdjustments")
    val mDisplayAdjustments: Any

    @PConstructor("android.app.LoadedApk")
    fun newLoadedApk(
        @PMethodParameter("android.app.ActivityThread") activityThread: Any,
        aInfo: ApplicationInfo,
        @PMethodParameter("android.content.res.CompatibilityInfo") compatInfo: Any,
        baseLoader: ClassLoader,
        securityViolation: Boolean, includeCode: Boolean, registerPackage: Boolean
    ): Any
}