package com.anthonycr.mockingbird.compiler.ir

import com.anthonycr.mockingbird.compiler.utils.debug
import com.anthonycr.mockingbird.compiler.utils.name
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.IrBuilder
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
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.addSimpleDelegatingConstructor
import org.jetbrains.kotlin.ir.util.classIdOrFail
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyParametersFrom
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class MockingbirdClassGenerator(
    val messageCollector: MessageCollector,
    val pluginContext: IrPluginContext,
    val typesToGenerate: Map<FqName, IrType>
) : IrElementTransformerVoid() {

    val classes = mutableListOf<IrClass>()

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment): IrExternalPackageFragment {
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
                if (inheritedIrType !is IrSimpleType) {
                    messageCollector.report(
                        CompilerMessageSeverity.ERROR,
                        "$inheritedFqName is not an IrSimpleType"
                    )
                    return super.visitPackageFragment(declaration)
                }

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
            name = Name.identifier("${inheritedIrType.name()}_Fakes")
            kind = ClassKind.CLASS
        }.apply {
            val irClass = this
            // TODO implement type parameters for parameterized interfaces and classes
            // if (inheritedIrType.classOrFail.owner.typeParameters.isNotEmpty()) {
            //     typeParameters =inheritedIrType.classOrFail.owner.typeParameters
            // }
            val verifiableIrType = pluginContext.referenceClass(verifiableClassId)!!
                .createType(false, emptyList())
            superTypes = listOf(inheritedIrType, verifiableIrType)
            createThisReceiverParameter()

            // Look for super constructor or use default any constructor if we are implementing an interface
            val constructorOfAny = inheritedIrType.classOrFail.constructors.firstOrNull()?.owner
                ?: pluginContext.irBuiltIns.anyClass.owner.constructors.first()
            addSimpleDelegatingConstructor(
                superConstructor = constructorOfAny,
                irBuiltIns = pluginContext.irBuiltIns,
                isPrimary = true
            )

            val invocationsPropertySymbol = pluginContext.referenceProperties(
                verifiableInvocationsPropertyCallableId
            ).first()
            val invocationsIrProperty = addProperty {
                name = verifiableInvocationsPropertyCallableId.callableName
                updateFrom(invocationsPropertySymbol.owner)
            }.apply {
                overriddenSymbols = listOf(invocationsPropertySymbol)
                addBackingField {
                    type = invocationsPropertySymbol.owner.getter!!.returnType
                }.apply {
                    val mutableListFunction =
                        pluginContext.referenceFunctions(mutableListOfCallableId).first()
                    initializer = with(pluginContext.irBuiltIns.createIrBuilder(symbol)) {
                        irExprBody(irCall(mutableListFunction))
                    }
                }
                addDefaultGetter(irClass, pluginContext.irBuiltIns)
                getter!!.overriddenSymbols = listOf(invocationsPropertySymbol.owner.getter!!.symbol)
            }

            val verificationContextPropertySymbol = pluginContext.referenceProperties(
                verifiableVerificationContextCallableId
            ).first()
            val verificationContextIrProperty = addProperty {
                name = verifiableVerificationContextCallableId.callableName
                updateFrom(verificationContextPropertySymbol.owner)
            }.apply {
                overriddenSymbols = listOf(verificationContextPropertySymbol)
                addBackingField {
                    type = verificationContextPropertySymbol.owner.getter!!.returnType
                }.apply {
                    initializer = with(pluginContext.irBuiltIns.createIrBuilder(symbol)) {
                        irExprBody(irNull())
                    }
                }
                addDefaultGetter(irClass, pluginContext.irBuiltIns)
                addDefaultSetter(irClass, pluginContext.irBuiltIns)
                getter!!.overriddenSymbols =
                    listOf(verificationContextPropertySymbol.owner.getter!!.symbol)
                setter!!.overriddenSymbols =
                    listOf(verificationContextPropertySymbol.owner.setter!!.symbol)
            }

            val supportedModalities = listOf(Modality.ABSTRACT, Modality.OPEN)
            inheritedIrType.classOrFail.functions.filter {
                it.owner.modality in supportedModalities
            }.forEach { inheritedFunction ->
                if (inheritedFunction.owner.returnType != pluginContext.irBuiltIns.unitType &&
                    inheritedFunction.owner.modality in supportedModalities
                ) {
                    generateNonVerifiableFunction(inheritedFunction)
                } else {
                    generateVerifiableFunction(
                        inheritedFunction,
                        verificationContextIrProperty,
                        invocationsIrProperty
                    )
                }
            }
        }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.generateNonVerifiableFunction(
        inheritedFunction: IrSimpleFunctionSymbol
    ): IrSimpleFunction = addFunction {
        name = inheritedFunction.owner.name
        returnType = inheritedFunction.owner.returnType
        updateFrom(inheritedFunction.owner)
    }.apply {
        modality = Modality.FINAL
        copyParametersFrom(inheritedFunction.owner)
        overriddenSymbols = listOf(inheritedFunction)
        body = pluginContext.irBuiltIns.createIrBuilder(symbol).irBlockBody {
            val checkFunction = pluginContext.referenceFunctions(errorCallableId)
                .first {
                    it.owner.parameters.size == 1 &&
                            it.owner.parameters[0].type == pluginContext.irBuiltIns.anyType
                }
            +irReturn(
                irCall(checkFunction).apply {
                    arguments[0] = irString("Only functions with return type Unit can be verified")
                }
            )
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.generateVerifiableFunction(
        inheritedFunction: IrSimpleFunctionSymbol,
        verificationContextIrProperty: IrProperty,
        invocationsIrProperty: IrProperty
    ): IrSimpleFunction {
        val irClass = this
        return addFunction {
            name = inheritedFunction.owner.name
            returnType = pluginContext.irBuiltIns.unitType
            updateFrom(inheritedFunction.owner)
        }.apply {
            val function = this
            modality = Modality.FINAL
            overriddenSymbols = listOf(inheritedFunction)
            copyParametersFrom(inheritedFunction.owner)
            parameters[0].apply {
                origin = irClass.origin
                type = irClass.thisReceiver!!.type
            }
            body = pluginContext.irBuiltIns.createIrBuilder(symbol).irBlockBody {
                val verificationContextVariable = irTemporary(
                    nameHint = "verificationContext",
                    irType = verificationContextIrProperty.getter!!.returnType,
                    value = irGet(dispatchReceiverParameter!!)
                        .irCallFunction(verificationContextIrProperty.getter!!)
                )

                +irIfThenElse(
                    type = pluginContext.irBuiltIns.unitType,
                    condition = irNotEquals(irGet(verificationContextVariable), irNull()),
                    thenPart = irBlock {
                        val firstOrNullFunction = pluginContext.referenceFunctions(
                            firstOrNullCallableId
                        )
                            .first { it.owner.parameters.firstOrNull()!!.type.classOrFail.owner.classIdOrFail == pluginContext.irBuiltIns.listClass.owner.classIdOrFail }

                        val invocation = irTemporary(
                            nameHint = "invocation",
                            value = irGet(dispatchReceiverParameter!!)
                                .irCallFunction(invocationsIrProperty.getter!!)
                                .irCallFunction(firstOrNullFunction)
                        )
                        val checkFunction = pluginContext.referenceFunctions(checkCallableId)
                            .first {
                                it.owner.parameters.size == 2 &&
                                        it.owner.parameters[0].type == pluginContext.irBuiltIns.booleanType &&
                                        it.owner.parameters[1].type.classOrFail.owner == pluginContext.irBuiltIns.functionN(
                                    0
                                )
                            }

                        +irCall(checkFunction).apply {
                            arguments[0] = irNotEquals(irGet(invocation), irNull())
                            arguments[1] =
                                irLambdaAnyReturn(irString("Expected an invocation, but got none instead"))
                        }

                        val removeAt = invocationsIrProperty.backingField!!.type.classOrFail
                            .functions.first { it.owner.name == Name.identifier("removeAt") }

                        +irGet(dispatchReceiverParameter!!)
                            .irCallFunction(invocationsIrProperty.getter!!)
                            .irCallFunction(removeAt, irInt(0))

                        +irCall(checkFunction).apply {
                            val functionName = irTemporary(
                                nameHint = "functionName",
                                value = irCall(
                                    pluginContext.referenceProperties(
                                        invocationFunctionNamePropertyCallableId
                                    ).first().owner.getter!!
                                ).apply {
                                    arguments[0] = irGet(invocation)
                                }
                            )
                            arguments[0] = irEquals(
                                irGet(functionName),
                                irString(inheritedFunction.owner.fqNameWhenAvailable!!.asString())
                            )
                            arguments[1] = irLambdaAnyReturn(
                                irConcat().apply {
                                    arguments.add(irString("Expected function call "))
                                    arguments.add(irString(inheritedFunction.owner.fqNameWhenAvailable!!.asString()))
                                    arguments.add(irString(", "))
                                    arguments.add(irGet(functionName))
                                    arguments.add(irString(" was called instead"))
                                }
                            )
                        }

                        val sizeProperty =
                            pluginContext.irBuiltIns.listClass.getPropertyGetter("size")

                        val getVerificationContextParameterMatcher =
                            irGet(verificationContextVariable)
                                .irCallFunction(
                                    pluginContext.referenceProperties(
                                        verificationContextParameterMatcherCallableId
                                    ).first().owner.getter!!
                                )

                        val invocationParameters = irGet(invocation)
                            .irCallFunction(
                                pluginContext.referenceProperties(
                                    invocationParametersPropertyCallableId
                                ).first().owner.getter!!
                            )

                        val isNotEmpty = pluginContext.referenceFunctions(isNotEmptyCallableId)
                            .first {
                                it.owner.parameters.size == 1 && it.owner.parameters[0].type.classOrFail.owner.classIdOrFail == pluginContext.irBuiltIns.collectionClass.owner.classIdOrFail
                            }
                        +irIfThen(
                            condition = getVerificationContextParameterMatcher
                                .irCallFunction(isNotEmpty),
                            thenPart = irCall(checkFunction).apply {
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
                                arguments[1] = irLambdaAnyReturn(irConcat().apply {
                                    arguments.add(irString("Expected "))
                                    arguments.add(irGet(invocationParametersSize))
                                    arguments.add(irString(" matchers, found "))
                                    arguments.add(irGet(parameterMatchersSize))
                                    arguments.add(irString(" instead. When using custom parameter verification, all parameters must use matchers."))
                                })
                            }
                        )

                        val verificationContextCompanion = pluginContext
                            .referenceClass(verificationContextClassId)!!.owner
                            .companionObject()!!
                        val defaultMatcher = verificationContextCompanion
                            .getPropertyGetter("DEFAULT_MATCHER")
                        val function2Invoke = pluginContext.irBuiltIns.functionN(2)
                            .getSimpleFunction("invoke")!!
                        val getOrNull = pluginContext.referenceFunctions(getOrNullCallableId)
                            .first {
                                it.owner.parameters.first().type.classOrFail.owner.classIdOrFail == pluginContext.irBuiltIns.listClass.owner.classIdOrFail
                            }

                        // Drop the receiver parameter
                        function.symbol.owner.parameters.drop(1)
                            .forEachIndexed { index, parameter ->
                                val matcherOrNull = irTemporary(
                                    nameHint = "nullableMatcher",
                                    value = getVerificationContextParameterMatcher
                                        .irCallFunction(getOrNull, irInt(index))
                                )
                                val nonNullMatcher = irIfNull(
                                    type = matcherOrNull.type,
                                    subject = irGet(matcherOrNull),
                                    thenPart = irGetObject(verificationContextCompanion.symbol)
                                        .irCallFunction(defaultMatcher!!),
                                    elsePart = irGet(matcherOrNull)
                                )
                                +irCall(checkFunction).apply {
                                    val listGet =
                                        pluginContext.irBuiltIns.listClass.getSimpleFunction(
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
                                        irLambdaAnyReturn(irConcat().apply {
                                            arguments.add(irString("Expected argument "))
                                            arguments.add(irGet(parameter))
                                            arguments.add(irString(", found "))
                                            arguments.add(irGet(invokedParameter))
                                            arguments.add(irString(" instead."))
                                        })
                                }
                            }

                        +irCall(
                            pluginContext.referenceProperties(
                                verificationContextParameterMatcherCallableId
                            ).first().owner.setter!!
                        ).apply {
                            arguments[0] = irGet(verificationContextVariable)
                            arguments[1] = irCall(
                                pluginContext.referenceFunctions(emptyListOfCallableId)
                                    .first {
                                        it.owner.returnType.classOrFail.owner.classIdOrFail == pluginContext.irBuiltIns.listClass.owner.classIdOrFail
                                    }
                            )
                        }
                    },
                    elsePart = irBlock {
                        val listAdd = pluginContext.irBuiltIns.mutableListClass.functions.first {
                            it.owner.name == Name.identifier("add") && it.owner.parameters.size == 2
                        }
                        val invocationClass = pluginContext
                            .referenceClass(invocationClassId)!!.owner
                        val invocationClassConstructor = invocationClass.primaryConstructor!!.symbol
                        +irGet(dispatchReceiverParameter!!)
                            .irCallFunction(invocationsIrProperty.getter!!)
                            .irCallFunction(
                                listAdd,
                                irCallConstructor(
                                    invocationClassConstructor,
                                    emptyList()
                                ).apply {
                                    arguments[0] =
                                        irString(inheritedFunction.owner.fqNameWhenAvailable!!.asString())

                                    val listOf = pluginContext.referenceFunctions(listOfCallableId)
                                        .first {
                                            it.owner.parameters.size == 1 &&
                                                    it.owner.parameters[0].isVararg &&
                                                    it.owner.returnType.classOrFail.owner.classIdOrFail == pluginContext.irBuiltIns.listClass.owner.classIdOrFail
                                        }
                                    arguments[1] = irCall(listOf).apply {
                                        arguments[0] = irVararg(
                                            elementType = pluginContext.irBuiltIns.anyType,
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
        }
    }

    private fun IrFunction.irLambdaAnyReturn(value: IrExpression): IrFunctionExpressionImpl {
        val lambda = pluginContext.irBuiltIns.functionN(0)
        return IrFunctionExpressionImpl(
            0,
            0,
            lambda.typeWith(listOf(pluginContext.irBuiltIns.anyType)),
            pluginContext.irFactory.buildFun {
                name = Name.special("<anonymous>")
                visibility = DescriptorVisibilities.LOCAL
                returnType = pluginContext.irBuiltIns.anyType
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            }.apply {
                parent = this@irLambdaAnyReturn
                modality = Modality.FINAL
                body = pluginContext.irBuiltIns.createIrBuilder(symbol).irBlockBody {
                    +irReturn(value)
                }
            },
            IrStatementOrigin.LAMBDA
        )
    }

    context(builder: IrBuilder)
    fun IrExpression.irCallFunction(
        irFunction: IrFunction,
        vararg parameters: IrExpression
    ): IrFunctionAccessExpression = builder.irCall(irFunction).apply {
        arguments[0] = this@irCallFunction
        parameters.forEachIndexed { index, parameter ->
            arguments[index + 1] = parameter
        }
    }

    context(builder: IrBuilder)
    fun IrExpression.irCallFunction(
        irFunctionSymbol: IrFunctionSymbol,
        vararg parameters: IrExpression
    ): IrFunctionAccessExpression = builder.irCall(irFunctionSymbol).apply {
        arguments[0] = this@irCallFunction
        parameters.forEachIndexed { index, parameter ->
            arguments[index + 1] = parameter
        }
    }

    companion object {
        val verifiableClassId =
            ClassId.topLevel(FqName("com.anthonycr.mockingbird.core.Verifiable"))
        val verifiableInvocationsPropertyCallableId = CallableId(
            verifiableClassId,
            Name.identifier("_mockingbird_invocations")
        )
        val verifiableVerificationContextCallableId = CallableId(
            verifiableClassId,
            Name.identifier("_mockingbird_verificationContext")
        )

        val invocationClassId = verifiableClassId.createNestedClassId(Name.identifier("Invocation"))
        val invocationFunctionNamePropertyCallableId = CallableId(
            invocationClassId,
            Name.identifier("functionName")
        )
        val invocationParametersPropertyCallableId = CallableId(
            invocationClassId,
            Name.identifier("parameters")
        )

        val verificationContextClassId =
            ClassId.topLevel(FqName("com.anthonycr.mockingbird.core.VerificationContext"))
        val verificationContextParameterMatcherCallableId = CallableId(
            verificationContextClassId,
            Name.identifier("parameterMatcher")
        )

        val mutableListOfCallableId = CallableId(
            FqName("kotlin.collections"),
            Name.identifier("mutableListOf")
        )

        val emptyListOfCallableId = CallableId(
            FqName("kotlin.collections"),
            Name.identifier("emptyList")
        )

        val listOfCallableId = CallableId(
            FqName("kotlin.collections"),
            Name.identifier("listOf")
        )

        val firstOrNullCallableId = CallableId(
            FqName("kotlin.collections"),
            Name.identifier("firstOrNull")
        )

        val isNotEmptyCallableId = CallableId(
            FqName("kotlin.collections"),
            Name.identifier("isNotEmpty")
        )

        val getOrNullCallableId = CallableId(
            FqName("kotlin.collections"),
            Name.identifier("getOrNull")
        )

        val errorCallableId = CallableId(
            FqName("kotlin"),
            Name.identifier("error")
        )

        val checkCallableId = CallableId(
            FqName("kotlin"),
            Name.identifier("check")
        )
    }
}
