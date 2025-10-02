package com.anthonycr.mockingbird.compiler.ir

import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

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
    irFunctionDeclaration: FunctionDeclaration,
    vararg parameters: IrExpression
): IrFunctionAccessExpression = builder.irCall(irFunctionDeclaration.symbol).apply {
    arguments[0] = this@irCallFunction
    parameters.forEachIndexed { index, parameter ->
        arguments[index + 1] = parameter
    }
}
