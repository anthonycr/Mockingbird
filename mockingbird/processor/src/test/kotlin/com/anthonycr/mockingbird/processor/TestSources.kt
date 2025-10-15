package com.anthonycr.mockingbird.processor

import com.tschuchort.compiletesting.SourceFile

val nonPropertyAnnotatedSource = SourceFile.kotlin(
    "Test1.kt", """
    package com.anthonycr.test

    import com.anthonycr.mockingbird.core.Verify

    class Test1 {
        @Verify
        fun myTest() {
        
        }
    }
""".trimIndent()
)

val nonInterfaceAnnotatedSource = SourceFile.kotlin("Test2.kt", """
    package com.anthonycr.test

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
    package com.anthonycr.test

    import com.anthonycr.mockingbird.core.Verify
    
    interface AnInterface {
        fun aFunction()
    }

    class Test3 {
        @Verify
        lateinit var anInterface: AnInterface
    }
""".trimIndent())

val validInterfaceAnnotatedImmutableProperty = SourceFile.kotlin("Test4.kt", """
    package com.anthonycr.test

    import com.anthonycr.mockingbird.core.fake
    import com.anthonycr.mockingbird.core.Verify
    
    interface AnInterface {
        fun aFunction()
    }

    class Test3 {
        @Verify
        private val anInterface: AnInterface = fake()
    }
""".trimIndent())

val validFunctionReferenceAnnotatedSource = SourceFile.kotlin("Test5.kt", """
    package com.anthonycr.test

    import com.anthonycr.mockingbird.core.fake
    import com.anthonycr.mockingbird.core.Verify
    
    interface AnInterface {
        fun aFunction()
    }

    class Test3 {
        @Verify
        private val aLambda: (String) -> Unit = fake()
    }
""".trimIndent())

val validAbstractClassOneFunction = SourceFile.kotlin("Test6.kt", """
    package com.anthonycr.test

    import com.anthonycr.mockingbird.core.fake
    import com.anthonycr.mockingbird.core.Verify
    
    abstract class AbstractClass {
        abstract fun aFunction()
    }

    class Test3 {
        @Verify
        private val abstractClass = fake<AbstractClass>()
    }
""".trimIndent())

val validAbstractClassRealAndAbstractFunction = SourceFile.kotlin("Test7.kt", """
    package com.anthonycr.test

    import com.anthonycr.mockingbird.core.fake
    import com.anthonycr.mockingbird.core.Verify
    
    abstract class AbstractClass {
        abstract fun aFunction()
        
        fun aRealFunction() = aFunction()
    }

    class Test3 {
        @Verify
        private val abstractClass = fake<AbstractClass>()
    }
""".trimIndent())

val invalidAbstractClassWithConstructorParameters = SourceFile.kotlin("Test8.kt", """
    package com.anthonycr.test

    import com.anthonycr.mockingbird.core.fake
    import com.anthonycr.mockingbird.core.Verify
    
    abstract class AbstractClass(val parameter: String) {
        abstract fun aFunction()
    }

    class Test3 {
        @Verify
        private val abstractClass = fake<AbstractClass>()
    }
""".trimIndent())

val packagePrivateJavaSrc = listOf(
    SourceFile.java(
        "FeatureAnalytics.java", """
    package com.anthonycr.test.feature;
    
    interface FeatureAnalytics {
        void trackEvent(String event);
    }
""".trimIndent()
    ), SourceFile.kotlin(
        "Test9.kt", """
        package com.anthonycr.test
        
        import com.anthonycr.mockingbird.core.fake
        import com.anthonycr.mockingbird.core.Verify
        import com.anthonycr.test.feature.FeatureAnalytics
        
        class Test {
        
            @Verify
            private val featureAnalytics = fake<FeatureAnalytics>()
        }
    """.trimIndent()
    )
)

val fakedInternalClassCarriesOverModifier = listOf(
    SourceFile.kotlin(
        "FeatureAnalytics.kt", """
    package com.anthonycr.test.feature
    
    internal interface FeatureAnalytics {
         fun trackEvent(event: String)
    }
""".trimIndent()
    ), SourceFile.kotlin(
        "Test9.kt", """
        package com.anthonycr.test
        
        import com.anthonycr.mockingbird.core.fake
        import com.anthonycr.mockingbird.core.Verify
        import com.anthonycr.test.feature.FeatureAnalytics
        
        internal class Test {
        
            @Verify
            private val featureAnalytics = fake<FeatureAnalytics>()
        }
    """.trimIndent()
    )
)
