@file:Suppress("unused")

package com.anthonycr.mockingbird.core

/**
 * Create a fake implementation of type [clazz].
 */
fun <T: Any> fake(clazz: Class<T>): T =
    error("Generated code is missing. Please apply the Mockingbird Gradle plugin.")
