package com.anthonycr.mockingbird.processor

import com.anthonycr.mockingbird.core.Verifiable
import com.anthonycr.mockingbird.core.Verify
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
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
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.writeTo

class MockingbirdSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotated = resolver.getSymbolsWithAnnotation(Verify::class.qualifiedName!!)

        val fakes = annotated
            .check("Only properties can be annotated with Verify", { it }) {
                it is KSPropertyDeclaration
            }
            .filterIsInstance<KSPropertyDeclaration>()
            .map { it.type.resolve() }
            .distinctBy { it.declaration.qualifiedName!!.asString() }
            .associateBy { it.declaration.qualifiedName!!.asString() }
            .map { (name, ksType) ->
                val declaration =
                    (ksType.declaration as? KSClassDeclaration)?.takeIf { it.classKind == ClassKind.INTERFACE }
                        ?: return run {
                            logger.error("Only interfaces can be verified", ksType.declaration)
                            emptyList()
                        }
                val fakeTypeSpec = generateFakeImplementation(ksType, declaration)

                FileSpec.builder(
                    declaration.normalizedPackageName,
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
                                ClassName(declaration.normalizedPackageName, fake.name!!)
                            addStatement("\"${declaration.normalizedQualifiedName}\" -> clazz.cast(${className.canonicalName}())!!")
                        }
                        addStatement("else -> error(\"Unsupported type \$clazz\")")
                    }
                    .endControlFlow()
                    .build()
            )
            .build()
    }

    private val KSClassDeclaration.normalizedQualifiedName: String
        get() = when (val qualifier = qualifiedName!!.getQualifier()) {
            "kotlin" -> "kotlin.jvm.functions"
            else -> qualifier
        } + ".${qualifiedName!!.getShortName()}"

    private val KSClassDeclaration.normalizedPackageName: String
        get() = packageName.takeIf { it.asString() != "kotlin" }?.asString() ?: "_kotlin"

    private fun generateFakeImplementation(
        ksType: KSType,
        interfaceDeclaration: KSClassDeclaration
    ): TypeSpec {
        val interfaceName = interfaceDeclaration.simpleName.asString()
        val implementationClassName = "${interfaceName}_Fake"

        val implementationTypeSpec = TypeSpec.classBuilder(implementationClassName)
            .addModifiers(KModifier.PUBLIC)
            .addSuperinterface(ksType.toTypeName())
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
        fun KSTypeReference.resolveType(): TypeName? {
            val arguments = ksType.arguments
            val parameters = interfaceDeclaration.typeParameters.map { it.name.getShortName() }
            if (arguments.isEmpty()) {
                return toTypeName()
            }
            return arguments.getOrNull(parameters.indexOf(toString()))?.toTypeName()
        }
        for (function in interfaceDeclaration.declarations.filterIsInstance<KSFunctionDeclaration>()) {
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

            if (returnType != unitTypeName) {
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
                    "\"Expected an invocation, but got none instead\"".noWrap(),
                )
                .endControlFlow()
                .addStatement("_mockingbird_invocations.removeAt(0)")
                .beginControlFlow("check(invocation.functionName == %S)", functionName)
                .addStatement(
                    "\"Expected function call %1L, \${invocation.functionName} was called instead\"".noWrap(),
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
                            "\"Expected argument \$%1L, found \${invocation.parameters[%2L]} instead.\"".noWrap(),
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

            implementationTypeSpec.addFunction(funSpec.build())
        }

        return implementationTypeSpec.build()
    }

    private fun <T> Sequence<T>.check(
        message: String,
        node: (T) -> KSNode,
        condition: (T) -> Boolean
    ): Sequence<T> = filter {
        val passed = condition(it)

        if (!passed) {
            logger.error(message, node(it))
        }

        passed
    }

    private fun String.noWrap(): String = this.replace(" ", "·")

}
