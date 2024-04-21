@file:Suppress("UNCHECKED_CAST")

package com.anthonycr.mockingbird.core

private const val MUST_BE_VERIFYING = "You can only call verifyParams inside a verify block"

/**
 * Assert that the parameter is equal to [other] using `==`.
 */
fun <T> eq(other: T): Pair<T, (T) -> Boolean> {
    return other to { it == other }
}

/**
 * Assert that the parameter is equal using the criteria provided by the [condition].
 *
 * @param other Required to execute the test, value is ignored in favor of matching using the
 * [condition].
 */
fun <T> eq(other: T, condition: (T) -> Boolean): Pair<T, (T) -> Boolean> {
    return other to condition
}

/**
 * Allows any parameter invocation to match.
 *
 * @param other Required to execute the test, value is ignored and any parameter passed will match.
 */
fun <T> any(other: T): Pair<T, (T) -> Boolean> {
    return other to { true }
}

fun <T, P0 : Any?> T.verifyParams(
    func: T.(P0) -> Unit,
    p0: Pair<P0, (P0) -> Boolean>
) {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(this.verifying) { MUST_BE_VERIFYING }
    this.nextInvocationParamVerifier = listOf { p0.second(it as P0) }
    func(p0.first)
}

fun <T, P0 : Any?, P1 : Any?> T.verifyParams(
    func: T.(P0, P1) -> Unit,
    p0: Pair<P0, (P0) -> Boolean>,
    p1: Pair<P1, (P1) -> Boolean>
) {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(this.verifying) { MUST_BE_VERIFYING }
    this.nextInvocationParamVerifier = listOf(
        { p0.second(it as P0) },
        { p1.second(it as P1) }
    )
    func(p0.first, p1.first)
}

fun <T, P0 : Any?, P1 : Any?, P2 : Any?> T.verifyParams(
    func: T.(P0, P1, P2) -> Unit,
    p0: Pair<P0, (P0) -> Boolean>,
    p1: Pair<P1, (P1) -> Boolean>,
    p2: Pair<P2, (P2) -> Boolean>
) {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(this.verifying) { MUST_BE_VERIFYING }
    this.nextInvocationParamVerifier = listOf(
        { p0.second(it as P0) },
        { p1.second(it as P1) },
        { p2.second(it as P2) }
    )
    func(p0.first, p1.first, p2.first)
}

fun <T, P0 : Any?, P1 : Any?, P2 : Any?, P3 : Any?> T.verifyParams(
    func: T.(P0, P1, P2, P3) -> Unit,
    p0: Pair<P0, (P0) -> Boolean>,
    p1: Pair<P1, (P1) -> Boolean>,
    p2: Pair<P2, (P2) -> Boolean>,
    p3: Pair<P3, (P3) -> Boolean>
) {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(this.verifying) { MUST_BE_VERIFYING }
    this.nextInvocationParamVerifier = listOf(
        { p0.second(it as P0) },
        { p1.second(it as P1) },
        { p2.second(it as P2) },
        { p3.second(it as P3) }
    )
    func(p0.first, p1.first, p2.first, p3.first)
}

fun <T, P0 : Any?, P1 : Any?, P2 : Any?, P3 : Any?, P4 : Any?> T.verifyParams(
    func: T.(P0, P1, P2, P3, P4) -> Unit,
    p0: Pair<P0, (P0) -> Boolean>,
    p1: Pair<P1, (P1) -> Boolean>,
    p2: Pair<P2, (P2) -> Boolean>,
    p3: Pair<P3, (P3) -> Boolean>,
    p4: Pair<P4, (P4) -> Boolean>
) {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(this.verifying) { MUST_BE_VERIFYING }
    this.nextInvocationParamVerifier = listOf(
        { p0.second(it as P0) },
        { p1.second(it as P1) },
        { p2.second(it as P2) },
        { p3.second(it as P3) },
        { p4.second(it as P4) }
    )
    func(p0.first, p1.first, p2.first, p3.first, p4.first)
}

fun <T : Any> T.verifyIgnoreParams(invocation: T.() -> Unit) {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(this.verifying) { "You can only call verifyIgnoreParams inside a verify block" }
    this.nextInvocationParamVerifier = listOf { true }
    this.invocation()
}
