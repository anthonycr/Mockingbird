package com.anthonycr.mockingbird.sample

class ClassToTest(
    private val interfaceToVerify1: InterfaceToVerify1,
    private val interfaceToVerify2: InterfaceToVerify2
) {

    fun act1() {
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

}
