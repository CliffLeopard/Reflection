package com.cliff.reflection

import com.cliff.reflection.common.annotation.ProxyClass
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

class ReflectSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Map<String, String>,
    private val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(ProxyClass::class.java.name)
        val result = symbols.filter { !it.validate() }.toList()
        symbols
            .filter {
                it is KSClassDeclaration && it.validate()
            }
            .forEach { annotation ->
                annotation.accept(ReflectVisitor(logger, codeGenerator), Unit)
            }
        return result
    }
}
