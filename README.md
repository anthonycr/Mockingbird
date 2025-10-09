# Mockingbird

A minimalist faking framework exclusively for verifying interactions. It generates fake
implementations of interfaces and abstract classes that can be verified during a test using Kotlin
Symbol Processing (KSP). It does not allow mocking behavior or return values, and does not support
mocking concrete classes.

## Why?

Other testing frameworks exist, like MockK, that support full mocking of all types, a wide range of
features, and have significantly more utility than this minimalist framework. However, mocking
objects is frequently viewed as an anti-pattern, as mocking an object can cover up poor class design
by making untestable code testable. Tests that heavily utilize mocks can lose their utility and even
obscure bugs, or can become difficult to understand and refactor. Additionally, mocks are usually
slow to construct, which in turn slows down test execution.

Fakes are less forgiving then mocks and require better class design in order to support testing.
Because they are less flexible, they have less potential to be abused. However, it can be annoying
to verify interactions on fakes because they are usually handwritten. Mockingbird auto-generates
fake implementations of interfaces and abstract classes that have `Unit` function types - types
usually performing side effects that are most often verified by a mocking framework.

## Usage

![Maven Central Version](https://img.shields.io/maven-central/v/com.anthonycr.mockingbird/core)


Mockingbird is available on Maven Central. Include the following dependencies in your
`build.gradle.kts` file.

```kotlin
testImplementation("com.anthonycr.mockingbird:core:<latest_version>")
kspTest("com.anthonycr.mockingbird:processor:<latest_version>")
```

Make sure you have the KSP Gradle plugin enabled as well.

Within your test, add the following annotation to a property that you wish to verify. Note that the
property you are verifying must be an interface or abstract class with a zero argument constructor.
All functions you wish to verify must return `Unit` and when verifying an abstract class, the
function must be abstract.

```kotlin
import com.anthonycr.mockingbird.core.Verify
import com.anthonycr.mockingbird.core.fake
import com.anthonycr.mockingbird.core.verify

interface Analytics {
    fun trackEvent(event: String)

    fun trackError(exception: Exception)
}

class ClassToTest(private val analytics: Analytics) {
    fun doSomething() {
        analytics.trackEvent("doSomething was called!")
    }

    fun doSomethingElse() {
        analytics.trackError(RuntimeException("Something went wrong"))
    }
}

class ClassToTestTest {

    @Verify
    val analytics: Analytics = fake()

    @Test
    fun `analytics event for doSomething was triggered`() {
        val classToTest = ClassToTest(analytics)

        classToTest.doSomething()

        verify(analytics) {
            analytics.trackEvent("doSomething was called!")
        }
    }

    @Test
    fun `analytics error for doSomethingElse was triggered`() {
        val classToTest = ClassToTest(analytics)

        classToTest.doSomethingElse()

        verify(analytics) {
            analytics.trackError(
                sameAs(RuntimeException("Something went wrong")) { e ->
                    e.message == "Something went wrong")
                }
            )
        }
    }
}
```

There are 3 `verify*` functions.
1. `verify`: This function accepts all the parameters that you wish to verify, performs the
    verifications, and finally asserts that there are no unverified invocations.
2. `verifyPartial`: This function behaves similarly to `verify`, except that it does not assert that
    there are no unverified invocations at the end.
3. `verifyComplete`: This function verifies that there are no unverified invocations left. This is
    useful if you want to verify that no functions were called, or if you are using `verifyPartial`
    and want to assert that one of the subjects being verified is actually fully verified.

There are 3 matcher functions that can be used to match parameters. If any parameter has a matcher,
then all parameters must have a matcher for that invocation.
1. `eq`: Just asserts that the value passed in matches using `==`
2. `sameAs`: Asserts that the value passed in matches using the lambda matcher.
3. `any`: Accepts any parameter, useful for dynamic values or values that you really don't care
    about asserting in your test.

## Benchmark
Run `./benchmark.sh` to execute a benchmark comparison of Mockingbird to Mockk. There are two Mockk
benchmarks. The first, `mockk-interface` just mocks an interface. The second, `mockk-static` uses a
static mock to simulate the worst case scenario for mockk, since static mocks are the most expensive
mocks to run. The benchmark code generates the tests according to the input parameters and then runs
the tests, recording the time it takes to generate the tests, compile the tests, and run the tests.

See the README files for each benchmark ([mockingbird](benchmark/mockingbird/README.md),
[mockk-interface](benchmark/mockk-interface/README.md),
[mockk-static](benchmark/mockk-static/README.md)) for more information about the performance
characteristics of each approach.

### Current benchmark runtimes (3 July 2025)

#### Parameters
- 3 classes to be verified
- 500 tests
- Apple M4 Pro

#### Results
- Mockingbird: 63.7 seconds
- Mockk (Interface): 68.7 seconds
- Mockk (Static): 110.4 seconds
