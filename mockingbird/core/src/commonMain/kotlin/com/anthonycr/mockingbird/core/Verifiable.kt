@file:Suppress("PropertyName")

package com.anthonycr.mockingbird.core

interface Verifiable {

    data class Invocation(
        val functionName: String,
        val parameters: List<Any?>
    )

    sealed class Matcher(val matches: (Any?) -> Boolean) {

        class Equals(val value: Any?) : Matcher({ it == value })

        @Suppress("UNCHECKED_CAST")
        class SameAs<T>(matcher: (T) -> Boolean) : Matcher({ matcher(it as T) })

        object Anything : Matcher({ true })
    }

    val _mockingbird_invocations: MutableList<Invocation>

    var _mockingbird_verificationContext: VerificationContext?
}

@Suppress("FunctionName", "Unused")
fun Verifiable._verifyCall(functionName: String, matchers: List<Verifiable.Matcher>) {
    requireNotNull(_mockingbird_verificationContext)
    val invocation = _mockingbird_invocations.firstOrNull()
    check(invocation != null) {
        "Expected an invocation, but got none instead"
    }
    _mockingbird_invocations.removeAt(0)
    check(invocation.functionName == functionName) {
        "Expected function call $functionName, ${invocation.functionName} was called instead"
    }
    check(matchers.size == invocation.parameters.size) {
        "Expected ${invocation.parameters.size} matchers, found ${matchers.size} instead. When using custom parameter verification, all parameters must use matchers."
    }

    invocation.parameters.forEachIndexed { index, parameter ->
        val matcher = matchers[index]
        check(matcher.matches(parameter)) {
            when (matcher) {
                Verifiable.Matcher.Anything -> "Compiler error."
                is Verifiable.Matcher.Equals -> "Expected argument ${matcher.value}, found ${invocation.parameters[index]} instead."
                is Verifiable.Matcher.SameAs<*> -> "Expected argument to pass [sameAs] matcher, found ${invocation.parameters[index]} instead."
            }
        }
    }
}

class VerificationContext {

    /**
     * Assert that the actual parameter matches the criteria specified by [matcher].
     *
     * @param matcher Return `true` if the expected is the same as the actual, `false` otherwise.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> sameAs(matcher: (actual: T) -> Boolean): T = error("AUTO-GENERATED")

    /**
     * Allows any parameter invocation to match.
     */
    fun <T> any(): T = error("AUTO-GENERATED")
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

const val MUST_BE_VERIFIABLE = "You can only verify interfaces that have been created by fake()"
