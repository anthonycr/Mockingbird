package com.anthonycr.mockingbird.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.util.classIdOrFail
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@OptIn(UnsafeDuringIrConstructionAPI::class)
class PropertyDeclaration(
    val pluginContext: IrPluginContext,
    val callableId: CallableId,
) {
    val symbol by lazy {
        pluginContext.referenceProperties(callableId).first()
    }
    val getter by lazy {
        symbol.owner.getter!!
    }
    val setter by lazy {
        symbol.owner.setter!!
    }
}

class FunctionDeclaration(
    private val symbolProvider: () -> IrSimpleFunctionSymbol
) {
    constructor(
        callableId: CallableId,
        pluginContext: IrPluginContext,
        selector: Collection<IrSimpleFunctionSymbol>.() -> IrSimpleFunctionSymbol
    ) : this(
        { pluginContext.referenceFunctions(callableId).selector() }
    )

    val symbol by lazy {
        symbolProvider()
    }
}

class Verifiable(private val pluginContext: IrPluginContext) {
    val classId = ClassId.Companion.topLevel(FqName("com.anthonycr.mockingbird.core.internal.Verifiable"))

    val symbol by lazy {
        pluginContext.referenceClass(classId)!!
    }

    val invocations by lazy {
        PropertyDeclaration(
            pluginContext,
            CallableId(classId, Name.identifier("_mockingbird_invocations")),
        )
    }

    val verificationContext by lazy {
        PropertyDeclaration(
            pluginContext,
            CallableId(
                classId,
                Name.identifier("_mockingbird_verificationContext")
            )
        )
    }

    val verifyCall by lazy {
        FunctionDeclaration(
            callableId = CallableId(
                FqName("com.anthonycr.mockingbird.core.internal"),
                Name.identifier("_verifyCall")
            ),
            pluginContext = pluginContext
        ) { first() }
    }

    class Invocation(private val pluginContext: IrPluginContext) {
        private val verifiable = Verifiable(pluginContext)

        val classId = verifiable.classId.createNestedClassId(Name.identifier("Invocation"))

        val symbol by lazy {
            pluginContext.referenceClass(classId)!!
        }
    }

    class Matcher(pluginContext: IrPluginContext) {
        private val verifiable = Verifiable(pluginContext)

        val classId = verifiable.classId.createNestedClassId(Name.identifier("Matcher"))

        val symbol by lazy {
            pluginContext.referenceClass(classId)!!
        }

        class Equals(private val pluginContext: IrPluginContext) {
            val matcher = Matcher(pluginContext)

            val classId = matcher.classId.createNestedClassId(Name.identifier("Equals"))

            val symbol by lazy {
                pluginContext.referenceClass(classId)!!
            }

            val value by lazy {
                PropertyDeclaration(pluginContext, CallableId(Name.identifier("value")))
            }
        }

        class SameAs(private val pluginContext: IrPluginContext) {
            val matcher = Matcher(pluginContext)

            val classId = matcher.classId.createNestedClassId(Name.identifier("SameAs"))

            val symbol by lazy {
                pluginContext.referenceClass(classId)!!
            }
        }

        class Anything(private val pluginContext: IrPluginContext) {
            val matcher = Matcher(pluginContext)

            val classId = matcher.classId.createNestedClassId(Name.identifier("Anything"))

            val symbol by lazy {
                pluginContext.referenceClass(classId)!!
            }
        }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
class MutableListFunctions(private val pluginContext: IrPluginContext) {

    val listAdd by lazy {
        FunctionDeclaration {
            pluginContext.irBuiltIns.mutableListClass.functions
                .first {
                    it.owner.name == Name.identifier("add") && it.owner.parameters.size == 2
                }
        }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
class KotlinFunctions(private val pluginContext: IrPluginContext) {
    private val collectionsFqName = FqName("kotlin.collections")
    private val kotlinFqName = FqName("kotlin")

    val mutableListOf by lazy {
        FunctionDeclaration(
            callableId = CallableId(
                collectionsFqName,
                Name.identifier("mutableListOf")
            ),
            pluginContext = pluginContext
        ) { first() }
    }

    val listOf by lazy {
        FunctionDeclaration(
            CallableId(
                collectionsFqName,
                Name.identifier("listOf")
            ),
            pluginContext
        ) {
            first {
                it.owner.parameters.size == 1 &&
                        it.owner.parameters[0].isVararg &&
                        it.owner.returnType.classOrFail.owner.classIdOrFail == pluginContext.irBuiltIns.listClass.owner.classIdOrFail
            }
        }
    }

    val error by lazy {
        FunctionDeclaration(
            CallableId(
                kotlinFqName,
                Name.identifier("error")
            ),
            pluginContext
        ) {
            first {
                it.owner.parameters.size == 1 &&
                        it.owner.parameters[0].type == pluginContext.irBuiltIns.anyType
            }
        }
    }

    val check by lazy {
        FunctionDeclaration(
            CallableId(
                kotlinFqName,
                Name.identifier("check")
            ),
            pluginContext
        ) {
            first {
                it.owner.parameters.size == 2 &&
                        it.owner.parameters[0].type == pluginContext.irBuiltIns.booleanType &&
                        it.owner.parameters[1].type.classOrFail.owner ==
                        pluginContext.irBuiltIns.functionN(0)
            }
        }
    }
}
