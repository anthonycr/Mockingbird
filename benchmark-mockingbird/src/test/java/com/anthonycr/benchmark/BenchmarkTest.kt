package com.anthonycr.benchmark

import com.anthonycr.mockingbird.core.Verify
import com.anthonycr.mockingbird.core.eq
import com.anthonycr.mockingbird.core.fake
import com.anthonycr.mockingbird.core.verify
import com.anthonycr.mockingbird.core.verifyParams
import org.junit.Test

class BenchmarkTest {

    @Verify
    private val interface1: Interface1 = fake()

    @Verify
    private val interface2: Interface2 = fake()

    @Verify
    private val interface3: Interface3 = fake()

    @Verify
    private val interface4: Interface4 = fake()

    @Verify
    private val interface5: Interface5 = fake()

    @Verify
    private val interface6: Interface6 = fake()

    @Verify
    private val interface7: Interface7 = fake()

    @Verify
    private val interface8: Interface8 = fake()

    @Verify
    private val interface9: Interface9 = fake()

    @Verify
    private val interface10: Interface10 = fake()

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

        verify(
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
        ) {
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
            interface7.verifyParams(
                func = Interface7::doThing,
                p0 = eq(Exception("test1")) { it.message == "test1" }
            )
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
