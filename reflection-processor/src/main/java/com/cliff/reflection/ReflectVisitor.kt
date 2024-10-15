package com.cliff.reflection

import com.cliff.reflection.common.annotation.PConstructor
import com.cliff.reflection.common.annotation.PField
import com.cliff.reflection.common.annotation.PMethod
import com.cliff.reflection.common.annotation.PMethodParameter
import com.cliff.reflection.common.annotation.PStaticField
import com.cliff.reflection.common.annotation.PStaticMethod
import com.cliff.reflection.common.annotation.ProxyClass
import com.cliff.reflection.common.kotlinBuiltIns
import com.cliff.reflection.common.kotlinToJava
import com.cliff.reflection.common.toJavaType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import java.util.Locale

class ReflectVisitor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator
) : KSVisitorVoid() {
    private val implFileSuffix = "__Functions"
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val clzAno = classDeclaration.annotations.first {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == ProxyClass::class.java.name
        }
        val clzAv = clzAno.arguments.first { it.name!!.asString() == "targetType" }.value.toString()
        val proxyClz = classDeclaration.toClassName()

        val implFile =
            FileSpec.builder(proxyClz.packageName, "${outerClassNamePrefix(classDeclaration)}${proxyClz.simpleName}${implFileSuffix}.kt")
        val implClass = TypeSpec.objectBuilder("${outerClassNamePrefix(classDeclaration)}${proxyClz.simpleName}${implFileSuffix}")

        implClass.addFunction(originFunction(classDeclaration, clzAv))
        implClass.addFunction(instanceFunction(classDeclaration, implClass))
        implFile.addType(implClass.build())
            .addImport("java.lang.reflect", "Proxy")
            .addImport("com.cliff.reflection.common", "Reflect")
            .addImport("com.cliff.reflection.common", "Section")
            .build()
            .writeTo(codeGenerator, Dependencies(true, classDeclaration.containingFile!!))
    }

    // 代理对象生成
    private fun instanceFunction(classDeclaration: KSClassDeclaration, implClass: TypeSpec.Builder): FunSpec {
        val reflectBuilder = FunSpec.builder("__instance__")
            .addParameter("obj", ClassName("kotlin", "Any"))
            .receiver(classDeclaration.toClassName().nestedClass("Companion"))
            .returns(classDeclaration.toClassName())

        val codeBuilder = CodeBlock.builder()
            .beginControlFlow(
                "val proxy = Proxy.newProxyInstance(%T::class.java.classLoader , arrayOf(%T::class.java)) { _, method, args -> ",
                classDeclaration.toClassName(),
                classDeclaration.toClassName()
            )
            .addStatement("val realArgs = args ?: arrayOf()")
            .addStatement("val parameterTypes = method.parameterTypes.joinToString(transform = { it.canonicalName?.toString() ?: it.name })")
            .addStatement("val methodName = method.name")
            .beginControlFlow("when {")
            .addStatement("%S == methodName -> %L", "_getOriginClass_", " __originClass__()")
            .addStatement("%S == methodName -> %L", "_getOriginObject_", "obj")

        classDeclaration.getAllFunctions()
            .forEach { invokeMethod(classDeclaration, it, codeBuilder, implClass) }

        classDeclaration.getAllProperties()
            .forEach { accessField(classDeclaration, it, codeBuilder, implClass) }

        reflectBuilder.addCode(codeBuilder.build())
            .addStatement("else ->%S+%L+%S+%L", "TargetEmpty:methodName:", "methodName", " parameterTypes:", "parameterTypes")
            .endControlFlow()
            .endControlFlow()
            .addStatement("return proxy as %T", classDeclaration.toClassName())
        return reflectBuilder.build()
    }

    // Method反射
    private fun invokeMethod(
        classDeclaration: KSClassDeclaration,
        fct: KSFunctionDeclaration,
        codeBuilder: CodeBlock.Builder,
        implClass: TypeSpec.Builder
    ) {
        fct.annotations.forEach { ksAnno ->
            // 方法注解
            val mAnt = ksAnno.annotationType.toTypeName().toString().replace("`", "")
            // 方法注解参数
            val mAv = ksAnno.arguments.firstOrNull { arg -> arg.name?.asString() == "targetType" }?.value?.toString()
            when (mAnt) {
                PMethod::class.java.name -> invokeMethod(false, mAv, fct, codeBuilder, implClass, classDeclaration)
                PStaticMethod::class.java.name -> invokeMethod(true, mAv, fct, codeBuilder, implClass, classDeclaration)
                PConstructor::class.java.name -> invokeConstructor(mAv!!, fct, codeBuilder)
                else -> Unit
            }
        }
    }

    // Field反射
    private fun accessField(
        classDeclaration: KSClassDeclaration,
        property: KSPropertyDeclaration,
        codeBuilder: CodeBlock.Builder,
        implClass: TypeSpec.Builder
    ) {
        property.annotations.forEach { psAnno ->
            val fAnt = psAnno.annotationType.toTypeName().toString().replace("`", "")
            val fAv = psAnno.arguments.firstOrNull { arg -> arg.name?.asString() == "targetType" }?.value?.toString()
            when (fAnt) {
                PField::class.java.name -> accessObjField(fAv, false, property, codeBuilder, implClass, classDeclaration)
                PStaticField::class.java.name -> accessObjField(fAv, true, property, codeBuilder, implClass, classDeclaration)
                else -> Unit
            }
        }
    }

    private fun invokeMethod(
        isStatic: Boolean,
        mAv: String?,
        fct: KSFunctionDeclaration,
        codeBuilder: CodeBlock.Builder,
        implClass: TypeSpec.Builder,
        classDeclaration: KSClassDeclaration
    ) {
        val methodName = fct.simpleName.asString()
        val methodInfo = prepareMethodInfo(fct)
        val invokeArg = LinkedHashMap<String, Any>()

        invokeArg["obj"] = if (isStatic) "null" else "obj"
        invokeArg["clazz"] = "__originClass__()"
        invokeArg["methodName"] = methodName
        invokeArg["sections"] = methodInfo.sections

        codeBuilder
            .addStatement("parameterTypes == %S &&", methodInfo.proxyTypes.joinToString(transform = String::kotlinToJava))
            .indent()
            .beginControlFlow(
                "methodName == %S -> ",
                methodName
            )
            .addNamed("val proxyResult = Reflect.invokeMethod(%obj:L, %clazz:L, %methodName:S, $isStatic, *%sections:L)", invokeArg)
            .addStatement("")
            .add(wrapResultCode(fct.returnType, mAv))
            .endControlFlow()
            .unindent()

        if (isStatic) {
            // 生成Companion对象的扩展函数，用于访问静态方法
            val funSpec = FunSpec.builder(methodName)
                .receiver(classDeclaration.toClassName().nestedClass("Companion"))
                .addModifiers(KModifier.PUBLIC)
                .returns(fct.returnType!!.toTypeName())
                .addNamedCode("val proxyResult = Reflect.invokeMethod(%obj:L, %clazz:L, %methodName:S, true, *%sections:L)", invokeArg)
                .addStatement("")
                .addCode("return %L as %T", wrapResultCode(fct.returnType, mAv), fct.returnType?.toTypeName())
                .build()
            implClass.addFunction(funSpec)
        }
    }

    // 构造函数反射
    private fun invokeConstructor(mAv: String, fct: KSFunctionDeclaration, codeBuilder: CodeBlock.Builder) {
        val methodInfo = prepareMethodInfo(fct)
        val typeCode = getTypeCode(mAv)
        codeBuilder
            .beginControlFlow(
                "methodName == %S && parameterTypes == %S -> ",
                fct.simpleName.asString(), methodInfo.proxyTypes.joinToString(transform = String::kotlinToJava)
            )
            .add("Reflect.newInstance(%L , *%L)", typeCode, methodInfo.sections)
            .endControlFlow()
    }

    private fun accessObjField(
        fAv: String?,
        isStatic: Boolean,
        pct: KSPropertyDeclaration,
        codeBuilder: CodeBlock.Builder,
        implClass: TypeSpec.Builder,
        classDeclaration: KSClassDeclaration
    ) {
        val fieldName = pct.simpleName.asString()
        val getName = getMethod(fieldName)
        val setName = setMethod(fieldName)
        codeBuilder
            .beginControlFlow(
                "methodName == %S && parameterTypes == %S -> ",
                getName, ""
            )
            .addStatement(
                "val proxyResult = Reflect.getField(%L, %L, %L, %S)",
                if (isStatic) "null" else "obj",
                "__originClass__()", isStatic, fieldName
            )
            .add(wrapResultCode(pct.type, fAv))
            .endControlFlow()
            .beginControlFlow(
                "methodName == %S && parameterTypes == %S -> ",
                setName, pct.type.toTypeName().toString().replace("`", "").kotlinToJava()
            )
            .addStatement(
                "Reflect.setField(%L, %L, %L, %S, %L)",
                if (isStatic) "null" else "obj",
                "__originClass__()", isStatic, fieldName,
                wrapParameterCode(pct.type, fAv, 0)
            )
            .endControlFlow()
        if (isStatic) {
            companionStaticField(fAv, pct, implClass, classDeclaration)
        }
    }

    // 生成Companion对象的扩展函数，用于访问静态属性
    private fun companionStaticField(fAv: String?, pct: KSPropertyDeclaration, implClass: TypeSpec.Builder, classDeclaration: KSClassDeclaration) {
        val fieldName = pct.simpleName.asString()
        val getName = getMethod(fieldName)
        val setName = setMethod(fieldName)
        val typeName = pct.type.toTypeName()
        val getCode = if (fAv.isNullOrBlank()) {
            CodeBlock.builder().addStatement(
                "return Reflect.getField(%L, %L, %L, %S) as %L",
                "null", "__originClass__()", true, fieldName,
                typeName.toString().replace("`", "")
            )
        } else if (typeName.toString().contains("kotlin.Any")) {
            CodeBlock.builder().addStatement(
                "return Reflect.getField(%L, %L, %L, %S)",
                "null", "__originClass__()", true, fieldName
            )
        } else {
            CodeBlock.builder()
                .addStatement(
                    "val proxyResult = Reflect.getField(%L, %L, %L, %S)",
                    "null", "__originClass__()", true, fieldName
                )
                .add("return %L", wrapResultCode(pct.type, fAv))
        }

        val setCode = if (fAv.isNullOrBlank()) {
            CodeBlock.builder()
                .addStatement(
                    "Reflect.setField(%L, %L, %L, %S, %L)",
                    "null", "__originClass__()", true, fieldName, "newValue"
                )

        } else if (typeName.toString().contains("kotlin.Any")) {
            CodeBlock.builder()
                .addStatement(
                    "Reflect.setField(%L, %L, %L, %S, %L)",
                    "null", "__originClass__()", true, fieldName, "newValue"
                )
        } else {
            CodeBlock.builder()
                .addStatement(
                    "Reflect.setField(%L, %L, %L, %S, %L._getOriginObject_())",
                    "null", "__originClass__()", true, fieldName,
                    if (typeName.toString().endsWith("?")) "newValue?" else "newValue"
                )
        }

        val getSpec = FunSpec.builder(getName)
            .receiver(classDeclaration.toClassName().nestedClass("Companion"))
            .addModifiers(KModifier.PUBLIC)
            .returns(typeName)
            .addCode(getCode.build())
            .build()


        val setSpec = FunSpec.builder(setName)
            .receiver(classDeclaration.toClassName().nestedClass("Companion"))
            .addParameter("newValue", typeName)
            .addModifiers(KModifier.PUBLIC)
            .addCode(setCode.build())
            .build()

        implClass.addFunction(getSpec)
        implClass.addFunction(setSpec)
    }

    private fun getMethod(fieldName: String): String {
        return "get${fieldName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}"
    }

    private fun setMethod(fieldName: String): String {
        return "set${fieldName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}"
    }

    private fun originFunction(classDeclaration: KSClassDeclaration, clzAv: String): FunSpec {
        return FunSpec.builder("__originClass__")
            .receiver(classDeclaration.toClassName().nestedClass("Companion"))
            .addModifiers(KModifier.PUBLIC)
            .returns(ClassName("java.lang", "Class").parameterizedBy(STAR))
            .addStatement("return Class.forName(%S)", clzAv)
            .build()
    }

    private fun wrapParameterCode(ksTypeReference: KSTypeReference, annoValue: String?, pIndex: Int): CodeBlock {
        val paraType = ksTypeReference.toTypeName().toString().replace("`", "").kotlinToJava()
        val codeBuilder = CodeBlock.builder()
        if (annoValue.isNullOrBlank()
            || paraType == "java.lang.Object"
            || paraType == annoValue
        ) {
            codeBuilder.add("realArgs[%L]", pIndex)
        } else {
            codeBuilder.add("(realArgs[%L] as %L)._getOriginObject_()", pIndex, paraType)
        }
        return codeBuilder.build()
    }

    private fun wrapResultCode(ksTypeReference: KSTypeReference?, targetType: String?): CodeBlock {
        val returnType = ksTypeReference?.toTypeName()?.toString()?.replace("`", "")?.kotlinToJava()
        val codeBuilder = CodeBlock.builder()
        if (targetType.isNullOrBlank() || returnType.isNullOrBlank()
            || "java.lang.Object" == returnType
            || targetType == returnType
        ) {
            codeBuilder.addStatement("proxyResult")
        } else {
            val declaration = ksTypeReference.resolve().declaration
            val suffix = outerClassNamePrefix(declaration)
            val packageName = declaration.packageName.asString()
            val simpleName = declaration.simpleName.asString().removeSuffix("?").split(".").last()
            codeBuilder
                .addStatement(
                    "if (null == proxyResult)  null  else  %L%T(proxyResult)",
                    returnType.removeSuffix("?") + ".",
                    ClassName("${packageName}.$suffix${simpleName}__Functions", "__instance__")
                )
        }
        return codeBuilder.build()
    }

    private fun getTypeCode(type: String): CodeBlock {
        val typeBuilder = CodeBlock.builder()
        if (kotlinBuiltIns.contains(type))
            typeBuilder.add("%L::class.java", type)
        else
            typeBuilder.add("Class.forName(%S)", type)
        return typeBuilder.build()
    }

    private fun prepareMethodInfo(fct: KSFunctionDeclaration): MethodInfo {
        val proxyTypes = mutableListOf<String>()
        val realTypes = mutableListOf<String>()
        val sectionsBuilder = CodeBlock.builder()

        if (fct.parameters.isEmpty())
            sectionsBuilder.add("arrayOf(")
        else
            sectionsBuilder.addStatement("arrayOf(⇥")

        fct.parameters.forEachIndexed { index, par ->
            // 参数类型
            val parType = par.type.toTypeName().toString().replace("`", "")
            val proxyJavaType = parType.kotlinToJava()
            val proxyKotlinType = parType.toJavaType()
            // 方法参数注解
            val fAv = par.annotations.firstOrNull {
                PMethodParameter::class.java.name == it.annotationType.toTypeName().toString().replace("`", "")
            }?.arguments?.firstOrNull { "targetType" == it.name?.asString() }?.value?.toString()
            val realJavaType = if (fAv.isNullOrBlank()) proxyJavaType else fAv.kotlinToJava()
            val realKotlinType = if (fAv.isNullOrBlank()) proxyKotlinType else fAv.removeSuffix("?")
            val realArg = wrapParameterCode(par.type, fAv, index)
            proxyTypes.add(proxyJavaType)
            realTypes.add(realJavaType)
            sectionsBuilder.addStatement("Section(%L,%L),", realArg, getTypeCode(realKotlinType))
        }

        if (fct.parameters.isEmpty())
            sectionsBuilder.add(")")
        else
            sectionsBuilder.addStatement(")")
        return MethodInfo(proxyTypes, realTypes, sectionsBuilder.build())
    }

    private fun outerClassNamePrefix(classDeclaration: KSDeclaration?): String {
        var prefix = ""
        var cld = classDeclaration?.parentDeclaration as? KSClassDeclaration
        while (true) {
            if (cld == null) {
                break
            } else {
                prefix += cld.simpleName.asString() + "_"
                cld = cld.parentDeclaration as? KSClassDeclaration
            }
        }
        return prefix
    }

    data class MethodInfo(
        val proxyTypes: MutableList<String>,
        val realTypes: MutableList<String>,
        val sections: CodeBlock
    )
}