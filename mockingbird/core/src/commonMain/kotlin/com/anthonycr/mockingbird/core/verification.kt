@file:Suppress("PropertyName")

package com.anthonycr.mockingbird.core

import com.anthonycr.mockingbird.core.internal.Verifiable
import com.anthonycr.mockingbird.core.internal.VerificationContext

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
