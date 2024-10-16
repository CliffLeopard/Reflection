package reflect.android.content.res

import com.cliff.reflection.common.IReflect
import com.cliff.reflection.common.annotation.PField
import com.cliff.reflection.common.annotation.ProxyClass

/**
 * @Author CliffLeopard
 * @Email  precipiceleopard@gmail.com
 * @Time   2024/10/16 15:25
 */
@ProxyClass("android.content.res.CompatibilityInfo")
interface CompatibilityInfo:IReflect {
    companion object
    @PField
    val mCompatibilityFlags:Int
    @PField
    val applicationDensity:Int
    @PField
    val applicationScale:Float
    @PField
    val applicationInvertedScale:Float
}