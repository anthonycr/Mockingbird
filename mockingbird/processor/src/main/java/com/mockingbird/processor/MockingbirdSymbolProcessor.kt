package com.mockingbird.processor

import com.mockingbird.core.Verifiable
import com.mockingbird.core.Verify
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class MockingbirdSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val fakes = resolver.getSymbolsWithAnnotation(Verify::class.qualifiedName!!)
            .filterIsInstance<KSPropertyDeclaration>()
            .map { it.type.resolve().declaration }
            .filterIsInstance<KSClassDeclaration>()
            .associateBy { it.qualifiedName!!.asString() }
            .map { (name, declaration) ->
                val fakeTypeSpec = generateFakeImplementation(declaration)

                FileSpec.builder(
                    declaration.packageName.asString(),
                    fakeTypeSpec.name!!
                )
                    .addImport("kotlin.collections", "forEach")
                    .addImport("kotlin.collections", "getOrPut")
                    .addType(fakeTypeSpec)
                    .build()
                    .writeTo(codeGenerator, true)
                logger.warn("$name $declaration")
                Pair(declaration, fakeTypeSpec)
            }

        if (fakes.isNotEmpty()) {
            generateFakeFunctions(fakes.associate { it }).writeTo(codeGenerator, true)
        }

        return emptyList()
    }

    private fun generateFakeFunctions(
        fakes: Map<KSClassDeclaration, TypeSpec>
    ): FileSpec {
        return FileSpec.builder("com.mockingbird.core", "Fakes")
            .addFunction(
                FunSpec.builder("fake")
                    .addTypeVariable(TypeVariableName("reified T"))
                    .returns(TypeVariableName("T"))
                    .addModifiers(KModifier.INLINE)
                    .addStatement("return fake(T::class.java)")
                    .build()
            )
            .addFunction(
                FunSpec.builder("fake")
                    .addTypeVariable(TypeVariableName("T"))
                    .addParameter(
                        "clazz",
                        ClassName("java.lang", "Class").parameterizedBy(TypeVariableName("T"))
                    )
                    .returns(TypeVariableName("T"))
                    .beginControlFlow("return when(clazz.canonicalName)")
                    .apply {
                        fakes.forEach { (declaration, fake) ->
                            val className =
                                ClassName(declaration.packageName.asString(), fake.name!!)
                            addStatement("\"${declaration.qualifiedName!!.asString()}\" -> clazz.cast(${className.canonicalName}())!!")
                        }
                        addStatement("else -> error(\"Unsupported type \$clazz\")")
                    }
                    .endControlFlow()
                    .build()
            )
            .build()
    }

    private fun generateFakeImplementation(interfaceDeclaration: KSClassDeclaration): TypeSpec {
        val interfaceName = interfaceDeclaration.simpleName.asString()

        val implementationClassName = "${interfaceName}_Fake"

        val implementationTypeSpec = TypeSpec.classBuilder(implementationClassName)
            .addModifiers(KModifier.PUBLIC)
            .addSuperinterface(interfaceDeclaration.asStarProjectedType().toTypeName())
            .addSuperinterface(Verifiable::class.asTypeName())

        implementationTypeSpec.addProperty(
            PropertySpec.builder(
                "invocations",
                ClassName("kotlin.collections", "MutableList").parameterizedBy(
                    Pair::class.asTypeName().parameterizedBy(
                        String::class.asTypeName(),
                        List::class.asClassName().parameterizedBy(Any::class.asTypeName())
                    )
                )
            )
                .initializer("mutableListOf()")
                .addModifiers(KModifier.OVERRIDE)
                .build()
        )

        implementationTypeSpec.addProperty(
            PropertySpec.builder(
                "verifying",
                Boolean::class.asClassName()
            )
                .initializer("false")
                .addModifiers(KModifier.OVERRIDE)
                .mutable(true).build()
        )

        implementationTypeSpec.addProperty(
            PropertySpec.builder(
                "expected",
                Int::class.asClassName()
            )
                .initializer("1")
                .addModifiers(KModifier.OVERRIDE)
                .mutable(true).build()
        )

        for (function in interfaceDeclaration.declarations.filterIsInstance<KSFunctionDeclaration>()) {
            val funSpec = FunSpec.builder(function.simpleName.asString())
                .addModifiers(KModifier.OVERRIDE)
                .returns(Unit::class)

            for (param in function.parameters) {
                funSpec.addParameter(param.name!!.asString(), param.type.toTypeName())
            }

            funSpec.beginControlFlow("if (verifying)")
                .addStatement("val expectedInvocations = invocations.take(expected)")
                .addStatement("invocations.removeAll(expectedInvocations)")
                .beginControlFlow("check(expectedInvocations.size == expected)")
                .addStatement(
                    "\"Expected %1Lexpected invocations, but got %1L{expectedInvocations.size} instead.\"".noWrap(),
                    "$"
                )
                .endControlFlow()
                .beginControlFlow("expectedInvocations.forEach")
                .apply {
                    beginControlFlow("check(it.first == %S)", function.qualifiedName!!.asString())
                    addStatement(
                        "\"Expected function call %1L, %2L{it.first} was called instead\"".noWrap(),
                        function.qualifiedName!!.asString(),
                        "$"
                    )
                    endControlFlow()
                    function.parameters.forEachIndexed { index, value ->
                        val name = value.name!!.asString()
                        beginControlFlow("check(%1L == it.second[%2L])", name, index)
                        addStatement(
                            "\"Expected argument %1L%2L, found %1L{it.second[%3L]} instead.\"".noWrap(),
                            "$",
                            name,
                            index
                        )
                        endControlFlow()
                    }
                }
                .endControlFlow()
                .nextControlFlow("else")
                .addStatement(
                    "invocations.add(Pair(%1S, listOf(%2L)))",
                    function.qualifiedName!!.asString(),
                    function.parameters.joinToString { it.name!!.asString() })
                .endControlFlow()

            implementationTypeSpec.addFunction(funSpec.build())
        }


        return implementationTypeSpec.build()
    }

    private fun String.noWrap(): String = this.replace(" ", "·")


}
