package com.mockingbird.sample

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
        interfaceToVerify2.performAction1(1, "two", "three")
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

}
