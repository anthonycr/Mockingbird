package com.anthonycr.sample

import com.anthonycr.mockingbird.core.fake
import com.anthonycr.mockingbird.core.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RobolectricTest {

    val interfaceToVerify = fake<InterfaceToVerify>()
    val lambdaToVerify1 = fake<() -> Unit>()
    val lambdaToVerify2 = fake<() -> Unit>()

    @Test
    fun robolectric() {
        interfaceToVerify.performAction()
        lambdaToVerify1()
        lambdaToVerify2()

        verify(interfaceToVerify, lambdaToVerify1, lambdaToVerify2) {
            interfaceToVerify.performAction()
            lambdaToVerify1()
            lambdaToVerify2()
        }
    }

}
