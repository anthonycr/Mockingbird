package com.anthonycr.mockingbird.core

import org.junit.Test

class FakesKtTest {

    @Test
    fun `inline fake throws exception when there is no gradle plugin applied`() {
        assertThrows<RuntimeException>("Generated code is missing. Please apply the Mockingbird Gradle plugin.") {
            fake<Unit>()
        }
    }

    @Test
    fun `fake throws exception when there is no gradle plugin applied`() {
        assertThrows<RuntimeException>("Generated code is missing. Please apply the Mockingbird Gradle plugin.") {
            fake(Unit::class.java)
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
                error("Expected exception of type ${T::class.java.simpleName}, ${t::class.java.simpleName} was thrown instead")
            }
        }
    }
}
