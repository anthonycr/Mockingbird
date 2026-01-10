package com.anthonycr.mockingbird.core

import kotlin.test.Test

class FakesKtTest {

    @Test
    fun inline_fake_throws_exception_when_there_is_no_gradle_plugin_applied() {
        assertThrows<RuntimeException>("Generated code is missing. Please apply the Mockingbird Gradle plugin.") {
            fake<Unit>()
        }
    }

    @Test
    fun fake_throws_exception_when_there_is_no_gradle_plugin_applied() {
        assertThrows<RuntimeException>("Generated code is missing. Please apply the Mockingbird Gradle plugin.") {
            fake(Unit::class)
        }
    }

    inline fun <reified T : Throwable> assertThrows(message: String, block: () -> Unit) {
        try {
            block()
            error("Excepted exception to be thrown, but none was")
        } catch (t: Throwable) {
            if (t is T) {
                require(t.message == message) {
                    "Exception message mismatch\n\tExpected: $message\n\tActual: ${t.message}"
                }
            } else {
                error("Expected exception of type ${T::class.simpleName}, ${t::class.simpleName} was thrown instead")
            }
        }
    }
}
