package com.anthonycr.mockingbird.compiler.ir

import com.anthonycr.mockingbird.compiler.ir.MockingbirdFunctionCollector.Companion.fakeFunctionFqName
import com.anthonycr.mockingbird.compiler.utils.debug
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class MockingbirdCallSiteTransformer(
    val messageCollector: MessageCollector,
    val pluginContext: IrPluginContext,
    val generatedClasses: List<IrClass>
) : IrElementTransformerVoid() {

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.symbol.owner.kotlinFqName == fakeFunctionFqName) {
            val replacement =
                generatedClasses.first { it.fqNameWhenAvailable!!.asString() == "${expression.type.classFqName!!.asString()}_Fake" }
            val constructor = replacement.primaryConstructor!!.symbol
            messageCollector.debug("Transforming call site for ${constructor.owner.returnType.classFqName}")

            return pluginContext.irBuiltIns.createIrBuilder(expression.symbol)
                .irCall(constructor)
        }
        return super.visitCall(expression)
    }
}
