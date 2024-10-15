package reflect.android.app

import android.content.pm.ApplicationInfo
import android.util.ArrayMap
import com.cliff.reflection.common.IReflect
import com.cliff.reflection.common.annotation.PField
import com.cliff.reflection.common.annotation.PMethod
import com.cliff.reflection.common.annotation.PMethodParameter
import com.cliff.reflection.common.annotation.PStaticField
import com.cliff.reflection.common.annotation.PStaticMethod
import com.cliff.reflection.common.annotation.ProxyClass
import java.lang.ref.WeakReference

@ProxyClass("android.app.ActivityThread")
interface ActivityThread : IReflect {
    companion object
    @PStaticMethod("android.app.ActivityThread")
    fun currentActivityThread(): ActivityThread?

    @PStaticField("android.content.pm.IPackageManager")
    val sPackageManager: Any?

    @PField("android.app.ActivityThread\$ApplicationThread")
    val mAppThread: Any?

    @PField
    val mPackages: ArrayMap<String, WeakReference<Any>>

    @PField
    val mResourcePackages: ArrayMap<String, WeakReference<Any>>

    @PMethod
    fun getProcessName(): String?

    @PMethod("android.app.ActivityThread\$ApplicationThread")
    fun getApplicationThread(): Any?

    @PMethod("android.app.LoadedApk")
    fun getPackageInfo(
        aInfo: ApplicationInfo?,
        @PMethodParameter("android.content.res.CompatibilityInfo") compatInfo: Any,
        baseLoader: ClassLoader? = null,
        securityViolation: Boolean = false,
        includeCode: Boolean = true,
        registerPackage: Boolean = false,
        isSdkSandbox: Boolean = false
    ): Any
}