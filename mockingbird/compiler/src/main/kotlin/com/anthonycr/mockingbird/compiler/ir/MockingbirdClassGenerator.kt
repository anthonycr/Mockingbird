package com.anthonycr.mockingbird.compiler.ir

import com.anthonycr.mockingbird.compiler.utils.debug
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addBackingField
import org.jetbrains.kotlin.ir.builders.declarations.addDefaultGetter
import org.jetbrains.kotlin.ir.builders.declarations.addDefaultSetter
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irReturnUnit
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.addSimpleDelegatingConstructor
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyParametersFrom
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class MockingbirdClassGenerator(
    val messageCollector: MessageCollector,
    val pluginContext: IrPluginContext,
    val typesToGenerate: Map<FqName, IrType>
) : IrElementTransformerVoid() {

    private val irBuiltIns = pluginContext.irBuiltIns
    private val verifiable = Verifiable(pluginContext)
    private val invocation = Verifiable.Invocation(pluginContext)
    private val kotlin = KotlinFunctions(pluginContext)

    val classes = mutableListOf<IrClass>()

    override fun visitExternalPackageFragment(
        declaration: IrExternalPackageFragment
    ): IrExternalPackageFragment {
        messageCollector.debug("Visiting ${declaration.packageFqName.asString()}")
        return super.visitExternalPackageFragment(declaration)
    }

    override fun visitPackageFragment(declaration: IrPackageFragment): IrPackageFragment {
        messageCollector.debug("Visiting ${declaration.packageFqName.asString()}")
        val normalizedPackageName = if (declaration.packageFqName.asString().startsWith("_")) {
            declaration.packageFqName.asString().substring(1)
        } else {
            declaration.packageFqName.asString()
        }
        typesToGenerate.filter { it.key.asString().contains(normalizedPackageName) }
            .forEach { (inheritedFqName, inheritedIrType) ->
                messageCollector.debug("Generating fake for $inheritedFqName")

                val irClass = generateFake(inheritedIrType)

                declaration.addChild(irClass)
                classes.add(irClass)

                messageCollector.debug(irClass.dumpKotlinLike())
            }
        return super.visitPackageFragment(declaration)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun generateFake(inheritedIrType: IrType): IrClass =
        pluginContext.irFactory.buildClass {
            name = Name.identifier("${inheritedIrType.classOrFail.owner.name}_Fakes")
            kind = ClassKind.CLASS
        }.apply {
            val irClass = this
            // TODO implement type parameters for parameterized interfaces and classes
            // if (inheritedIrType.classOrFail.owner.typeParameters.isNotEmpty()) {
            //     typeParameters =inheritedIrType.classOrFail.owner.typeParameters
            // }
            val verifiableIrType = verifiable.symbol.createType(false, emptyList())
            superTypes = listOf(inheritedIrType, verifiableIrType)
            createThisReceiverParameter()

            // Look for super constructor or use default any constructor if we are implementing an interface
            val constructorOfAny = inheritedIrType.classOrFail.constructors.firstOrNull()?.owner
                ?: irBuiltIns.anyClass.owner.constructors.first()
            addSimpleDelegatingConstructor(
                superConstructor = constructorOfAny,
                irBuiltIns = irBuiltIns,
                isPrimary = true
            )

            val invocationsIrProperty = addProperty {
                name = verifiable.invocations.callableId.callableName
                updateFrom(verifiable.invocations.symbol.owner)
            }.apply {
                overriddenSymbols = listOf(verifiable.invocations.symbol)
                addBackingField {
                    type = verifiable.invocations.getter.returnType
                }.apply {
                    initializer = with(irBuiltIns.createIrBuilder(symbol)) {
                        irExprBody(irCall(kotlin.mutableListOf.symbol))
                    }
                }
                addDefaultGetter(irClass, irBuiltIns)
                getter!!.overriddenSymbols = listOf(verifiable.invocations.getter.symbol)
            }

            val verificationContextIrProperty = addProperty {
                name = verifiable.verificationContext.callableId.callableName
                updateFrom(verifiable.verificationContext.symbol.owner)
            }.apply {
                overriddenSymbols = listOf(verifiable.verificationContext.symbol)
                addBackingField {
                    type = verifiable.verificationContext.getter.returnType
                }.apply {
                    initializer = with(irBuiltIns.createIrBuilder(symbol)) {
                        irExprBody(irNull())
                    }
                }
                addDefaultGetter(irClass, irBuiltIns)
                addDefaultSetter(irClass, irBuiltIns)
                getter!!.overriddenSymbols = listOf(verifiable.verificationContext.getter.symbol)
                setter!!.overriddenSymbols = listOf(verifiable.verificationContext.setter.symbol)
            }

            val supportedModalities = listOf(Modality.ABSTRACT, Modality.OPEN)
            inheritedIrType.classOrFail.functions.filter {
                it.owner.modality in supportedModalities
            }.forEach { inheritedFunction ->
                if (inheritedFunction.owner.returnType != irBuiltIns.unitType &&
                    inheritedFunction.owner.modality in supportedModalities
                ) {
                    addFunction {
                        name = inheritedFunction.owner.name
                        returnType = inheritedFunction.owner.returnType
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

        val verificationContext = VerificationContext(pluginContext)
        val mutableList = MutableListFunctions(pluginContext)

        +irIfThenElse(
            type = irBuiltIns.unitType,
            condition = irNotEquals(irGet(verificationContextVariable), irNull()),
            thenPart = irBlock {
                val invocationVariable = irTemporary(
                    nameHint = "invocation",
                    value = irGet(function.dispatchReceiverParameter!!)
                        .irCallFunction(invocationsIrProperty.getter!!)
                        .irCallFunction(kotlin.firstOrNull.symbol)
                )

                +irCall(kotlin.check.symbol).apply {
                    arguments[0] = irNotEquals(irGet(invocationVariable), irNull())
                    arguments[1] = function.irLambdaAnyReturn(
                        irString("Expected an invocation, but got none instead")
                    )
                }

                +irGet(function.dispatchReceiverParameter!!)
                    .irCallFunction(invocationsIrProperty.getter!!)
                    .irCallFunction(mutableList.removeAt.symbol, irInt(0))

                +irCall(kotlin.check.symbol).apply {
                    val functionName = irTemporary(
                        nameHint = "functionName",
                        value = irGet(invocationVariable).irCallFunction(invocation.functionName.getter)
                    )
                    arguments[0] = irEquals(
                        irGet(functionName),
                        irString(inheritedFunction.owner.fqNameWhenAvailable!!.asString())
                    )
                    arguments[1] = function.irLambdaAnyReturn(
                        irConcat().apply {
                            arguments.add(irString("Expected function call "))
                            arguments.add(irString(inheritedFunction.owner.fqNameWhenAvailable!!.asString()))
                            arguments.add(irString(", "))
                            arguments.add(irGet(functionName))
                            arguments.add(irString(" was called instead"))
                        }
                    )
                }

                val sizeProperty = irBuiltIns.listClass.getPropertyGetter("size")

                val getVerificationContextParameterMatcher = irGet(verificationContextVariable)
                    .irCallFunction(verificationContext.parameterMatcher.getter)

                val invocationParameters = irGet(invocationVariable)
                    .irCallFunction(invocation.parameters.getter)

                +irIfThen(
                    condition = getVerificationContextParameterMatcher
                        .irCallFunction(kotlin.isNotEmpty.symbol),
                    thenPart = irCall(kotlin.check.symbol).apply {
                        val invocationParametersSize = irTemporary(
                            nameHint = "invocationParametersSize",
                            value = invocationParameters.irCallFunction(sizeProperty!!)
                        )
                        val parameterMatchersSize = irTemporary(
                            nameHint = "parameterMatchersSize",
                            value = getVerificationContextParameterMatcher
                                .irCallFunction(sizeProperty)
                        )
                        arguments[0] = irEquals(
                            irGet(invocationParametersSize),
                            irGet(parameterMatchersSize)
                        )
                        arguments[1] = function.irLambdaAnyReturn(irConcat().apply {
                            arguments.add(irString("Expected "))
                            arguments.add(irGet(invocationParametersSize))
                            arguments.add(irString(" matchers, found "))
                            arguments.add(irGet(parameterMatchersSize))
                            arguments.add(irString(" instead. When using custom parameter verification, all parameters must use matchers."))
                        })
                    }
                )

                val verificationContextCompanion = verificationContext.symbol.owner
                    .companionObject()!!
                val defaultMatcher = verificationContextCompanion
                    .getPropertyGetter("DEFAULT_MATCHER")
                val function2Invoke = irBuiltIns.functionN(2)
                    .getSimpleFunction("invoke")!!

                // Drop the receiver parameter
                function.symbol.owner.parameters.drop(1)
                    .forEachIndexed { index, parameter ->
                        val matcherOrNull = irTemporary(
                            nameHint = "nullableMatcher",
                            value = getVerificationContextParameterMatcher
                                .irCallFunction(kotlin.getOrNull.symbol, irInt(index))
                        )
                        val nonNullMatcher = irIfNull(
                            type = matcherOrNull.type,
                            subject = irGet(matcherOrNull),
                            thenPart = irGetObject(verificationContextCompanion.symbol)
                                .irCallFunction(defaultMatcher!!),
                            elsePart = irGet(matcherOrNull)
                        )
                        +irCall(kotlin.check.symbol).apply {
                            val listGet = irBuiltIns.listClass.getSimpleFunction(
                                "get"
                            )!!
                            val invokedParameter = irTemporary(
                                nameHint = "invokedParameter",
                                value = irCall(listGet).apply {
                                    arguments[0] = invocationParameters
                                    arguments[1] = irInt(index)
                                }
                            )
                            arguments[0] = nonNullMatcher.irCallFunction(
                                function2Invoke,
                                irGet(parameter),
                                irGet(invokedParameter)
                            )
                            arguments[1] =
                                function.irLambdaAnyReturn(irConcat().apply {
                                    arguments.add(irString("Expected argument "))
                                    arguments.add(irGet(parameter))
                                    arguments.add(irString(", found "))
                                    arguments.add(irGet(invokedParameter))
                                    arguments.add(irString(" instead."))
                                })
                        }
                    }

                +irCall(verificationContext.parameterMatcher.setter).apply {
                    arguments[0] = irGet(verificationContextVariable)
                    arguments[1] = irCall(kotlin.emptyList.symbol)
                }
            },
            elsePart = irBlock {
                val invocationClass = invocation.symbol.owner
                val invocationClassConstructor = invocationClass.primaryConstructor!!.symbol
                +irGet(function.dispatchReceiverParameter!!)
                    .irCallFunction(invocationsIrProperty.getter!!)
                    .irCallFunction(
                        mutableList.listAdd.symbol,
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
            pluginContext.irFactory.buildFun {
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
