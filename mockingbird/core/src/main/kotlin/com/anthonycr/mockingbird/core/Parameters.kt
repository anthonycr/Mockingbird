@file:Suppress("UNCHECKED_CAST")

package com.anthonycr.mockingbird.core

private const val MUST_BE_VERIFYING = "You can only call verifyParams inside a verify block"

/**
 * Assert that the parameter is equal to [other] using `==`.
 */
fun <T> eq(other: T): Pair<T, (T, T) -> Boolean> {
    return other to { e, a -> e == a }
}

/**
 * Assert that parameters of a certain type match using the criteria provided by the [condition].
 *
 * @param other Required to execute the test, value is ignored in favor of matching using the
 * [condition].
 */
fun <T> eq(other: T, condition: (expected: T, actual: T) -> Boolean): Pair<T, (T, T) -> Boolean> {
    return other to condition
}

/**
 * Allows any parameter invocation to match.
 *
 * @param other Required to execute the test, value is ignored and any parameter passed will match.
 */
fun <T> any(other: T): Pair<T, (T, T) -> Boolean> {
    return other to { _, _ -> true }
}

fun <T, P0> T.verifyParams(
    func: T.(P0) -> Unit,
    p0: Pair<P0, (P0, P0) -> Boolean>
) {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(this._mockingbird_verifying) { MUST_BE_VERIFYING }
    this._mockingbird_paramMatcher = listOf { e, a -> p0.second(e as P0, a as P0) }
    func(p0.first)
}

fun <T, P0, P1> T.verifyParams(
    func: T.(P0, P1) -> Unit,
    p0: Pair<P0, (P0, P0) -> Boolean>,
    p1: Pair<P1, (P1, P1) -> Boolean>
) {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(this._mockingbird_verifying) { MUST_BE_VERIFYING }
    this._mockingbird_paramMatcher = listOf(
        { e, a -> p0.second(e as P0, a as P0) },
        { e, a -> p1.second(e as P1, a as P1) }
    )
    func(p0.first, p1.first)
}

fun <T, P0, P1, P2> T.verifyParams(
    func: T.(P0, P1, P2) -> Unit,
    p0: Pair<P0, (P0, P0) -> Boolean>,
    p1: Pair<P1, (P1, P1) -> Boolean>,
    p2: Pair<P2, (P2, P2) -> Boolean>
) {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(this._mockingbird_verifying) { MUST_BE_VERIFYING }
    this._mockingbird_paramMatcher = listOf(
        { e, a -> p0.second(e as P0, a as P0) },
        { e, a -> p1.second(e as P1, a as P1) },
        { e, a -> p2.second(e as P2, a as P2) }
    )
    func(p0.first, p1.first, p2.first)
}

fun <T, P0, P1, P2, P3> T.verifyParams(
    func: T.(P0, P1, P2, P3) -> Unit,
    p0: Pair<P0, (P0, P0) -> Boolean>,
    p1: Pair<P1, (P1, P1) -> Boolean>,
    p2: Pair<P2, (P2, P2) -> Boolean>,
    p3: Pair<P3, (P3, P3) -> Boolean>
) {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(this._mockingbird_verifying) { MUST_BE_VERIFYING }
    this._mockingbird_paramMatcher = listOf(
        { e, a -> p0.second(e as P0, a as P0) },
        { e, a -> p1.second(e as P1, a as P1) },
        { e, a -> p2.second(e as P2, a as P2) },
        { e, a -> p3.second(e as P3, a as P3) }
    )
    func(p0.first, p1.first, p2.first, p3.first)
}

fun <T, P0, P1, P2, P3, P4> T.verifyParams(
    func: T.(P0, P1, P2, P3, P4) -> Unit,
    p0: Pair<P0, (P0, P0) -> Boolean>,
    p1: Pair<P1, (P1, P1) -> Boolean>,
    p2: Pair<P2, (P2, P2) -> Boolean>,
    p3: Pair<P3, (P3, P3) -> Boolean>,
    p4: Pair<P4, (P4, P4) -> Boolean>
) {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(this._mockingbird_verifying) { MUST_BE_VERIFYING }
    this._mockingbird_paramMatcher = listOf(
        { e, a -> p0.second(e as P0, a as P0) },
        { e, a -> p1.second(e as P1, a as P1) },
        { e, a -> p2.second(e as P2, a as P2) },
        { e, a -> p3.second(e as P3, a as P3) },
        { e, a -> p4.second(e as P4, a as P4) }
    )
    func(p0.first, p1.first, p2.first, p3.first, p4.first)
}

fun <T : Any> T.verifyIgnoreParams(invocation: T.() -> Unit) {
    check(this is Verifiable) { MUST_BE_VERIFIABLE }
    check(this._mockingbird_verifying) { "You can only call verifyIgnoreParams inside a verify block" }
    this._mockingbird_paramMatcher = listOf { _, _ -> true }
    this.invocation()
}
