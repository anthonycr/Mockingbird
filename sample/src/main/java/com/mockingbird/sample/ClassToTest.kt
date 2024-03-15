package com.mockingbird.sample

class ClassToTest(
    private val interfaceToVerify: InterfaceToVerify
) {

    fun act1() {
        interfaceToVerify.performAction1(1)
        interfaceToVerify.performAction1(2)
    }

    fun act2() {
        interfaceToVerify.performAction1(1)
        interfaceToVerify.performAction2("test", 1)
    }

    fun act3() {
        interfaceToVerify.performAction1(1)
        interfaceToVerify.performAction1(1)
    }

    fun act4() {
        // Do nothing
    }

}
