package com.mockingbird.core

interface Verifiable {

    val invocations: MutableMap<String, MutableList<List<Any>>>

    var verifying: Boolean

    var expected: Int
}

fun Any.verify(block: () -> Unit) {
    check(this is Verifiable) { "You can only verify interfaces that have been annotated with Verify" }

    this.verifying = true
    this.expected = 1
    block()
    this.verifying = false
}

fun Any.verifyNoInvocations() {
    check(this is Verifiable) { "You can only verify interfaces that have been annotated with Verify" }

    this.verifying = true
    check(this.invocations.values.isEmpty()) { "Expected no invocations, but found ${this.invocations.values.isEmpty()}" }
    this.verifying = false
}


fun Any.times(times: Int, block: () -> Unit) {
    check(this is Verifiable) { "You can only verify interfaces that have been annotated with Verify" }

    this.expected = times
    block()
}
