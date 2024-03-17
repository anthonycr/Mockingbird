# Mockingbird

A minimalist faking framework exclusively for verifying interactions. It generates fake
implementations of interfaces that can be verified during a test using Kotlin Symbol Processing
(KSP). It does not allow mocking behavior or return values, and does not support mocking concrete
classes.

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
import com.mockingbird.core.Verify
import com.mockingbird.core.fake
import com.mockingbird.core.verify

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
