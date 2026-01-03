@file:Suppress("unused")

package com.anthonycr.mockingbird.core

import kotlin.reflect.KClass

/**
 * Create a fake implementation of type [T].
 */
inline fun <reified T: Any> fake(): T = fake(T::class)

/**
 * Create a fake implementation of type [clazz].
 */
fun <T : Any> fake(clazz: KClass<T>): T =
    error("Generated code is missing. Please apply the Mockingbird Gradle plugin.")
