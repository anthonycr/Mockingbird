package com.anthonycr.mockingbird.processor

import com.anthonycr.mockingbird.core.Verify
import com.anthonycr.mockingbird.processor.internal.check
import com.anthonycr.mockingbird.processor.internal.generator.FakeFunctionGenerator
import com.anthonycr.mockingbird.processor.internal.generator.FakeImplementationGenerator
import com.anthonycr.mockingbird.processor.internal.isInterface
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.writeTo

class MockingbirdSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val fakeImplementationGenerator: FakeImplementationGenerator,
    private val fakeFunctionGenerator: FakeFunctionGenerator
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotated = resolver.getSymbolsWithAnnotation(Verify::class.qualifiedName!!)

        val fakes = annotated
            .check(
                message = "Only properties can be annotated with Verify",
                logger = logger,
                node = { it },
                condition = { it is KSPropertyDeclaration }
            )
            .map { declaration ->
                require(declaration is KSPropertyDeclaration)

                val ksType = declaration.type.resolve()
                ksType to ksType.declaration
            }
            .check(
                message = "Only interfaces can be verified",
                logger = logger,
                node = Pair<KSType, KSDeclaration>::second,
                condition = { (_, declaration) -> declaration.isInterface }
            )
            .distinctBy { (_, declaration) -> declaration.qualifiedName!!.asString() }
            .map { (ksType, declaration) ->
                require(declaration is KSClassDeclaration)

                val (typeSpec, fileSpec) = fakeImplementationGenerator.generate(ksType, declaration)
                fileSpec.writeTo(codeGenerator, true)
                logger.info("Generating fake for: ${declaration.qualifiedName}")

                Pair(declaration, typeSpec)
            }
            .associate { it }

        if (fakes.isNotEmpty()) {
            fakeFunctionGenerator.generate(fakes).writeTo(codeGenerator, true)
        }

        return emptyList()
    }
}
