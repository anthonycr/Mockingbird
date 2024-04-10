package com.anthonycr.benchmark

import io.mockk.mockk
import io.mockk.verifyAll
import org.junit.Test

class BenchmarkTest {

    private val interface1: Interface1 = mockk(relaxUnitFun = true)
    private val interface2: Interface2 = mockk(relaxUnitFun = true)
    private val interface3: Interface3 = mockk(relaxUnitFun = true)
    private val interface4: Interface4 = mockk(relaxUnitFun = true)
    private val interface5: Interface5 = mockk(relaxUnitFun = true)
    private val interface6: Interface6 = mockk(relaxUnitFun = true)
    private val interface7: Interface7 = mockk(relaxUnitFun = true)
    private val interface8: Interface8 = mockk(relaxUnitFun = true)
    private val interface9: Interface9 = mockk(relaxUnitFun = true)
    private val interface10: Interface10 = mockk(relaxUnitFun = true)

    @Test
    fun benchmark() {
        val benchmark = Benchmark(
            interface1,
            interface2,
            interface3,
            interface4,
            interface5,
            interface6,
            interface7,
            interface8,
            interface9,
            interface10
        )

        benchmark.benchmark()

        verifyAll {
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
            interface7.doThing(match { it.message == "test1" })
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
}
