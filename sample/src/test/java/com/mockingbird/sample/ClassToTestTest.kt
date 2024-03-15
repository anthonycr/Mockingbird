package com.mockingbird.sample

import com.mockingbird.core.Verify
import com.mockingbird.core.fake
import com.mockingbird.core.times
import com.mockingbird.core.verify
import com.mockingbird.core.verifyNoInvocations
import org.junit.Test

class ClassToTestTest {

    @Verify
    val interfaceToVerify: InterfaceToVerify = fake()

    @Test
    fun act1() {
        val classToTest = ClassToTest(interfaceToVerify)

        classToTest.act1()

        interfaceToVerify.verify {
            interfaceToVerify.performAction1(1)
            interfaceToVerify.performAction1(2)
        }
    }

    @Test
    fun act2() {
        val classToTest = ClassToTest(interfaceToVerify)

        classToTest.act2()

        interfaceToVerify.verify {
            interfaceToVerify.performAction2("test", 1)
        }
    }

    @Test
    fun act3() {
        val classToTest = ClassToTest(interfaceToVerify)

        classToTest.act3()

        interfaceToVerify.verify {
            interfaceToVerify.times(2) { interfaceToVerify.performAction1(1) }
        }
    }

    @Test
    fun act4() {
        val classToTest = ClassToTest(interfaceToVerify)

        classToTest.act4()

        interfaceToVerify.verifyNoInvocations()
    }
}
