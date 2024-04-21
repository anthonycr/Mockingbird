package com.anthonycr.mockingbird.core

interface Verifiable {

    val invocations: MutableList<Pair<String, List<Any?>>>

    var nextInvocationParamVerifier: (List<(Any?) -> Boolean>)?

    var verifying: Boolean

    var expected: Int
}

fun verify(vararg any: Any, block: () -> Unit) {
    val verifiable: List<Verifiable> = any.map {
        check(it is Verifiable) { MUST_BE_VERIFIABLE }
        it
    }

    verifiable.forEach {
        check(!it.verifying) { "Do not call verify within another verify block" }
        it.verifying = true
        it.expected = 1
    }
    block()
    verifiable.forEach {
        it.verifying = false
    }
}

fun Any.verifyNoInvocations() {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(!this.verifying) { "Do not call verifyNoInvocations from within a verify block" }

    this.verifying = true
    check(this.invocations.isEmpty()) { "Expected no invocations, but found ${this.invocations.size}" }
    this.verifying = false
}

fun Any.times(times: Int, block: () -> Unit) {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }

    this.expected = times
    block()
}
