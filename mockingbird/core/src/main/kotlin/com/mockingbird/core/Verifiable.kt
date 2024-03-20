package com.mockingbird.core

interface Verifiable {

    val invocations: MutableList<Pair<String, List<Any>>>

    var nextInvocationParamVerifier: ((List<Any>) -> Boolean)?

    var verifying: Boolean

    var expected: Int
}

fun verify(vararg any: Any, block: () -> Unit) {
    val verifiable: List<Verifiable> = any.map {
        check(it is Verifiable) { "You can only verify interfaces that have been annotated with Verify" }
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

fun <T : Any> T.verifyParams(verifier: (List<Any>) -> Boolean, invocation: T.() -> Unit) {
    check(this is Verifiable) { "You can only verify interfaces that have been annotated with Verify" }
    check(this.verifying) { "You can only call verifyParams inside a verify block" }
    this.nextInvocationParamVerifier = verifier
    this.invocation()
}

fun <T : Any> T.verifyIgnoreParams(invocation: T.() -> Unit) {
    check(this is Verifiable) { "You can only verify interfaces that have been annotated with Verify" }
    check(this.verifying) { "You can only call verifyIgnoreParams inside a verify block" }
    this.nextInvocationParamVerifier = { true }
    this.invocation()
}

fun Any.verifyNoInvocations() {
    check(this is Verifiable) { "You can only verify interfaces that have been annotated with Verify" }
    check(!this.verifying) { "Do not call verifyNoInvocations from within a verify block" }

    this.verifying = true
    check(this.invocations.isEmpty()) { "Expected no invocations, but found ${this.invocations.size}" }
    this.verifying = false
}

fun Any.times(times: Int, block: () -> Unit) {
    check(this is Verifiable) { "You can only verify interfaces that have been annotated with Verify" }

    this.expected = times
    block()
}
