@file:Suppress("PropertyName")

package com.anthonycr.mockingbird.core

interface Verifiable {

    data class Invocation(
        val functionName: String,
        val parameters: List<Any?>
    )

    val _mockingbird_invocations: MutableList<Invocation>

    var _mockingbird_paramMatcher: List<(Any?, Any?) -> Boolean>

    var _mockingbird_verifying: Boolean

    var _mockingbird_expected: Int
}

fun verify(vararg any: Any, block: () -> Unit) {
    val verifiable: List<Verifiable> = any.map {
        check(it is Verifiable) { MUST_BE_VERIFIABLE }
        it
    }

    verifiable.forEach {
        check(!it._mockingbird_verifying) { "Do not call verify within another verify block" }
        it._mockingbird_verifying = true
        it._mockingbird_expected = 1
    }
    block()
    verifiable.forEach {
        it._mockingbird_verifying = false
    }
}

fun Any.verifyNoInvocations() {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(!this._mockingbird_verifying) { "Do not call verifyNoInvocations from within a verify block" }

    this._mockingbird_verifying = true
    check(this._mockingbird_invocations.isEmpty()) { "Expected no invocations, but found ${this._mockingbird_invocations.size} unverified" }
    this._mockingbird_verifying = false
}

fun Any.times(times: Int, block: () -> Unit) {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }

    this._mockingbird_expected = times
    block()
}
