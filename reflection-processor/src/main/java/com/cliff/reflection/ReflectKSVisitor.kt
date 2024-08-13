package com.cliff.reflection

import com.cliff.reflection.common.ProxyBaseImpl
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
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class ReflectKSVisitor(private val logger: KSPLogger, private val codeGenerator: CodeGenerator) :
    KSVisitorVoid() {
    private val implFileSuffix = "ReImpl"
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val clzAno = classDeclaration.annotations.first {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == ProxyClass::class.java.name
        }

        val clzAv = clzAno.arguments.first { it.name!!.asString() == "value" }.value.toString()
        val dotIndex = clzAv.lastIndexOf(".")
        val originClz =
            ClassName(clzAv.substring(0, dotIndex), clzAv.substring(dotIndex + 1, clzAv.length))
        val proxyClz = classDeclaration.toClassName()

        val implFile = FileSpec.builder(proxyClz.packageName,  "${proxyClz.simpleName}${implFileSuffix}.kt")
        val implClass = TypeSpec.objectBuilder("${proxyClz.simpleName}${implFileSuffix}")
            .superclass(ProxyBaseImpl::class)
            .addSuperclassConstructorParameter(CodeBlock.of("%S",originClz.canonicalName))

        classDeclaration.getAllProperties().forEach { property ->
            val propertyName = property.simpleName.asString()
            property.annotations.forEach { anno ->
                val pAnType = anno.annotationType.resolve().declaration.qualifiedName?.asString()
                if (pAnType == PField::class.java.name || pAnType == PStaticField::class.java.name) {

                    val pAv = anno.arguments.firstOrNull { arg ->
                        arg.name?.asString() == "value"
                    }?.value?.toString()

                    val fieldType: TypeName = if (pAv.isNullOrBlank()) {
                        property.type.toTypeName()
                    } else {
                        val index = pAv.lastIndexOf('.')
                        ClassName(
                            pAv.substring(0, index),
                            pAv.substring(index + 1, pAv.length)
                        )
                    }

                    when (pAnType) {
                        PField::class.java.name -> {
                            implClass.addFunctions(
                                listOf(
                                    objFieldGet(
                                        propertyName,
                                        fieldType,
                                        property.type.toTypeName()
                                    ),
                                    objFieldSet(propertyName, fieldType, property.type.toTypeName())
                                )
                            )
                        }

                        PStaticField::class.java.name -> {
                            implClass.addFunctions(
                                listOf(
                                    staticFieldGet(
                                        propertyName,
                                        fieldType,
                                        property.type.toTypeName()
                                    ),
                                    staticFieldSet(
                                        propertyName,
                                        fieldType,
                                        property.type.toTypeName()
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }

        classDeclaration.getAllFunctions().forEach { fct ->
            val funcName = fct.simpleName.asString()
            fct.annotations.forEach { ksAnno ->
                val fAnt = ksAnno.annotationType.resolve().declaration.qualifiedName?.asString()
                // 真实参数类型
                val parTypes = fct.parameters.map { par ->
                    // 函数参数注解的值
                    val fAv = par.annotations.firstOrNull { para ->
                        para.annotationType.resolve().declaration.qualifiedName?.asString() == PMethodParameter::class.java.name
                    }?.arguments?.firstOrNull{ kv -> kv.name!!.asString() == "value" }?.value?.toString()

                    // 如果没有参数注解，则使用声明的参数类型
                    if(fAv.isNullOrBlank()) {
                        extractType(par.type.toTypeName().toString())
                    } else {
                        extractType(fAv)
                    }
                }

                when (fAnt) {
                    PMethod::class.java.name -> {
                        implClass.addFunction(invokeMethod(funcName, fct, parTypes))
                    }

                    PStaticMethod::class.java.name -> {
                        implClass.addFunction(invokeStaticMethod(funcName, fct, parTypes))
                    }

                    PConstructor::class.java.name -> {
                        implClass.addFunction(
                            invokeConstructor(ksAnno, funcName, fct, parTypes)
                        )
                    }
                }
            }
        }
        implFile.addType(implClass.build())
            .addImport("com.cliff.reflection.common", "Section")
            .build()
            .writeTo(codeGenerator, Dependencies(true, classDeclaration.containingFile!!))
    }

    // 生成属性get方法
    private fun objFieldGet(
        propertyName: String,
        fieldType: TypeName,
        returnType: TypeName
    ): FunSpec {
        val suffix = if (fieldType.toString().endsWith("?")) {
            ""
        } else {
            "!!"
        }
        val ft = CodeBlock.of("%S",fieldType.toString()).toString().replace("`","")
        return FunSpec.builder(propertyName + getSuffix)
            .addParameter("obj", Any::class)
            .addCode(
                CodeBlock.of(
                    """
                    return getFiled(
                    obj,
                    "$propertyName",
                    $ft,
                    false)$suffix
                """.trimIndent()
                )
            )
            .returns(returnType)
            .build()
    }

    // 生成静态属性set方法
    private fun staticFieldGet(
        propertyName: String,
        fieldType: TypeName,
        returnType: TypeName
    ): FunSpec {
        val suffix = if (fieldType.toString().endsWith("?")) {
            ""
        } else {
            "!!"
        }
        val ft = CodeBlock.of("%S",fieldType.toString()).toString().replace("`","")
        return FunSpec.builder(propertyName + getStaticSuffix)
            .addCode(
                CodeBlock.of(
                    """
                    return getFiled(
                    null,
                    "$propertyName",
                    $ft,
                    true)${suffix}
                    """.trimIndent()
                )
            )
            .returns(returnType)
            .build()
    }

    // 生成属性set方法
    private fun objFieldSet(
        propertyName: String,
        fieldType: TypeName,
        valueType: TypeName
    ): FunSpec {
        val ft = CodeBlock.of("%S",fieldType.toString()).toString().replace("`","")
        return FunSpec.builder(propertyName + setSuffix)
            .addParameter("obj", Any::class)
            .addParameter("value", valueType)
            .addCode(
                CodeBlock.of(
                    """
                    setFiled(
                    obj,
                    "$propertyName",
                    value,
                    $ft,
                    false)
                    """.trimIndent()
                )
            )
            .build()
    }

    // 生成静态属性set方法
    private fun staticFieldSet(
        propertyName: String,
        fieldType: TypeName,
        valueType: TypeName
    ): FunSpec {
        val ft = CodeBlock.of("%S",fieldType.toString()).toString().replace("`","")
        return FunSpec.builder(propertyName + setStaticSuffix)
            .addParameter("value", valueType)
            .addCode(
                CodeBlock.of(
                    """
                    return setFiled(null,"$propertyName",value,
                    $ft,
                    true)
                    """.trimIndent()
                )
            )
            .build()
    }

    // 生成成员方法访问函数
    private fun invokeMethod(
        funcName: String,
        fct: KSFunctionDeclaration,
        parTypes: List<String>
    ): FunSpec {
        return FunSpec.builder(funcName)
            .addModifiers(fct.modifiers.map { mod ->
                mod.toKModifier()!!
            })
            .addParameter("obj", Any::class)
            .addParameters(
                fct.parameters.map { kp ->
                    ParameterSpec(kp.name!!.asString(), kp.type.toTypeName())
                }
            )
            .returns(fct.returnType!!.toTypeName())
            .addCode(
                CodeBlock.of(
                    """
                    return invokeMethod(obj,"$funcName", false ${if (fct.parameters.isNotEmpty()) "," else ""} ${
                        fct.parameters.mapIndexed { index, par ->
                            "Section($par," +
                                    "try { Class.forName(\"${parTypes[index]}\") } catch (exp:ClassNotFoundException) { " +
                                    "${ if(parTypes[index].contains("kotlin.")) "${parTypes[index]}::class.java })" else "throw exp })" } "
                        }.joinToString(",")
                    })
                            """.trimIndent(),
                )
            )
            .build()
    }

    // 生成静态方法访问函数
    private fun invokeStaticMethod(
        funcName: String,
        fct: KSFunctionDeclaration,
        parTypes: List<String>
    ): FunSpec {
        return FunSpec.builder(funcName)
            .addModifiers(fct.modifiers.map { mod ->
                mod.toKModifier()!!
            })
            .addParameters(
                fct.parameters.map { kp ->
                    ParameterSpec(kp.name!!.asString(), kp.type.toTypeName())
                }
            )
            .returns(fct.returnType!!.toTypeName())
            .addCode(
                CodeBlock.of(
                    """
                    return invokeMethod(null, "$funcName", true ${if (fct.parameters.isNotEmpty()) "," else ""} ${
                        fct.parameters.mapIndexed { index, par ->
                            "Section($par," +
                                    "try { Class.forName(\"${parTypes[index]}\") } catch (exp:ClassNotFoundException) { " +
                                    "${ if(parTypes[index].contains("kotlin.")) "${parTypes[index]}::class.java })" else "throw exp })" } "
                        }.joinToString(",")
                    })
                            """.trimIndent()
                )
            )
            .build()
    }

    // 生成构造器访问函数
    private fun invokeConstructor(
        funAnno: KSAnnotation,
        funcName: String,
        fct: KSFunctionDeclaration,
        parTypes: List<String>
    ): FunSpec {
        val anoValue =
            funAnno.arguments.firstOrNull { arg -> arg.name?.asString() == "value" }?.value?.toString()
        val fieldType: String = if (anoValue.isNullOrBlank()) {
            fct.returnType?.toTypeName().toString()
                .removeSuffix("?") + "::class.java.name"
        } else {
            "\"" + anoValue + "\""
        }
        return FunSpec.builder(funcName)
            .addModifiers(fct.modifiers.map { mod ->
                mod.toKModifier()!!
            })
            .addParameters(
                fct.parameters.map { kp ->
                    ParameterSpec(kp.name!!.asString(), kp.type.toTypeName())
                }
            )
            .returns(fct.returnType!!.toTypeName())
            .addCode(
                CodeBlock.of(
                    """
                    return invokeConstructor($fieldType ${if (fct.parameters.isNotEmpty()) "," else ""} ${
                        fct.parameters.mapIndexed { index, par ->
                            "Section($par," +
                                    "try { Class.forName(\"${parTypes[index]}\") } catch (exp:ClassNotFoundException) { " +
                                    "${ if(parTypes[index].contains("kotlin.")) "${parTypes[index]}::class.java })" else "throw exp })" } "
                        }.joinToString(",")
                    })
                            """.trimIndent()
                )
            )
            .build()
    }

    private val getSuffix = "_o_get_"
    private val setSuffix = "_o_set_"
    private val getStaticSuffix = "_s_get_"
    private val setStaticSuffix = "_s_set_"

}