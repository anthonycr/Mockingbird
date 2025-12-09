package com.anthonycr.mockingbird.core.internal

@Suppress("PropertyName")
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
        "expected: < FUNCTION_CALL: $functionName> but was: < FUNCTION_CALL: ${invocation.functionName}>"
    }
    check(matchers.size == invocation.parameters.size) {
        "Expected ${invocation.parameters.size} matchers, found ${matchers.size} instead. This is a bug, please file an issue."
    }

    invocation.parameters.forEachIndexed { index, parameter ->
        val matcher = matchers[index]
        check(matcher.matches(parameter)) {
            when (matcher) {
                Verifiable.Matcher.Anything -> "This is a bug, please file an issue."
                is Verifiable.Matcher.Equals -> "expected: < ARGUMENT $index: ${matcher.value}> but was: < ARGUMENT $index: ${invocation.parameters[index]}>"
                is Verifiable.Matcher.SameAs<*> -> "[sameAs] matcher for <ARGUMENT $index> rejects value: ${invocation.parameters[index]}"
            }
        }
    }
}
