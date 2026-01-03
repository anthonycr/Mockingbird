package com.anthonycr.mockingbird.compiler.ir

import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addBackingField
import org.jetbrains.kotlin.ir.builders.declarations.addDefaultGetter
import org.jetbrains.kotlin.ir.builders.declarations.addDefaultSetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

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

@UnsafeDuringIrConstructionAPI
fun IrClass.addPropertyFromDeclaration(
    irBuiltIns: IrBuiltIns,
    propertyDeclaration: PropertyDeclaration,
    mutable: Boolean,
    defaultValue: DeclarationIrBuilder.() -> IrExpression
) = addProperty {
    name = propertyDeclaration.callableId.callableName
    updateFrom(propertyDeclaration.symbol.owner)
}.apply {
    overriddenSymbols = listOf(propertyDeclaration.symbol)
    addBackingField {
        type = propertyDeclaration.getter.returnType
    }.apply {
        initializer = with(irBuiltIns.createIrBuilder(symbol)) {
            irExprBody(defaultValue())
        }
    }
    addDefaultGetter(this@addPropertyFromDeclaration, irBuiltIns)
    getter!!.overriddenSymbols = listOf(propertyDeclaration.getter.symbol)
    if (mutable) {
        addDefaultSetter(this@addPropertyFromDeclaration, irBuiltIns)
        setter!!.overriddenSymbols = listOf(propertyDeclaration.setter.symbol)
    }
}
