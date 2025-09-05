package com.anthonycr.mockingbird.compiler.utils

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrType.name(): String {
    return ((this as IrSimpleType).classifier.owner as IrDeclarationWithName).name.asString()
}

fun IrTypeArgument.name(): String {
    return (this as IrTypeProjection).type.name()
}

fun MessageCollector.debug(message: String) {
    report(CompilerMessageSeverity.STRONG_WARNING, message)
}
