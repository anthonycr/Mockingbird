@file:Suppress("unused")

package com.anthonycr.mockingbird.core

/**
 * Create a fake implementation of type [T].
 */
inline fun <reified T> fake(): T = fake(T::class.java)

/**
 * Create a fake implementation of type [clazz].
 */
fun <T> fake(clazz: Class<T>): T =
    error("Generated code is missing. Please apply the Mockingbird Gradle plugin.")
