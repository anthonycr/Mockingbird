package com.anthonycr.benchmark

import kotlin.Exception

class Benchmark(
    private val interface1: Interface1,
    private val interface2: Interface2,
    private val interface3: Interface3,
    private val interface4: Interface4,
    private val interface5: Interface5,
    private val interface6: Interface6,
    private val interface7: Interface7,
    private val interface8: Interface8,
    private val interface9: Interface9,
    private val interface10: Interface10
) {

    fun benchmark() {
        interface1.doThing()
        interface2.doThing1()
        interface2.doThing2()
        interface3.doThing1("test1")
        interface3.doThing2(1)
        interface3.doThing3(1)
        interface4.doThing1("test1", "test2")
        interface4.doThing2(1, true)
        interface5.doThing("test1", 1, true)
        interface6.doThing("test1", true, 1, 1)
        interface7.doThing(Exception("test1"))
        interface8.doThing(Model1("test1", 1, true))
        interface9.doThing(
            Model1("test1", 1, true),
            Model1("test2", 2, false)
        )
        interface10.doThing(
            Model1("test1", 1, true),
            Model1("test2", 2, false),
            Model1("test3", 3, true)
        )
    }

}

data class Model1(
    val value1: String,
    val value2: Int,
    val value3: Boolean
)

interface Interface1 {

    fun doThing()

}

interface Interface2 {

    fun doThing1()


    fun doThing2()

}

interface Interface3 {

    fun doThing1(arg: String)


    fun doThing2(arg: Int)


    fun doThing3(arg: Long)

}

interface Interface4 {

    fun doThing1(arg1: String, arg2: String)


    fun doThing2(arg1: Int, arg2: Boolean)

}

interface Interface5 {

    fun doThing(arg1: String, arg2: Int, arg3: Boolean)

}

interface Interface6 {

    fun doThing(arg1: String, arg2: Boolean, arg3: Int, arg4: Long)

}

interface Interface7 {

    fun doThing(arg: Exception)

}

interface Interface8 {

    fun doThing(arg: Model1)

}

interface Interface9 {

    fun doThing(arg1: Model1, arg2: Model1)

}

interface Interface10 {

    fun doThing(arg1: Model1, arg2: Model1, arg3: Model1)

}
