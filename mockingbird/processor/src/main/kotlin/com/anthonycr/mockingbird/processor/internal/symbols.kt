package com.anthonycr.mockingbird.processor.internal

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration

/**
 * Normalize the qualified name of a declaration to accommodate functions having different package
 * names in KSP contexts than in JVM contexts.
 */
val KSClassDeclaration.normalizedQualifiedName: String
    get() = when (val qualifier = qualifiedName!!.getQualifier()) {
        "kotlin" -> "kotlin.jvm.functions"
        else -> qualifier
    } + ".${qualifiedName!!.getShortName()}"

/**
 * Return a safe package name that accounts for kotlin interfaces being faked and the inability to
 * use their package names as is.
 */
val KSClassDeclaration.safePackageName: String
    get() = packageName.takeIf { it.asString() != "kotlin" }?.asString() ?: "_kotlin"

/**
 * Return true if the declaration is of an interface, false otherwise.
 */
val KSDeclaration.isInterface: Boolean
    get() = this is KSClassDeclaration && this.classKind == ClassKind.INTERFACE
