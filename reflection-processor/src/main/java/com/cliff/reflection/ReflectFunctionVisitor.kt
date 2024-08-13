package com.cliff.reflection

import com.cliff.reflection.common.annotation.PConstructor
import com.cliff.reflection.common.annotation.PField
import com.cliff.reflection.common.annotation.PMethod
import com.cliff.reflection.common.annotation.PMethodParameter
import com.cliff.reflection.common.annotation.PStaticField
import com.cliff.reflection.common.annotation.PStaticMethod
import com.cliff.reflection.common.annotation.ProxyClass
import com.cliff.reflection.common.extractType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
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

class ReflectFunctionVisitor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator
) : KSVisitorVoid() {
    private val implFileSuffix = "__Functions"
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val clzAno = classDeclaration.annotations.first {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == ProxyClass::class.java.name
        }
        val clzAv = clzAno.arguments.first { it.name!!.asString() == "value" }.value.toString()
        val proxyClz = classDeclaration.toClassName()

        val implFile =
            FileSpec.builder(proxyClz.packageName, "${proxyClz.simpleName}${implFileSuffix}.kt")
        val implClass = TypeSpec.objectBuilder("${proxyClz.simpleName}${implFileSuffix}")

        implClass.addFunction(originFunction(classDeclaration, clzAv))
        implClass.addFunction(instanceFunction(classDeclaration))
        implFile.addType(implClass.build())
            .addImport("java.lang.reflect", "Proxy")
            .addImport("com.cliff.reflection.common", "Reflect")
            .addImport("com.cliff.reflection.common", "Section")
            .addImport("java.lang.ref", "WeakReference")
            .build()
            .writeTo(codeGenerator, Dependencies(true, classDeclaration.containingFile!!))
    }

    // 代理对象生成
    private fun instanceFunction(classDeclaration: KSClassDeclaration): FunSpec {
        val reflectBuilder = FunSpec.builder("reflectInstance")
            .addParameter("obj", ClassName("kotlin", "Any").copy(nullable = true))
            .receiver(classDeclaration.toClassName().nestedClass("Companion"))
            .returns(classDeclaration.toClassName())

        val codeBuilder = CodeBlock.builder()
            .addStatement("val weakObj = WeakReference(obj)")
            .beginControlFlow(
                "val proxy = Proxy.newProxyInstance(%L::class.java.classLoader , arrayOf(%L::class.java)) { _, method, args -> ",
                classDeclaration.toClassName().simpleName,
                classDeclaration.toClassName().simpleName
            )
            .addStatement("val realArgs = args ?: arrayOf()")
            .addStatement("val parameterTypes = method.parameterTypes.joinToString()")
            .addStatement("val methodName = method.name")
            .beginControlFlow("when {")
            .addStatement("%S == methodName -> %L", "_getOriginClass_", " __originClass__()")
            .addStatement("%S == methodName -> %L", "_getOriginObject_", "obj")

        classDeclaration.getAllFunctions()
            .forEach { invokeMethod(it, codeBuilder) }

        classDeclaration.getAllProperties()
            .forEach { accessField(it, codeBuilder) }

        reflectBuilder.addCode(codeBuilder.build())
            .addStatement("%L", "else -> null")
            .endControlFlow()
            .endControlFlow()
            .addStatement("return proxy as %L", classDeclaration.toClassName().simpleName)
        return reflectBuilder.build()
    }

    // Method反射
    private fun invokeMethod(fct: KSFunctionDeclaration, codeBuilder: CodeBlock.Builder) {
        fct.annotations.forEach { ksAnno ->
            val mAnt = ksAnno.annotationType.toTypeName().toString().replace("`", "")
            val mAv = ksAnno.arguments.firstOrNull { arg -> arg.name?.asString() == "value" }?.value?.toString()
            when (mAnt) {
                PMethod::class.java.name -> invokeMethod(false, mAv, fct, codeBuilder)
                PStaticMethod::class.java.name -> invokeMethod(true, mAv, fct, codeBuilder)
                PConstructor::class.java.name -> invokeConstructor(mAv!!, fct, codeBuilder)
                else -> Unit
            }
        }
    }

    // Field反射
    private fun accessField(property: KSPropertyDeclaration, codeBuilder: CodeBlock.Builder) {
        property.annotations.forEach { psAnno ->
            val fAnt = psAnno.annotationType.toTypeName().toString().replace("`", "")
            val fAv = psAnno.arguments.firstOrNull { arg -> arg.name?.asString() == "value" }?.value?.toString()
            when (fAnt) {
                PField::class.java.name -> accessObjField(fAv, false, property, codeBuilder)
                PStaticField::class.java.name -> accessObjField(fAv, true, property, codeBuilder)
                else -> Unit
            }
        }
    }

    private fun invokeMethod(isStatic: Boolean, mAv: String?, fct: KSFunctionDeclaration, codeBuilder: CodeBlock.Builder) {
        val methodInfo = prepareMethodInfo(fct)
        val invokeArg = LinkedHashMap<String, Any>()
        val returnType = fct.returnType?.toTypeName()?.toString()

        invokeArg["obj"] = "weakObj.get()"
        invokeArg["clazz"] = "__originClass__()"
        invokeArg["methodName"] = fct.simpleName.asString()
        invokeArg["sections"] = methodInfo.sections

        codeBuilder
            .addStatement("parameterTypes == %S &&", methodInfo.proxyTypes.joinToString())
            .indent()
            .beginControlFlow(
                "methodName == %S -> ",
                fct.simpleName.asString()
            )
            .addNamed("val proxyResult = Reflect.invokeMethod(%obj:L, %clazz:L, %methodName:S, $isStatic, *%sections:L)", invokeArg)
            .addStatement("")
            .add(wrapResultCode(returnType, mAv))
            .endControlFlow()
            .unindent()
    }

    // 构造函数反射
    private fun invokeConstructor(mAv: String, fct: KSFunctionDeclaration, codeBuilder: CodeBlock.Builder) {
        val methodInfo = prepareMethodInfo(fct)
        val typeCode = getTypeCode(mAv)
        codeBuilder
            .beginControlFlow(
                "methodName == %S && parameterTypes == %S -> ",
                fct.simpleName.asString(), methodInfo.proxyTypes.joinToString()
            )
            .add("Reflect.newInstance(%L , *%L)", typeCode, methodInfo.sections)
            .endControlFlow()
    }

    private fun accessObjField(fAv: String?, isStatic: Boolean, pct: KSPropertyDeclaration, codeBuilder: CodeBlock.Builder) {
        val fieldName = pct.simpleName.asString()
        val getName = getMethod(fieldName)
        val setName = setMethod(fieldName)
        val fieldType = pct.type.toTypeName().toString().replace("`", "")
        logger.warn("accessObjField: fieldName:$fieldName  fieldType:$fieldType  fAv:${fAv}")
        codeBuilder
            .beginControlFlow(
                "methodName == %S && parameterTypes == %S -> ",
                getName, ""
            )
            .addStatement(
                "val proxyResult = Reflect.getField(%L, %L, %L, %S)",
                if (isStatic) "null" else "weakObj.get()",
                "__originClass__()", isStatic, fieldName
            )
            .add(wrapResultCode(fieldType, fAv))
            .endControlFlow()
            .beginControlFlow(
                "methodName == %S && parameterTypes == %S -> ",
                setName, fieldType
            )
            .addStatement(
                "Reflect.setField(%L, %L, %L, %S, %L)",
                if (isStatic) "null" else "weakObj.get()",
                "__originClass__()", isStatic, fieldName,
                wrapParameterCode(fieldType, fAv, 0)
            )
            .endControlFlow()
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

    private fun wrapParameterCode(paraType: String, annoValue: String?, pIndex: Int): CodeBlock {
        val codeBuilder = CodeBlock.builder()
        if (annoValue.isNullOrBlank()
            || paraType.removeSuffix("?") == "kotlin.Any"
            || paraType == annoValue) {
            codeBuilder.add("realArgs[%L]", pIndex)
        } else {
            codeBuilder.addStatement("(realArgs[%L] as %L)?._getOriginObject_()", pIndex, paraType)
        }
        return codeBuilder.build()
    }

    private fun wrapResultCode(returnType: String?, annotationValue: String?): CodeBlock {
        val codeBuilder = CodeBlock.builder()
        if (annotationValue.isNullOrBlank() || returnType.isNullOrBlank() || "kotlin.Any" == returnType.removeSuffix("?")) {
            codeBuilder.addStatement("proxyResult")
        } else if (annotationValue != returnType) {
            codeBuilder.addStatement("%L.reflectInstance(proxyResult)", returnType.removeSuffix("?"))
        } else {
            codeBuilder.addStatement("proxyResult")
        }
        return codeBuilder.build()
    }

    private fun getTypeCode(type: String): CodeBlock {
        val typeBuilder = CodeBlock.builder()
        typeBuilder.add("try { Class.forName(%S)} catch (exp:ClassNotFoundException) {", type)
        if (type.startsWith("kotlin.")) {
            typeBuilder.add("%L::class.java }", type)
        } else {
            typeBuilder.add("throw exp }")
        }
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
            val proxyType = extractType(par.type.toTypeName().toString().replace("`", ""))
            val fAv = par.annotations.firstOrNull {
                PMethodParameter::class.java.name == it.annotationType.toTypeName().toString().replace("`", "")
            }?.arguments?.firstOrNull { "value" == it.name?.asString() }?.value?.toString()
            val realType = if (fAv.isNullOrBlank()) proxyType else extractType(fAv)
            val realArg = wrapParameterCode(proxyType,fAv,index)
            proxyTypes.add(proxyType)
            realTypes.add(realType)
            sectionsBuilder.addStatement("Section(%L,%L),", realArg, getTypeCode(realType))
        }

        if (fct.parameters.isEmpty())
            sectionsBuilder.add(")")
        else
            sectionsBuilder.addStatement(")")
        return MethodInfo(proxyTypes, realTypes, sectionsBuilder.build())
    }

    data class MethodInfo(
        val proxyTypes: MutableList<String>,
        val realTypes: MutableList<String>,
        val sections: CodeBlock
    )
}