package com.anthonycr.mockingbird.compiler.ir

import com.anthonycr.mockingbird.compiler.utils.debug
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName

class MockingbirdFunctionCollector(
    private val messageCollector: MessageCollector
) : IrElementTransformerVoid() {

    val typesToGenerate = mutableMapOf<FqName, IrType>()

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.symbol.owner.kotlinFqName == fakeFunctionFqName) {
            typesToGenerate.put(expression.type.classFqName!!, expression.type)
            messageCollector.debug("Collected type from call ${expression.type.classFqName!!}")
        }
        return super.visitCall(expression)
    }

    companion object {
        val fakeFunctionFqName = FqName("com.anthonycr.mockingbird.core.fake")
    }
}
