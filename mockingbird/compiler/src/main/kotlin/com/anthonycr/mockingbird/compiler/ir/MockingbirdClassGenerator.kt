package com.anthonycr.mockingbird.compiler.ir

import com.anthonycr.mockingbird.compiler.utils.debug
import com.anthonycr.mockingbird.compiler.utils.debugDump
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.getCompilerMessageLocation
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irEqualsNull
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irReturnUnit
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.types.typeWithArguments
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.addSimpleDelegatingConstructor
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyParametersFrom
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class MockingbirdClassGenerator(
    private val messageCollector: MessageCollector,
    pluginContext: IrPluginContext,
    private val typesToGenerate: Map<FqName, IrType>
) : IrElementTransformerVoid() {

    private val irBuiltIns = pluginContext.irBuiltIns
    private val irFactory = pluginContext.irFactory
    private val verifiable = Verifiable(pluginContext)
    private val invocation = Verifiable.Invocation(pluginContext)
    private val kotlin = KotlinFunctions(pluginContext)
    private val mutableList = MutableListFunctions(pluginContext)
    private val matcher = Verifiable.Matcher(pluginContext)
    private val equalsMatcher = Verifiable.Matcher.Equals(pluginContext)
    private val sameAsMatcher = Verifiable.Matcher.SameAs(pluginContext)
    private val anythingMatcher = Verifiable.Matcher.Anything(pluginContext)
    private val verificationContext = VerificationContext()
    private val anyFqName = verificationContext.any
    private val sameAsFqName = verificationContext.sameAs
    private val verification = Verification()
    private val verifyFqName = verification.verifyFqName
    private val verifyPartialFqName = verification.verifyPartialFqName

    val classes = mutableListOf<IrClass>()

    private var currentFile: IrFile? = null

    override fun visitFile(declaration: IrFile): IrFile {
        // com.example.Interface_Fake.kt -> com.example.Interface
        val fakeFqName =
            declaration.packageFqName.child(Name.identifier(declaration.name.substringBeforeLast("_")))
        messageCollector.debug("Visiting file: ${declaration.packageFqName.asString()}.${declaration.name}")
        typesToGenerate[fakeFqName]?.let { inheritedIrType ->
            messageCollector.debug("Generating fake for $fakeFqName")

            val irClass = generateFake(inheritedIrType)

            declaration.addChild(irClass)
            classes.add(irClass)

            messageCollector.debugDump(irClass)
        }
        currentFile = declaration
        return super.visitFile(declaration)
    }

    inner class VerificationCallTransformer(
        private val messageCollector: MessageCollector,
        private val irBuiltIns: IrBuiltIns,
        private val properties: List<IrProperty>,
    ) : IrElementTransformerVoid() {
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        override fun visitCall(expression: IrCall): IrExpression {
            val statement = expression
            val statementFirstArgument = statement.arguments.first()

            messageCollector.debug("Visiting call: ${statement.symbol.owner.name}")
            messageCollector.debugDump(statement)
            return if (statementFirstArgument is IrCall) {
                val functionReceiverTarget =
                    statementFirstArgument.symbol.owner.correspondingPropertySymbol?.owner?.backingField?.type?.classFqName
                val functionOwner = statement.symbol.owner.parent.kotlinFqName
                // Only rewrite actual function calls, not extension functions
                val isMemberFunction = functionReceiverTarget == functionOwner
                if (properties.contains(statementFirstArgument.symbol.owner.correspondingPropertySymbol?.owner) &&
                    isMemberFunction
                ) {
                    irBuiltIns.createIrBuilder(expression.symbol).irBlock {
                        +statementFirstArgument.irCallFunction(
                            verifiable.verifyCall,
                            irString(statement.symbol.owner.fqNameWhenAvailable!!.asString()),
                            irCall(kotlin.listOf.symbol).apply {
                                arguments[0] = irVararg(
                                    elementType = matcher.symbol.defaultType,
                                    // Drop receiver
                                    values = (statement.arguments.drop(1)).map {
                                        when (it) {
                                            is IrCall -> when (it.symbol.owner.fqNameWhenAvailable) {
                                                anyFqName -> irGetObject(anythingMatcher.symbol)

                                                sameAsFqName -> irCall(
                                                    sameAsMatcher.symbol.owner.primaryConstructor!!
                                                ).apply {
                                                    arguments[0] = it.arguments[1]
                                                }

                                                else -> irCall(equalsMatcher.symbol.owner.primaryConstructor!!).apply {
                                                    arguments[0] = it
                                                }
                                            }

                                            null -> {
                                                // Default parameters can't be accessed from within the verify function.
                                                // Different approach would be needed.
                                                messageCollector.report(
                                                    severity = CompilerMessageSeverity.ERROR,
                                                    message = "Mockingbird can't access default parameters, please specify expected arguments explicitly",
                                                    location = statement.getCompilerMessageLocation(
                                                        currentFile!!
                                                    )
                                                )
                                                irNull()
                                            }

                                            else -> irCall(equalsMatcher.symbol.owner.primaryConstructor!!).apply {
                                                arguments[0] = it
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }.also {
                        messageCollector.debug("Transformed call:")
                        messageCollector.debugDump(it)
                    }
                } else {
                    statement.transformChildren(
                        VerificationCallTransformer(
                            messageCollector,
                            irBuiltIns,
                            properties
                        ), null
                    )
                    statement
                }
            } else {
                // May be a function call like repeat(n) with the body containing verification calls
                statement.transformChildren(
                    VerificationCallTransformer(
                        messageCollector,
                        irBuiltIns,
                        properties
                    ), null
                )
                statement
            }
        }
    }

    /**
     * Rewrites all `verify` and `verifyPartial` blocks such that calls to verifiables are replaced
     * with `_verifyCall` invocations.
     *
     * Given `verify` block:
     * ```
     * verify(fakeInterface) {
     *     fakeInterface.function(1)
     * }
     * ```
     * It rewrites the block to
     * ```
     * verify(fakeInterface) {
     *     fakeInterface._verifyCall("com.example.Interface.function", listOf(Equals(1)))
     * }
     * ```
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.symbol.owner.kotlinFqName == verifyFqName ||
            expression.symbol.owner.kotlinFqName == verifyPartialFqName
        ) {
            val verifiables = expression.arguments.first() as IrVararg

            messageCollector.debug("Available verifiable instances:")
            verifiables.elements.forEach {
                messageCollector.debugDump(it)
            }

            val properties = verifiables.elements
                .map { it as IrCall }
                .mapNotNull { it.symbol.owner.correspondingPropertySymbol?.owner }

            expression.transformChildren(
                VerificationCallTransformer(
                    messageCollector,
                    irBuiltIns,
                    properties
                ), null
            )
        }
        return super.visitCall(expression)
    }

    /**
     * Generates fake implementation for the provided [inheritedIrType].
     *
     * Given interface definition:
     * ```
     * package com.example
     *
     * interface Interface {
     *     fun function(parameter0: Int)
     * }
     * ```
     * It generates:
     * ```
     * package com.example
     *
     * class Interface_Fake : Interface {
     *     override val _mockingbird_invocations: MutableList<Invocation> = mutableListOf()
     *
     *     override var _mockingbird_verificationContext: VerificationContext? = null
     *
     *     override fun function(parameter0: Int) {
     *         check(_mockingbird_verificationContext == null) {
     *             "Function called inside verification block and not handled by mockingbird"
     *         }
     *         _mockingbird_invocations.add(Invocation("com.example.Interface.function", listOf(parameter0)))
     *     }
     * }
     * ```
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun generateFake(inheritedIrType: IrType): IrClass =
        irFactory.buildClass {
            name = Name.identifier("${inheritedIrType.classOrFail.owner.name}_Fake")
            kind = ClassKind.CLASS
        }.apply {
            val irClass = this
            val verifiableIrType = verifiable.symbol.createType(false, emptyList())
            val resolvedType = inheritedIrType.classOrFail.typeWithArguments(
                inheritedIrType.classOrFail.owner.typeParameters.map { it.superTypes.first() }
            )
            superTypes = listOf(resolvedType, verifiableIrType)
            createThisReceiverParameter()

            // Look for super constructor or use default any constructor if we are implementing an interface
            val constructorOfAny = inheritedIrType.classOrFail.constructors.firstOrNull()?.owner
                ?: irBuiltIns.anyClass.owner.constructors.first()
            addSimpleDelegatingConstructor(
                superConstructor = constructorOfAny,
                irBuiltIns = irBuiltIns,
                isPrimary = true
            )

            val invocationsIrProperty = addPropertyFromDeclaration(
                irBuiltIns = irBuiltIns,
                propertyDeclaration = verifiable.invocations,
                mutable = false
            ) {
                irCall(kotlin.mutableListOf.symbol)
            }

            val verificationContextIrProperty = addPropertyFromDeclaration(
                irBuiltIns = irBuiltIns,
                propertyDeclaration = verifiable.verificationContext,
                mutable = true
            ) {
                irNull()
            }

            val supportedModalities = listOf(Modality.ABSTRACT, Modality.OPEN)
            inheritedIrType.classOrFail.functions.filter {
                it.owner.modality in supportedModalities
            }.forEach { inheritedFunction ->
                val resolvedReturnType = if (inheritedFunction.owner.returnType.isTypeParameter()) {
                    inheritedFunction.owner.returnType.superTypes().first()
                } else {
                    inheritedFunction.owner.returnType
                }
                if (resolvedReturnType != irBuiltIns.unitType &&
                    resolvedReturnType != irBuiltIns.anyType &&
                    resolvedReturnType != irBuiltIns.anyNType &&
                    inheritedFunction.owner.modality in supportedModalities
                ) {
                    addFunction {
                        name = inheritedFunction.owner.name
                        returnType = resolvedReturnType
                        updateFrom(inheritedFunction.owner)
                    }.apply {
                        modality = Modality.FINAL
                        copyParametersFrom(inheritedFunction.owner)
                        overriddenSymbols = listOf(inheritedFunction)
                        body = generateNonVerifiableFunctionBody(this)
                    }
                } else {
                    addFunction {
                        name = inheritedFunction.owner.name
                        returnType = irBuiltIns.unitType
                        updateFrom(inheritedFunction.owner)
                    }.apply {
                        modality = Modality.FINAL
                        overriddenSymbols = listOf(inheritedFunction)
                        copyParametersFrom(inheritedFunction.owner)
                        parameters = parameters.map {
                            if (it.type.isTypeParameter()) {
                                // Erase type parameter and just use first supertype
                                it.type = it.type.superTypes().first()
                            }
                            it
                        }
                        parameters[0].apply {
                            origin = irClass.origin
                            type = irClass.thisReceiver!!.type
                        }
                        body = generateVerifiableFunction(
                            function = this,
                            inheritedFunction = inheritedFunction,
                            verificationContextIrProperty = verificationContextIrProperty,
                            invocationsIrProperty = invocationsIrProperty
                        )
                    }
                }
            }
        }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun generateNonVerifiableFunctionBody(
        function: IrFunction
    ): IrBody = irBuiltIns.createIrBuilder(function.symbol).irBlockBody {
        +irReturn(
            irCall(kotlin.error.symbol).apply {
                arguments[0] = irString("Only functions with return type Unit can be verified")
            }
        )
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun generateVerifiableFunction(
        function: IrFunction,
        inheritedFunction: IrSimpleFunctionSymbol,
        verificationContextIrProperty: IrProperty,
        invocationsIrProperty: IrProperty
    ): IrBody = irBuiltIns.createIrBuilder(function.symbol).irBlockBody {
        val verificationContextVariable = irTemporary(
            nameHint = "verificationContext",
            value = irGet(function.dispatchReceiverParameter!!)
                .irCallFunction(verificationContextIrProperty.getter!!)
        )

        +irCall(kotlin.check.symbol).apply {
            arguments[0] = irEqualsNull(irGet(verificationContextVariable))
            arguments[1] = function.irLambdaAnyReturn(irConcat().apply {
                arguments.add(irString("Function called inside verification block and not handled by mockingbird"))
            })
        }


        val invocationClass = invocation.symbol.owner
        val invocationClassConstructor = invocationClass.primaryConstructor!!.symbol
        +irGet(function.dispatchReceiverParameter!!)
            .irCallFunction(invocationsIrProperty.getter!!)
            .irCallFunction(
                mutableList.listAdd,
                irCallConstructor(
                    invocationClassConstructor,
                    emptyList()
                ).apply {
                    arguments[0] =
                        irString(inheritedFunction.owner.fqNameWhenAvailable!!.asString())

                    arguments[1] = irCall(kotlin.listOf.symbol).apply {
                        arguments[0] = irVararg(
                            elementType = irBuiltIns.anyType,
                            // Drop the receiver parameter
                            values = function.symbol.owner.parameters.drop(1)
                                .map { irGet(it) }
                        )
                    }
                }
            )
        +irReturnUnit()
    }

    private fun IrFunction.irLambdaAnyReturn(value: IrExpression): IrFunctionExpressionImpl {
        val lambda = irBuiltIns.functionN(0)
        return IrFunctionExpressionImpl(
            0,
            0,
            lambda.typeWith(listOf(irBuiltIns.anyType)),
            irFactory.buildFun {
                name = Name.special("<anonymous>")
                visibility = DescriptorVisibilities.LOCAL
                returnType = irBuiltIns.anyType
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            }.apply {
                parent = this@irLambdaAnyReturn
                modality = Modality.FINAL
                body = irBuiltIns.createIrBuilder(symbol).irBlockBody {
                    +irReturn(value)
                }
            },
            IrStatementOrigin.LAMBDA
        )
    }
}
