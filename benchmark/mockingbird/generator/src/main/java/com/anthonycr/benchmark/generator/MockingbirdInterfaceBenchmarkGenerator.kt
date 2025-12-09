package com.anthonycr.benchmark.generator

import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.writeTo
import org.junit.Test

class MockingbirdInterfaceBenchmarkGenerator(
    private val codeGenerator: CodeGenerator,
    private val interfaceCount: Int,
    private val testCount: Int
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val toGenerate =
            resolver.getSymbolsWithAnnotation("com.anthonycr.benchmark.executor.Generate")
        if (toGenerate.toList().isEmpty()) return emptyList()

        val interfaces = 1.rangeTo(interfaceCount).map {
            ClassName("com.anthonycr.benchmark.interfaces", "Interface$it")
        }

        interfaces.forEach {
            val typeSpec = TypeSpec.interfaceBuilder(it.simpleName)
                .addFunction(
                    FunSpec.builder("test")
                        .addParameter(ParameterSpec.builder("number", String::class).build())
                        .addModifiers(KModifier.ABSTRACT)
                        .build()
                )
                .addOriginatingKSFile(toGenerate.toList().first().containingFile!!)
                .build()

            FileSpec.builder(it)
                .addType(typeSpec)
                .build()
                .writeTo(codeGenerator, aggregating = true)
        }

        1.rangeTo(testCount).forEach { testCount ->
            val properties = interfaces.map {
                PropertySpec.builder(it.simpleName.lowercase(), it)
                    .mutable(false)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(
                        CodeBlock.builder()
                            .addStatement("com.anthonycr.mockingbird.core.fake()")
                            .build()
                    )
                    .build()
            }

            val test = TypeSpec.classBuilder("Test$testCount")
                .addProperties(properties)
                .addFunction(
                    FunSpec.builder("scenario")
                        .addAnnotation(Test::class)
                        .addCode(
                            CodeBlock.builder()
                                .apply {
                                    properties.forEach {
                                        addStatement("${it.name}.test(%S)", "test")
                                    }
                                }
                                .beginControlFlow(
                                    "com.anthonycr.mockingbird.core.verify(%L)",
                                    properties.joinToString { it.name })
                                .apply {
                                    properties.forEach {
                                        addStatement("${it.name}.test(%S)", "test")
                                    }
                                }
                                .endControlFlow()
                                .build()
                        )
                        .build()
                )
                .addOriginatingKSFile(toGenerate.toList().first().containingFile!!)
                .build()

            FileSpec.builder("com.anthonycr.benchmark.tests", test.name!!)
                .addType(test)
                .build()
                .writeTo(codeGenerator, aggregating = true)
        }



        return emptyList()
    }
}
