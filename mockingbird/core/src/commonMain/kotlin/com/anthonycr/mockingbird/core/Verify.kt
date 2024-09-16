package com.anthonycr.mockingbird.core

/**
 * Annotate the property for which you wish to create a fake verification implementation.
 * Mockingbird will generate a fake implementation for the annotated property. Only interfaces are
 * supported.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Verify
