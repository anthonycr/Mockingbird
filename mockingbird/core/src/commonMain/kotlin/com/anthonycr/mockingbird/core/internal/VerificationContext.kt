package com.anthonycr.mockingbird.core.internal

class VerificationContext {

    /**
     * Assert that the actual parameter matches the criteria specified by [matcher].
     *
     * @param matcher Return `true` if the expected is the same as the actual, `false` otherwise.
     */
    @Suppress("unused")
    fun <T> sameAs(matcher: (actual: T) -> Boolean): T = error("AUTO-GENERATED")

    /**
     * Allows any parameter invocation to match.
     */
    fun <T> any(): T = error("AUTO-GENERATED")
}
