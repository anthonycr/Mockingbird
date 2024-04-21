package com.anthonycr.mockingbird.sample

import com.anthonycr.mockingbird.core.Verify
import com.anthonycr.mockingbird.core.any
import com.anthonycr.mockingbird.core.eq
import com.anthonycr.mockingbird.core.fake
import com.anthonycr.mockingbird.core.times
import com.anthonycr.mockingbird.core.verify
import com.anthonycr.mockingbird.core.verifyIgnoreParams
import com.anthonycr.mockingbird.core.verifyNoInvocations
import com.anthonycr.mockingbird.core.verifyParams
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

        interfaceToVerify1.verifyNoInvocations()
        interfaceToVerify2.verifyNoInvocations()
    }

    @Test(expected = IllegalStateException::class)
    fun `act1 expected failure`() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act1()

        verify(interfaceToVerify1, interfaceToVerify2) {
            // The first invocation has 1 as its parameter
            interfaceToVerify1.performAction1(2)
        }
    }

    @Test
    fun act2() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act2()

        verify(interfaceToVerify1, interfaceToVerify2) {
            interfaceToVerify1.performAction2("test", 1)
            interfaceToVerify1.performAction1(1)
            interfaceToVerify2.performAction2()
            interfaceToVerify2.performAction1(2, "three", "four")
        }

        interfaceToVerify1.verifyNoInvocations()
        interfaceToVerify2.verifyNoInvocations()
    }

    @Test(expected = IllegalStateException::class)
    fun `act2 expected failure`() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act2()

        verify(interfaceToVerify1, interfaceToVerify2) {
            // performAction2 was invoked first
            interfaceToVerify1.performAction1(1)
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

    @Test(expected = IllegalStateException::class)
    fun `act3  expected failure`() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act3()

        verify(interfaceToVerify1, interfaceToVerify2) {
            // Only invoked 2 times
            interfaceToVerify1.times(3) { interfaceToVerify1.performAction1(1) }
        }
    }

    @Test
    fun act4() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act4()

        interfaceToVerify1.verifyNoInvocations()
        interfaceToVerify2.verifyNoInvocations()
    }

    @Test(expected = IllegalStateException::class)
    fun `act4 expected failure`() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act3()

        // act3 function call actually has invocations
        interfaceToVerify1.verifyNoInvocations()
    }

    @Test(expected = IllegalStateException::class)
    fun `double verify expected failure`() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act3()

        verify(interfaceToVerify1) {
            verify(interfaceToVerify1) {
                // Won't get here
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `verifyNoInvocations within verify expected failure`() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act3()

        verify(interfaceToVerify1) {
            interfaceToVerify1.verifyNoInvocations()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `verification of inequitable type expected failure`() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act5()

        verify(interfaceToVerify1) {
            interfaceToVerify1.performAction3(Exception("test"))
        }
    }

    @Test
    fun `verification of inequitable type with verifyIgnoreParameter`() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act5()

        verify(interfaceToVerify1) {
            interfaceToVerify1.verifyIgnoreParams { performAction3(Exception("test")) }
        }
    }

    @Test
    fun `verification of inequitable type with verifyParams`() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act5()

        verify(interfaceToVerify1) {
            interfaceToVerify1.verifyParams(
                func = InterfaceToVerify1::performAction3,
                p0 = eq(Exception("test")) { e, a -> e.message == a.message }
            )
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `verification of inequitable type with verifyParams expected failure`() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act5()

        verify(interfaceToVerify1) {
            interfaceToVerify1.verifyParams(
                func = InterfaceToVerify1::performAction3,
                p0 = eq(Exception("test1")) { e, a -> e.message == a.message }
            )
        }
    }

    @Test
    fun `verification of multiple types with verifyParams`() {
        val classToTest = ClassToTest(interfaceToVerify1, interfaceToVerify2)

        classToTest.act6()

        verify(interfaceToVerify1) {
            interfaceToVerify1.verifyParams(
                func = InterfaceToVerify1::performAction4,
                p0 = eq("one"),
                p1 = any(1),
                p2 = eq(Exception("test")) { e, a -> e.message == a.message }
            )
        }
    }
}
