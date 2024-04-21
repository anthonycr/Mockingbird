package com.anthonycr.mockingbird.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.anthonycr.mockingbird.core.Verifiable
import com.anthonycr.mockingbird.core.Verify
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
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
        val annotated = resolver.getSymbolsWithAnnotation(Verify::class.qualifiedName!!)

        val fakes = annotated
            .check("Only properties can be annotated with Verify") { it is KSPropertyDeclaration }
            .filterIsInstance<KSPropertyDeclaration>()
            .map { it.type.resolve().declaration }
            .check("Only interfaces can be verified") { it is KSClassDeclaration && it.classKind == ClassKind.INTERFACE }
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
                logger.info("Generating fake for: $name")
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
        return FileSpec.builder("com.anthonycr.mockingbird.core", "Fakes")
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
                "_mockingbird_invocations",
                ClassName("kotlin.collections", "MutableList").parameterizedBy(
                    Pair::class.asTypeName().parameterizedBy(
                        String::class.asTypeName(),
                        List::class.asClassName()
                            .parameterizedBy(Any::class.asTypeName().copy(nullable = true))
                    )
                )
            )
                .initializer("mutableListOf()")
                .addModifiers(KModifier.OVERRIDE)
                .build()
        )

        implementationTypeSpec.addProperty(
            PropertySpec.builder(
                "_mockingbird_paramMatcher",
                ClassName(
                    "kotlin.collections",
                    "List"
                ).parameterizedBy(
                    LambdaTypeName.get(
                        parameters = arrayOf(
                            Any::class.asTypeName().copy(nullable = true),
                            Any::class.asTypeName().copy(nullable = true)
                        ),
                        returnType = Boolean::class.asTypeName()
                    )
                )
            )
                .initializer("listOf { e, a -> e == a }")
                .addModifiers(KModifier.OVERRIDE)
                .mutable(true)
                .build()
        )

        implementationTypeSpec.addProperty(
            PropertySpec.builder(
                "_mockingbird_verifying",
                Boolean::class.asClassName()
            )
                .initializer("false")
                .addModifiers(KModifier.OVERRIDE)
                .mutable(true).build()
        )

        implementationTypeSpec.addProperty(
            PropertySpec.builder(
                "_mockingbird_expected",
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

            val functionName = function.qualifiedName!!.asString()

            funSpec.beginControlFlow("if (_mockingbird_verifying)")
                .addStatement("val expectedInvocations = _mockingbird_invocations.take(_mockingbird_expected)")
                .addStatement("_mockingbird_invocations.removeAll(expectedInvocations)")
                .beginControlFlow("check(expectedInvocations.size == _mockingbird_expected)")
                .addStatement(
                    "\"Expected \$_mockingbird_expected invocations, but got \${expectedInvocations.size} instead.\"".noWrap(),
                )
                .endControlFlow()
                .beginControlFlow("expectedInvocations.forEach")
                .apply {
                    beginControlFlow("check(it.first == %S)", functionName)
                    addStatement(
                        "\"Expected function call %1L, \${it.first} was called instead\"".noWrap(),
                        functionName,
                    )
                    endControlFlow()
                    beginControlFlow("val allParamVerifier = _mockingbird_paramMatcher.firstOrNull()?.takeIf { _ ->")
                    addStatement("_mockingbird_paramMatcher.size != it.second.size")
                    endControlFlow()
                    function.parameters.forEachIndexed { index, value ->
                        val name = value.name!!.asString()
                        beginControlFlow(
                            "check((allParamVerifier ?: _mockingbird_paramMatcher[%1L]).invoke(%2L, it.second[%1L]))",
                            index,
                            name
                        )
                        addStatement(
                            "\"Expected argument \$%1L, found \${it.second[%2L]} instead.\"".noWrap(),
                            name,
                            index
                        )
                        endControlFlow()
                    }
                    addStatement("_mockingbird_paramMatcher = listOf { e, a -> e == a }")
                }
                .endControlFlow()
                .nextControlFlow("else")
                .addStatement(
                    "_mockingbird_invocations.add(Pair(%1S, listOf(%2L)))",
                    functionName,
                    function.parameters.joinToString { it.name!!.asString() })
                .endControlFlow()

            implementationTypeSpec.addFunction(funSpec.build())
        }

        return implementationTypeSpec.build()
    }

    private fun <T : KSNode> Sequence<T>.check(
        message: String,
        condition: (T) -> Boolean
    ): Sequence<T> = filter {
        val passed = condition(it)

        if (!passed) {
            logger.error(message, it)
        }

        passed
    }

    private fun String.noWrap(): String = this.replace(" ", "·")

}
