# Reflection

This is an annotation processing tool developed using KSP to simplify the complexity of using reflection in Android projects.

The functionality involved in accessing Android's hidden API in the Demo uses [AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass)


## Usage

For example, the following two classes exist，and the `Hidden` is not directly accessible
```java
public class A {
    private String str;
    private Hidden hidden;
    
    private void method1() {
        ....
    }
    
    private void method2(String str,Hidden hidden) {
        ...
    }
    
    private Hidden method3(String str) {
        
    }
}

public class Hidden {
    private String str;
    private int age;
}
```


You Can write a new Kotlin interface

```kotlin

@ProxyClass(targetType ="Hidden")
interface DecorationHidden:IReflect {
    companion object
    @PField
    var str:String?
    @PField
    var age:Int
}

@ProxyClass(targetType ="A")
interface DecorationA:IReflect {
    companion object
    @PField
    var str:String?
    @PField
    var hidden:Any

    @PMethod
    fun method1()

    @PMethod
    fun method2(str:String ,@PMethodParameter("Hidden") hidden:Any)

    @PMethod(targetType="Hidden")
    fun method3(str:String):Any
}

```

Access as follows

```kotlin
fun main() {
    val a = A()
    val decorationA = DecorationA.__instance__(a)
    decorationA.method1()
    val hidden = decorationA.method3("sd")
    decorationA.method2("123",hidden)
}
```

If you want to see more other ways to use please refer to demo