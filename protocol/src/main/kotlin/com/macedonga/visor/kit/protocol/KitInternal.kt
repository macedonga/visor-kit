package com.macedonga.visor.kit.protocol

/**
 * Marks declarations that only Visor and the kit's own modules use: the constructors of the wire types, the JSON
 * setup, the server side. They carry no compatibility promise and can change in any release, so an app that uses
 * one fails to compile.
 */
@RequiresOptIn(
    message = "Internal to Visor and the kit. It can change in any release.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
)
public annotation class KitInternal
