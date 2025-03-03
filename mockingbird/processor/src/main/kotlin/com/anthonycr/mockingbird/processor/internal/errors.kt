package com.anthonycr.mockingbird.processor.internal

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.NonExistLocation

/**
 * Check the provided [condition] and only allow the items through that pass.
 */
fun <T> Sequence<T>.check(
    message: String,
    logger: KSPLogger,
    node: (T) -> KSNode,
    condition: (T) -> Boolean
): Sequence<T> = filter {
    val passed = condition(it)

    if (!passed) {
        val realNode = node(it)
        val actualMessage = when (val location = realNode.location) {
            is FileLocation -> "$message: ${location.filePath}:${location.lineNumber}"
            NonExistLocation -> message
        }
        logger.error(actualMessage, node(it))
    }

    passed
}
