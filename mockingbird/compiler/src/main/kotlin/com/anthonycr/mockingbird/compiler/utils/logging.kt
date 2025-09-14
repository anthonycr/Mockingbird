package com.anthonycr.mockingbird.compiler.utils

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

fun MessageCollector.debug(message: String) {
    report(CompilerMessageSeverity.STRONG_WARNING, "${System.nanoTime()}: " + message)
}
