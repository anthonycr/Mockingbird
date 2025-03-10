package com.anthonycr.mockingbird.processor.internal.data

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * A wrapper for [KSClassDeclaration] that allows it to be safely used as a key in a map or set.
 *
 * @property ksClassDeclaration The underlying [KSClassDeclaration].
 * @property key The key derived from [ksClassDeclaration].
 */
data class KSClassDeclarationKey(
    val ksClassDeclaration: KSClassDeclaration
) {
    val key: String
        get() = ksClassDeclaration.qualifiedName!!.asString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KSClassDeclarationKey

        return key == other.key
    }

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = "KSClassDeclarationKey(key='$key')"
}
