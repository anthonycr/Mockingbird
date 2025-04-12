package com.anthonycr.mockingbird.sample

abstract class ClassToVerify2 {
    abstract fun act1(one: String)

    fun act2(two: String) {
        act1(one = two)
    }
}
