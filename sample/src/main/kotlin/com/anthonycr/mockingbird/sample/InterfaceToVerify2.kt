package com.anthonycr.mockingbird.sample

interface InterfaceToVerify2 {

    fun performAction1(one: Long, two: String, three: String)

    fun performAction2()

    fun cantPerformAction3(): Boolean

    suspend fun performAction4(string: String)

    suspend fun performAction5(): Boolean
}
