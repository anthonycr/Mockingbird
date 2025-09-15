package com.anthonycr.mockingbird.compiler.utils

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

private const val DEBUG = false

fun MessageCollector.debug(message: String) {
    if (DEBUG) {
        report(CompilerMessageSeverity.STRONG_WARNING, "${System.nanoTime()}: " + message)
    }
}

fun MessageCollector.debugDump(irElement: IrElement) {
    if (DEBUG) {
        debug(irElement.dumpKotlinLike())
    }
}

fun MessageCollector.debugDump(irClass: IrClass) {
    if (DEBUG) {
        debug(irClass.dumpKotlinLike())
    }
}
