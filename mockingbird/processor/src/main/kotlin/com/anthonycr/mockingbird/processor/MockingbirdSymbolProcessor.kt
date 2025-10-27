package com.anthonycr.mockingbird.processor

import com.anthonycr.mockingbird.core.Verify
import com.anthonycr.mockingbird.processor.internal.check
import com.anthonycr.mockingbird.processor.internal.data.asKey
import com.anthonycr.mockingbird.processor.internal.generator.FakeFunctionGenerator
import com.anthonycr.mockingbird.processor.internal.generator.FakeImplementationGenerator
import com.anthonycr.mockingbird.processor.internal.isInterface
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Visibility
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
            .filterIsInstance<KSPropertyDeclaration>()
            .map { declaration -> declaration to declaration.type.resolve().declaration }
            .check(
                message = "Only interfaces and abstract classes can be verified",
                logger = logger,
                node = { (declaration, _) -> declaration },
                condition = { (_, resolvedDeclaration) ->
                    resolvedDeclaration is KSClassDeclaration &&
                            (resolvedDeclaration.isInterface || resolvedDeclaration.isAbstract())
                }
            )
            .check(
                message = "Package private Java files cannot be verified, please update the visibility modifier to public",
                logger = logger,
                node = { (declaration, _) -> declaration },
                condition = { (_, resolvedDeclaration) -> resolvedDeclaration.getVisibility() != Visibility.JAVA_PACKAGE }
            )
            .filterIsInstance<Pair<KSPropertyDeclaration, KSClassDeclaration>>()
            .check(
                message = "Only abstract classes with zero argument constructors can be verified",
                logger = logger,
                node = { (declaration, _) -> declaration },
                condition = { (_, resolvedDeclaration) ->
                    resolvedDeclaration.getConstructors()
                        .filter { it.parameters.isNotEmpty() }
                        .toList()
                        .isEmpty()
                }
            )
            .groupBy(
                keySelector = { (_, resolvedDeclaration) -> resolvedDeclaration.asKey() },
                valueTransform = { (propertyDeclaration, _) -> propertyDeclaration }
            )
            .map { (resolvedDeclarationKey, propertyDeclarations) ->
                val (resolvedDeclaration) = resolvedDeclarationKey
                val (typeSpec, fileSpec) = fakeImplementationGenerator.generate(
                    propertyDeclarations,
                    resolvedDeclaration
                )
                fileSpec.writeTo(codeGenerator, true)
                logger.info("Generating fake for: ${resolvedDeclaration.qualifiedName}")

                Triple(propertyDeclarations, resolvedDeclaration, typeSpec)
            }

        if (fakes.isNotEmpty()) {
            fakeFunctionGenerator.generate(fakes).writeTo(codeGenerator, true)
        }

        return emptyList()
    }
}
