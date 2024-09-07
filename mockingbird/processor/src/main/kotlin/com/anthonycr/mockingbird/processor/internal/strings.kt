package com.anthonycr.mockingbird.processor.internal

/**
 * Use a space character to prevent KotlinPoet from wrapping string literals.
 */
fun String.noWrap(): String = this.replace(" ", "·")
