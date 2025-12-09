package com.anthonycr.mockingbird.sample

import com.anthonycr.mockingbird.core.fake
import com.anthonycr.mockingbird.core.verify
import com.anthonycr.mockingbird.core.verifyComplete
import com.anthonycr.mockingbird.core.verifyPartial
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ClassToTestTest {

    private val nonFake = ""

    private val lambdaToVerify1: (String) -> Unit = fake()

    private val lambdaToVerify2: (Int) -> Unit = fake()

    private val interfaceToVerify1 = fake<InterfaceToVerify1>()

    private val interfaceToVerify2 = fake(InterfaceToVerify2::class.java)

    private val classToVerify1 = fake<ClassToVerify1>()

    private val classToVerify2 = fake<ClassToVerify2>()

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

    @Test
    fun `act1 expected failure`() {
        assertThrows<IllegalStateException>("expected: < ARGUMENT 0: 2> but was: < ARGUMENT 0: 1>") {
            val classToTest = createClassToTest()

            classToTest.act1()

            verify(lambdaToVerify1, lambdaToVerify2, interfaceToVerify1, interfaceToVerify2) {
                lambdaToVerify1("test")
                // The first invocation has 1 as its parameter
                interfaceToVerify1.performAction1(2)
            }
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

    @Test
    fun `act2 expected failure`() {
        assertThrows<IllegalStateException>("expected: < FUNCTION_CALL: com.anthonycr.mockingbird.sample.InterfaceToVerify1.performAction1> but was: < FUNCTION_CALL: com.anthonycr.mockingbird.sample.InterfaceToVerify1.performAction2>") {
            val classToTest = createClassToTest()

            classToTest.act2()

            verify(interfaceToVerify1, interfaceToVerify2) {
                // performAction2 was invoked first
                interfaceToVerify1.performAction1(1)
            }
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

    @Test
    fun `act3  expected failure`() {
        assertThrows<IllegalStateException>("Expected an invocation, but got none instead") {
            val classToTest = createClassToTest()

            classToTest.act3()

            verify(interfaceToVerify1, interfaceToVerify2) {
                // Only invoked 2 times
                repeat(3) {
                    interfaceToVerify1.performAction1(1)
                }
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

    @Test
    fun `act3 expected failure`() {
        assertThrows<IllegalStateException>("Expected no invocations, but found 2 unverified") {
            val classToTest = createClassToTest()

            classToTest.act3()

            // act3 function call actually has invocations
            interfaceToVerify1.verifyComplete()
        }
    }

    @Test
    fun `act3 expected failure unverified`() {
        assertThrows<IllegalStateException>("Found 1 unverified invocations") {
            val classToTest = createClassToTest()

            classToTest.act3()

            verify(interfaceToVerify1, interfaceToVerify2) {
                interfaceToVerify1.performAction1(1)
                interfaceToVerify1.performAction1(1)
                interfaceToVerify2.performAction1(1, "two", "three")
            }
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

    @Test
    fun `double verify expected failure`() {
        assertThrows<IllegalStateException>("Do not call verify within another verify block") {
            val classToTest = createClassToTest()

            classToTest.act3()

            verify(interfaceToVerify1) {
                verify(interfaceToVerify1) {
                    // Won't get here
                }
            }
        }
    }

    @Test
    fun `verifyNoInvocations within verify expected failure`() {
        assertThrows<IllegalStateException>("Do not call verifyComplete from within a verify block") {
            val classToTest = createClassToTest()

            classToTest.act3()

            verify(interfaceToVerify1) {
                interfaceToVerify1.verifyComplete()
            }
        }
    }

    @Test
    fun `verification of inequitable type expected failure`() {
        assertThrows<IllegalStateException>("expected: < ARGUMENT 0: java.lang.Exception: test> but was: < ARGUMENT 0: java.lang.Exception: test>") {
            val classToTest = createClassToTest()

            classToTest.act5()

            verify(interfaceToVerify1) {
                interfaceToVerify1.performAction3(Exception("test"))
            }
        }
    }

    @Test
    fun `verification of inequitable type with parameter verification`() {
        val classToTest = createClassToTest()

        classToTest.act5()

        verify(interfaceToVerify1) {
            interfaceToVerify1.performAction3(sameAs { a -> a.message == "test" })
        }
    }

    @Test
    fun `verification of inequitable type with parameter verification expected failure`() {
        assertThrows<IllegalStateException>("[sameAs] matcher for <ARGUMENT 0> rejects value: java.lang.Exception: test") {
            val classToTest = createClassToTest()

            classToTest.act5()

            verify(interfaceToVerify1) {
                interfaceToVerify1.performAction3(sameAs { a -> a.message == "test1" })
            }
        }
    }

    @Test
    fun `verification of multiple types with parameter verification`() {
        val classToTest = createClassToTest()

        classToTest.act6()

        verify(interfaceToVerify1) {
            interfaceToVerify1.performAction4(
                one = "one",
                two = any(),
                exception = sameAs { a -> a.message == "test" }
            )
        }
    }

    @Test
    fun `verification of multiple types with parameter verification expected failure`() {
        assertThrows<IllegalStateException>("expected: < ARGUMENT 1: 1> but was: < ARGUMENT 1: 2>") {
            val classToTest = createClassToTest()

            classToTest.act6()

            verify(interfaceToVerify1) {
                interfaceToVerify1.performAction4(
                    one = "one",
                    two = 1,
                    exception = sameAs { a -> a.message == "test" }
                )
            }
        }
    }

    @Test
    fun `verification of non Unit returns is not allowed`() {
        assertThrows<IllegalStateException>("Only functions with return type Unit can be verified") {
            val classToTest = createClassToTest()

            classToTest.act7()
        }
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
            interfaceToVerify2.performAction4("one")
        }
    }

    @Test
    fun `verification of non Unit returns suspending function is not allowed`() {
        assertThrows<IllegalStateException>("Only functions with return type Unit can be verified") {
            runTest {
                val classToTest = createClassToTest()

                classToTest.act9()
            }
        }
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

    @Test
    fun `verification of non faked class is not allowed`() {
        assertThrows<IllegalStateException>("You can only verify interfaces that have been created by fake()") {
            verify(nonFake) {
                nonFake.chars()
            }
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
