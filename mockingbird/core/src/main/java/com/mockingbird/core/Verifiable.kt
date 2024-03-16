package com.mockingbird.core

interface Verifiable {

    val invocations: MutableList<Pair<String, List<Any>>>

    var verifying: Boolean

    var expected: Int
}

fun verify(vararg any: Any, block: () -> Unit) {
    val verifiable: List<Verifiable> = any.map {
        check(it is Verifiable) { "You can only verify interfaces that have been annotated with Verify" }
        it
    }

    verifiable.forEach {
        it.verifying = true
        it.expected = 1
    }
    block()
    verifiable.forEach {
        it.verifying = false
    }
}

fun Any.verifyNoInvocations() {
    check(this is Verifiable) { "You can only verify interfaces that have been annotated with Verify" }

    this.verifying = true
    check(this.invocations.isEmpty()) { "Expected no invocations, but found ${this.invocations.size}" }
    this.verifying = false
}

fun Any.times(times: Int, block: () -> Unit) {
    check(this is Verifiable) { "You can only verify interfaces that have been annotated with Verify" }

    this.expected = times
    block()
}
