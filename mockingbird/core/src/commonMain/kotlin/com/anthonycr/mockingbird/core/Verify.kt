package com.anthonycr.mockingbird.core

/**
 * Annotate the property for which you wish to create a fake verification implementation.
 * Mockingbird will generate a fake implementation for the annotated property. Only interfaces and
 * abstract classes with zero argument constructors are supported.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Verify
