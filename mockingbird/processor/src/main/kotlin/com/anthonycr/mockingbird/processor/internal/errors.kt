package com.anthonycr.mockingbird.processor.internal

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

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
        logger.error(message, node(it))
    }

    passed
}
