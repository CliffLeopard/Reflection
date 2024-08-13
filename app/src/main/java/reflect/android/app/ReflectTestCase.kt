package reflect.android.app

import com.cliff.reflection.common.annotation.PMethod
import com.cliff.reflection.common.annotation.ProxyClass

@ProxyClass("reflect.android.app.ReflectOrigin")
interface ReflectTestCase {
    companion object
    @PMethod
    fun good():String
}