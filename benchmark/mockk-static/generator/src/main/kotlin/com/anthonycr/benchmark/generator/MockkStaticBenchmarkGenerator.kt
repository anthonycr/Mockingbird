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
import org.junit.After
import org.junit.Before
import org.junit.Test

class MockkStaticBenchmarkGenerator(
    private val codeGenerator: CodeGenerator,
    private val objectCount: Int,
    private val testCount: Int
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val toGenerate = resolver.getSymbolsWithAnnotation("com.anthonycr.benchmark.executor.Generate")
        if (toGenerate.toList().isEmpty()) return emptyList()

        val objects = 1.rangeTo(objectCount).map {
            ClassName("com.anthonycr.benchmark.objects", "Object$it")
        }

        objects.forEach {
            val typeSpec = TypeSpec.objectBuilder(it.simpleName)
                .addFunction(
                    FunSpec.builder("test")
                        .addParameter(ParameterSpec.builder("number", String::class).build())
                        .addCode(CodeBlock.builder().add("println(%S)", "test").build())
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
            val properties = objects.map {
                PropertySpec.builder(it.simpleName.lowercase(), it)
                    .mutable(false)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(
                        CodeBlock.builder()
                            .addStatement("io.mockk.mockk(relaxed = true)")
                            .build()
                    )
                    .build()
            }

            val test = TypeSpec.classBuilder("Test$testCount")
                .addProperties(properties)
                .addFunction(
                    FunSpec.builder("setup")
                        .addAnnotation(Before::class)
                        .apply {
                            properties.forEach {
                                addCode(CodeBlock.builder()
                                    .addStatement("io.mockk.mockkStatic(${it.type}::class)")
                                    .build())
                            }
                        }
                        .build()
                )
                .addFunction(
                    FunSpec.builder("teardown")
                        .addAnnotation(After::class)
                        .addCode(
                            CodeBlock.builder()
                                .addStatement("io.mockk.unmockkAll()")
                                .build()
                        )
                        .build()
                )
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
                                .beginControlFlow("io.mockk.verifyOrder")
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
