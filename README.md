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

        classToTest.doSomething()

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
