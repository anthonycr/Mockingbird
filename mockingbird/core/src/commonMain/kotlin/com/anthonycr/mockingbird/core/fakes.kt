package com.anthonycr.mockingbird.core

/**
 * Create a fake implementation of type [T].
 */
inline fun <reified T> fake(): T = fake(T::class.java)

/**
 * Create a fake implementation of type [clazz].
 */
fun <T> fake(clazz: Class<T>): T = try {
    fakeInternal(clazz)
} catch (noClassDefFoundError: NoClassDefFoundError) {
    throw RuntimeException(
        "Generated code is missing. Please apply the KSP plugin and add `kspTest(com.anthonycr.mockingbird:processor:<latest_version>)` as a dependency.",
        noClassDefFoundError
    )
}
