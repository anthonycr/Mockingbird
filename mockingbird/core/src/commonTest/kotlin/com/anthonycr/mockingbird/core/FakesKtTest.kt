package com.anthonycr.mockingbird.core

import org.junit.Test

class FakesKtTest {

    @Test(expected = RuntimeException::class)
    fun `inline fake throws exception when there is no ksp dependency`() {
        fake<Unit>()
    }

    @Test(expected = RuntimeException::class)
    fun `fake throws exception when there is no ksp dependency`() {
        fake(Unit::class.java)
    }
}
