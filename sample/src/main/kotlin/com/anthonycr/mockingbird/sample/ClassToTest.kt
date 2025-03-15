package com.anthonycr.mockingbird.sample

class ClassToTest(
    private val lambdaToVerify1: (String) -> Unit,
    private val lambdaToVerify2: (Int) -> Unit,
    private val interfaceToVerify1: InterfaceToVerify1,
    private val interfaceToVerify2: InterfaceToVerify2,
    private val classToVerify1: ClassToVerify1,
    private val classToVerify2: ClassToVerify2,
) {

    fun act1() {
        lambdaToVerify1("test")
        lambdaToVerify2(1)
        interfaceToVerify1.performAction1(1)
        interfaceToVerify1.performAction1(2)
        interfaceToVerify2.performAction1(1, "two", "three")
    }

    fun act2() {
        interfaceToVerify1.performAction2("test", 1)
        interfaceToVerify1.performAction1(1)
        interfaceToVerify2.performAction2()
        interfaceToVerify2.performAction1(2, "three", "four")
    }

    fun act3() {
        interfaceToVerify1.performAction1(1)
        interfaceToVerify1.performAction1(1)
        interfaceToVerify2.performAction1(1, "two", "three")
        interfaceToVerify2.performAction1(1, "two", "three")
    }

    fun act4() {
        // Do nothing
    }

    fun act5() {
        interfaceToVerify1.performAction3(Exception("test"))
    }

    fun act6() {
        interfaceToVerify1.performAction4("one", 2, Exception("test"))
    }

    fun act7() {
        interfaceToVerify2.cantPerformAction3()
    }

    suspend fun act8() {
        interfaceToVerify2.performAction4("one")
    }

    suspend fun act9() {
        interfaceToVerify2.performAction5()
    }

    fun act10() {
        classToVerify1.act1()
    }

    fun act11() {
        classToVerify2.act1("one")
        classToVerify2.act2("two")
    }

}
