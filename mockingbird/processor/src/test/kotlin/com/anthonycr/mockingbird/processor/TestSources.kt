package com.anthonycr.mockingbird.processor

import com.tschuchort.compiletesting.SourceFile

val nonPropertyAnnotatedSource = SourceFile.kotlin(
    "Test1.kt", """
    import com.anthonycr.mockingbird.core.Verify

    class Test1 {
        @Verify
        fun myTest() {
        
        }
    }
""".trimIndent()
)

val nonInterfaceAnnotatedSource = SourceFile.kotlin("Test2.kt", """
    import com.anthonycr.mockingbird.core.Verify

    class NotAnInterface {
        fun aFunction() {
        
        }
    }

    class Test2 {
        @Verify
        lateinit var notAnInterface: NotAnInterface
    }
""".trimIndent())

val validInterfaceAnnotatedSource = SourceFile.kotlin("Test3.kt", """
    import com.anthonycr.mockingbird.core.Verify
    
    interface AnInterface {
        fun aFunction()
    }

    class Test3 {
        @Verify
        lateinit var anInterface: AnInterface
    }
""".trimIndent())
