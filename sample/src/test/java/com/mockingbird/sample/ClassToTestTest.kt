package com.mockingbird.sample

import com.mockingbird.core.Verify
import com.mockingbird.core.fake
import com.mockingbird.core.times
import com.mockingbird.core.verify
import com.mockingbird.core.verifyNoInvocations
import org.junit.Test

class ClassToTestTest {

    @Verify
    val interfaceToVerify1: InterfaceToVerify1 = fake()

    @Verify
    val interfaceToVerify2: InterfaceToVerify2 = fake()

    @Test
    fun act1() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act1()

        verify(interfaceToVerify1, interfaceToVerify2) {
            interfaceToVerify1.performAction1(1)
            interfaceToVerify1.performAction1(2)
            interfaceToVerify2.performAction1(1, "two", "three")
        }
    }

    @Test
    fun act2() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act2()

        verify(interfaceToVerify1, interfaceToVerify2) {
            interfaceToVerify1.performAction2("test", 1)
            interfaceToVerify1.performAction1(1)
            interfaceToVerify2.performAction1(1, "two", "three")
            interfaceToVerify2.performAction1(2, "three", "four")
        }
    }

    @Test
    fun act3() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act3()

        verify(interfaceToVerify1, interfaceToVerify2) {
            interfaceToVerify1.times(2) { interfaceToVerify1.performAction1(1) }
            interfaceToVerify2.times(2) { interfaceToVerify2.performAction1(1, "two", "three") }
        }
    }

    @Test
    fun act4() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act4()

        interfaceToVerify1.verifyNoInvocations()
        interfaceToVerify2.verifyNoInvocations()
    }
}
