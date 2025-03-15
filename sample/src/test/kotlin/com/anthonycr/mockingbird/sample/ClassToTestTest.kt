package com.anthonycr.mockingbird.sample

import com.anthonycr.mockingbird.core.Verify
import com.anthonycr.mockingbird.core.any
import com.anthonycr.mockingbird.core.eq
import com.anthonycr.mockingbird.core.fake
import com.anthonycr.mockingbird.core.sameAs
import com.anthonycr.mockingbird.core.verify
import com.anthonycr.mockingbird.core.verifyComplete
import com.anthonycr.mockingbird.core.verifyIgnoreParams
import com.anthonycr.mockingbird.core.verifyParams
import com.anthonycr.mockingbird.core.verifyPartial
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ClassToTestTest {

    @Verify
    val lambdaToVerify1: (String) -> Unit = fake()

    @Verify
    val lambdaToVerify2: (Int) -> Unit = fake()

    @Verify
    val interfaceToVerify1 = fake<InterfaceToVerify1>()

    @Verify
    val interfaceToVerify2 = fake(InterfaceToVerify2::class.java)

    @Verify
    val classToVerify1 = fake<ClassToVerify1>()

    @Verify
    val classToVerify2 = fake<ClassToVerify2>()

    private fun createClassToTest() = ClassToTest(
        lambdaToVerify1,
        lambdaToVerify2,
        interfaceToVerify1,
        interfaceToVerify2,
        classToVerify1,
        classToVerify2
    )

    @Test
    fun act1() {
        val classToTest = createClassToTest()

        classToTest.act1()

        verify(lambdaToVerify1, lambdaToVerify2, interfaceToVerify1, interfaceToVerify2) {
            lambdaToVerify1("test")
            lambdaToVerify2(1)
            interfaceToVerify1.performAction1(1)
            interfaceToVerify1.performAction1(2)
            interfaceToVerify2.performAction1(1, "two", "three")
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `act1 expected failure`() {
        val classToTest = createClassToTest()

        classToTest.act1()

        verify(lambdaToVerify1, lambdaToVerify2, interfaceToVerify1, interfaceToVerify2) {
            lambdaToVerify1("test")
            // The first invocation has 1 as its parameter
            interfaceToVerify1.performAction1(2)
        }
    }

    @Test
    fun act2() {
        val classToTest = createClassToTest()

        classToTest.act2()

        verify(interfaceToVerify1, interfaceToVerify2) {
            interfaceToVerify1.performAction2("test", 1)
            interfaceToVerify1.performAction1(1)
            interfaceToVerify2.performAction2()
            interfaceToVerify2.performAction1(2, "three", "four")
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `act2 expected failure`() {
        val classToTest = createClassToTest()

        classToTest.act2()

        verify(interfaceToVerify1, interfaceToVerify2) {
            // performAction2 was invoked first
            interfaceToVerify1.performAction1(1)
        }
    }

    @Test
    fun act3() {
        val classToTest = createClassToTest()

        classToTest.act3()

        verify(interfaceToVerify1, interfaceToVerify2) {
            interfaceToVerify1.performAction1(1)
            interfaceToVerify1.performAction1(1)
            interfaceToVerify2.performAction1(1, "two", "three")
            interfaceToVerify2.performAction1(1, "two", "three")
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `act3  expected failure`() {
        val classToTest = createClassToTest()

        classToTest.act3()

        verify(interfaceToVerify1, interfaceToVerify2) {
            // Only invoked 2 times
            repeat(3) {
                interfaceToVerify1.performAction1(1)
            }
        }
    }

    @Test
    fun act4() {
        val classToTest = createClassToTest()

        classToTest.act4()

        lambdaToVerify1.verifyComplete()
        interfaceToVerify1.verifyComplete()
        interfaceToVerify2.verifyComplete()
    }

    @Test(expected = IllegalStateException::class)
    fun `act3 expected failure`() {
        val classToTest = createClassToTest()

        classToTest.act3()

        // act3 function call actually has invocations
        interfaceToVerify1.verifyComplete()
    }

    @Test(expected = IllegalStateException::class)
    fun `act3 expected failure unverified`() {
        val classToTest = createClassToTest()

        classToTest.act3()

        verify(interfaceToVerify1, interfaceToVerify2) {
            interfaceToVerify1.performAction1(1)
            interfaceToVerify1.performAction1(1)
            interfaceToVerify2.performAction1(1, "two", "three")
        }
    }

    @Test
    fun `act3 passes with unverified using verifyPartial`() {
        val classToTest = createClassToTest()

        classToTest.act3()

        verifyPartial(interfaceToVerify1, interfaceToVerify2) {
            interfaceToVerify1.performAction1(1)
            interfaceToVerify1.performAction1(1)
            interfaceToVerify2.performAction1(1, "two", "three")
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `double verify expected failure`() {
        val classToTest = createClassToTest()

        classToTest.act3()

        verify(interfaceToVerify1) {
            verify(interfaceToVerify1) {
                // Won't get here
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `verifyNoInvocations within verify expected failure`() {
        val classToTest = createClassToTest()

        classToTest.act3()

        verify(interfaceToVerify1) {
            interfaceToVerify1.verifyComplete()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `verification of inequitable type expected failure`() {
        val classToTest = createClassToTest()

        classToTest.act5()

        verify(interfaceToVerify1) {
            interfaceToVerify1.performAction3(Exception("test"))
        }
    }

    @Test
    fun `verification of inequitable type with verifyIgnoreParameter`() {
        val classToTest = createClassToTest()

        classToTest.act5()

        verify(interfaceToVerify1) {
            interfaceToVerify1.verifyIgnoreParams { performAction3(Exception("test")) }
        }
    }

    @Test
    fun `verification of inequitable type with verifyParams`() {
        val classToTest = createClassToTest()

        classToTest.act5()

        verify(interfaceToVerify1) {
            interfaceToVerify1.verifyParams(
                func = InterfaceToVerify1::performAction3,
                p0 = sameAs(Exception("test")) { e, a -> e.message == a.message }
            )
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `verification of inequitable type with verifyParams expected failure`() {
        val classToTest = createClassToTest()

        classToTest.act5()

        verify(interfaceToVerify1) {
            interfaceToVerify1.verifyParams(
                func = InterfaceToVerify1::performAction3,
                p0 = sameAs(Exception("test1")) { e, a -> e.message == a.message }
            )
        }
    }

    @Test
    fun `verification of multiple types with verifyParams`() {
        val classToTest = createClassToTest()

        classToTest.act6()

        verify(interfaceToVerify1) {
            interfaceToVerify1.verifyParams(
                func = InterfaceToVerify1::performAction4,
                p0 = eq("one"),
                p1 = any(1),
                p2 = sameAs(Exception("test")) { e, a -> e.message == a.message }
            )
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `verification of non Unit returns is not allowed`() {
        val classToTest = createClassToTest()

        classToTest.act7()
    }

    @Test
    fun `verification of suspending function works as expected`() = runTest {
        val classToTest = createClassToTest()

        classToTest.act8()

        verify(interfaceToVerify2) {
            interfaceToVerify2.performAction4("one")
        }
    }

    @Test
    fun `verification of suspending functions parameters works as expected`() = runTest {
        val classToTest = createClassToTest()

        classToTest.act8()

        verify(interfaceToVerify2) {
            interfaceToVerify2.verifyParams(
                func = InterfaceToVerify2::performAction4,
                p0 = eq("one")
            )
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `verification of non Unit returns suspending function is not allowed`() = runTest {
        val classToTest = createClassToTest()

        classToTest.act9()
    }

    @Test
    fun `abstract class with all abstract functions can be verified`() {
        val classToTest = createClassToTest()

        classToTest.act10()

        verify(classToVerify1) {
            classToVerify1.act1()
        }
    }

    @Test
    fun `abstract class with an abstract function and real function can be verified`() {
        val classToTest = createClassToTest()

        classToTest.act11()

        verify(classToVerify2) {
            classToVerify2.act1("one")
            classToVerify2.act1("two")
        }
    }
}
