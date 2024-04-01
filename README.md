# Mockingbird

A minimalist faking framework exclusively for verifying interactions. It generates fake
implementations of interfaces that can be verified during a test using Kotlin Symbol Processing
(KSP). It does not allow mocking behavior or return values, and does not support mocking concrete
classes.

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
fake implementations of interfaces that have `Unit` function types - types usually performing side
effects that are most often verified by a mocking framework.

## Usage

Include the following dependencies in your `build.gradle.kts` file.

```kotlin
testImplementation(project(":mockingbird:core")
ksp(project(":mockingbird:processor"))
```

Make sure you have the KSP Gradle plugin enabled as well.

Within your test, add the following annotation to a property that you wish to verify. Note that the
property you are verifying must be an interface with all functions returning `Unit`.

```kotlin
import com.anthonycr.mockingbird.core.Verify
import com.anthonycr.mockingbird.core.fake
import com.anthonycr.mockingbird.core.verify

interface MyInterface {
  fun doThing(value: String)
}

class MyTest {
  @Verify
  private val myInterface: MyInterface = fake()

  @Test
  fun myTest {
    // Perform action on the interface (usually done implicitly rather than explicitly)
    myInterface.doThing("hello")

    verify(myInterface) {
      myInterface.doThing("hello")
    }
  }
}
```

Note: The `com.anthonycr.mockingbird.core.fake` function is generated at compile time, so it will not be
available to import until KSP runs.
