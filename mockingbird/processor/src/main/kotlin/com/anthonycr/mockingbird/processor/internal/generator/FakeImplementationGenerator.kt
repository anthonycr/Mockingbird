package com.anthonycr.mockingbird.processor.internal.generator

import com.anthonycr.mockingbird.core.Verifiable
import com.anthonycr.mockingbird.processor.internal.isInterface
import com.anthonycr.mockingbird.processor.internal.safePackageName
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

/**
 * Used to generate the fake implementation of an interface.
 */
class FakeImplementationGenerator {

    /**
     * Generate the [TypeSpec] and [FileSpec] for a fake implementation of an interface.
     */
    fun generate(
        propertyDeclarations: List<KSPropertyDeclaration>,
        declaration: KSClassDeclaration
    ): Pair<TypeSpec, FileSpec> {
        val fakeTypeSpec = generateFakeImplementation(propertyDeclarations, declaration)

        return fakeTypeSpec to FileSpec.builder(
            declaration.safePackageName,
            fakeTypeSpec.name!!
        )
            .addImport("kotlin.collections", "forEach")
            .addImport("kotlin.collections", "getOrPut")
            .addType(fakeTypeSpec)
            .build()
    }

    private fun generateFakeImplementation(
        propertyDeclarations: List<KSPropertyDeclaration>,
        interfaceDeclaration: KSClassDeclaration
    ): TypeSpec {
        val interfaceName = interfaceDeclaration.simpleName.asString()
        val implementationClassName = "${interfaceName}_Fake"

        val typeParameters = interfaceDeclaration.typeParameters.map {
            it.bounds.first().toTypeName()
        }
        val implementationTypeSpec = TypeSpec.classBuilder(implementationClassName)
            .addModifiers(KModifier.PUBLIC)
            .apply {
                if (interfaceDeclaration.isInterface) {
                    addSuperinterface(
                        interfaceDeclaration.toClassName().maybeParameterizedBy(typeParameters)
                    )
                } else {
                    superclass(
                        interfaceDeclaration.toClassName().maybeParameterizedBy(typeParameters)
                    )
                }
            }
            .addSuperinterface(Verifiable::class.asTypeName())

        implementationTypeSpec.addProperty(
            PropertySpec.builder(
                "_mockingbird_invocations",
                ClassName("kotlin.collections", "MutableList").parameterizedBy(
                    Verifiable.Invocation::class.asTypeName()
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

        val unitTypeName = Unit::class.asTypeName()
        val anyTypeName = Any::class.asTypeName()
        val anyTypeNameNullable = Any::class.asTypeName().copy(nullable = true)
        fun KSTypeReference.resolveType(): TypeName? {
            val parameters = interfaceDeclaration.typeParameters.map { it.name.getShortName() }
            if (typeParameters.isEmpty()) {
                return toTypeName()
            }
            return typeParameters.getOrNull(parameters.indexOf(toString()))
        }

        val functions = interfaceDeclaration.declarations
            .filterIsInstance<KSFunctionDeclaration>()
            .filter { it.isAbstract }
        for (function in functions) {
            val returnType = function.returnType?.resolveType() ?: unitTypeName
            val funSpec = FunSpec.builder(function.simpleName.asString())
                .addModifiers(KModifier.OVERRIDE)
                .apply {
                    if (function.modifiers.contains(Modifier.SUSPEND)) {
                        addModifiers(KModifier.SUSPEND)
                    }
                }
                .returns(returnType)

            for (param in function.parameters) {
                funSpec.addParameter(
                    param.name!!.asString(),
                    param.type.resolveType() ?: unitTypeName
                )
            }

            if (returnType != unitTypeName && returnType != anyTypeName && returnType != anyTypeNameNullable) {
                funSpec.addStatement(
                    "%M(\"Only functions with return type Unit can be verified\")",
                    MemberName("kotlin", "error")
                )
                implementationTypeSpec.addFunction(funSpec.build())
                continue
            }

            val functionName = function.qualifiedName!!.asString()

            funSpec.beginControlFlow("if (_mockingbird_verifying)")
                .addStatement("val invocation = _mockingbird_invocations.firstOrNull()")
                .beginControlFlow("check(invocation != null)")
                .addStatement(
                    "\"Expected an invocation, but got none instead\"",
                )
                .endControlFlow()
                .addStatement("_mockingbird_invocations.removeAt(0)")
                .beginControlFlow("check(invocation.functionName == %S)", functionName)
                .addStatement(
                    "\"Expected function call %1L, \${invocation.functionName} was called instead\"",
                    functionName,
                )
                .endControlFlow()
                .apply {
                    if (function.parameters.isEmpty()) return@apply
                    beginControlFlow("val allParamVerifier = _mockingbird_paramMatcher.firstOrNull()?.takeIf { _ ->")
                    addStatement("_mockingbird_paramMatcher.size != invocation.parameters.size")
                    endControlFlow()
                    function.parameters.forEachIndexed { index, value ->
                        val name = value.name!!.asString()
                        beginControlFlow(
                            "check((allParamVerifier ?: _mockingbird_paramMatcher[%1L]).invoke(%2L, invocation.parameters[%1L]))",
                            index,
                            name
                        )
                        addStatement(
                            "\"Expected argument \$%1L, found \${invocation.parameters[%2L]} instead.\"",
                            name,
                            index
                        )
                        endControlFlow()
                    }
                    addStatement("_mockingbird_paramMatcher = listOf { e, a -> e == a }")
                }
                .nextControlFlow("else")
                .addStatement(
                    "_mockingbird_invocations.add(%1T(%2S, listOf(%3L)))",
                    Verifiable.Invocation::class.asClassName(),
                    functionName,
                    function.parameters.joinToString { it.name!!.asString() })
                .endControlFlow()
                .addStatement("return Unit")

            implementationTypeSpec.addFunction(funSpec.build())
        }

        propertyDeclarations.mapNotNull { it.containingFile }.forEach {
            implementationTypeSpec.addOriginatingKSFile(it)
        }
        interfaceDeclaration.containingFile?.let {
            implementationTypeSpec.addOriginatingKSFile(it)
        }

        return implementationTypeSpec.build()
    }

    private fun ClassName.maybeParameterizedBy(typeArguments: List<TypeName>): TypeName {
        return if (typeArguments.isEmpty()) {
            this
        } else {
            parameterizedBy(typeArguments)
        }
    }

}
