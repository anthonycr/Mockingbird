@file:Suppress("PropertyName")

package com.anthonycr.mockingbird.core

interface Verifiable {

    data class Invocation(
        val functionName: String,
        val parameters: List<Any?>
    )

    val _mockingbird_invocations: MutableList<Invocation>

    var _mockingbird_verificationContext: VerificationContext?
}

class VerificationContext {

    var parameterMatcher: List<(Any?, Any?) -> Boolean> = emptyList()

    companion object {
        val DEFAULT_MATCHER: (Any?, Any?) -> Boolean = { e, a -> e == a }
    }

    /**
     * Assert that the actual parameter is equal to [expected] using `==`. The same as passing a
     * parameter directly to the function.
     */
    fun <T> eq(expected: T): T {
        parameterMatcher += { e, a -> e == a }
        return expected
    }

    /**
     * Assert that the actual parameter is the same as the [expected] using the criteria provided by the
     * [matcher].
     *
     * @param expected The expected parameter that is evaluated against the actual parameter using the
     * [matcher].
     * @param matcher Return `true` if the expected is the same as the actual, `false` otherwise.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> sameAs(
        expected: T,
        matcher: (actual: T) -> Boolean
    ): T {
        parameterMatcher += { _, a -> matcher(a as T) }
        return expected
    }

    /**
     * Allows any parameter invocation to match.
     *
     * @param anything Required to execute the test, value is ignored and any parameter passed will match.
     */
    fun <T> any(anything: T): T {
        parameterMatcher += { _, _ -> true }
        return anything
    }
}

/**
 * Verifies the [verifiable] and invokes the [block], in which to call functions on the [verifiable]
 * that are expected to be called. All expected invocations must be verified within the [block].
 *
 * This function cannot be called within another [verify] or [verifyPartial] block.
 */
inline fun verify(vararg verifiable: Any, block: VerificationContext.() -> Unit) {
    val verifiableList: List<Verifiable> = verifiable.map {
        check(it is Verifiable) { MUST_BE_VERIFIABLE }
        it
    }

    val verificationContext = VerificationContext()
    verifiableList.forEach {
        check(it._mockingbird_verificationContext == null) { "Do not call verify within another verify block" }
        it._mockingbird_verificationContext = verificationContext
    }
    verificationContext.block()
    verifiableList.forEach {
        check(it._mockingbird_invocations.isEmpty()) { "Found ${it._mockingbird_invocations.size} unverified invocations" }
        it._mockingbird_verificationContext = null
    }
}

/**
 * Verifies the [verifiable] and invokes the [block], in which to call functions on the [verifiable]
 * that are expected to be called. Unlike [verify] not all invocations need to be verified.
 *
 * This function cannot be called within another [verify] or [verifyPartial] block.
 */
inline fun verifyPartial(vararg verifiable: Any, block: VerificationContext.() -> Unit) {
    val verifiableList: List<Verifiable> = verifiable.map {
        check(it is Verifiable) { MUST_BE_VERIFIABLE }
        it
    }

    val verificationContext = VerificationContext()
    verifiableList.forEach {
        check(it._mockingbird_verificationContext == null) { "Do not call verifyPartial within another verify block" }
        it._mockingbird_verificationContext = verificationContext
    }
    verificationContext.block()
    verifiableList.forEach {
        it._mockingbird_invocations.clear()
        it._mockingbird_verificationContext = null
    }
}

/**
 * Verifies that all expected function invocations have been verified. Useful if there are no
 * invocations expected for a certain verifiable.
 *
 * Instead of calling [verify] and not doing anything in the block
 * ```
 * verify(myInterface) {
 *
 * }
 * ```
 * Just use [verifyComplete]
 * ```
 * myInterface.verifyComplete()
 * ```
 *
 * This function cannot be called from within a [verify] block, which does its own version of
 * completion verification.
 */
fun Any.verifyComplete() {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(this._mockingbird_verificationContext == null) { "Do not call verifyComplete from within a verify block" }
    check(this._mockingbird_invocations.isEmpty()) { "Expected no invocations, but found ${this._mockingbird_invocations.size} unverified" }
}
