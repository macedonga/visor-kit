package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** Where the phone's link to a device stands. */
@Serializable
@JvmInline
public value class Link(public val id: String) {
    override fun toString(): String = id

    public companion object {
        public val DISCONNECTED: Link = Link("disconnected")

        /** Reaching the device, setting up the link, or proving who the phone is. */
        public val CONNECTING: Link = Link("connecting")

        /** Ready for requests. */
        public val CONNECTED: Link = Link("connected")
    }
}

/** The glasses as Visor knows them now. */
@Poko
@Serializable
public class GlassesStatus @KitInternal constructor(
    /** False when Visor has no glasses set up. Every other field is then empty. */
    public val paired: Boolean,
    public val link: Link,
    /** The glasses' own model name, to show to people. Code tells models apart by [display], never by this. */
    public val model: String? = null,
    /** The name the person gave the pair, in Visor or in the Meta app. */
    public val name: String? = null,
    public val serial: String? = null,
    /** The glasses' firmware build. */
    public val firmware: String? = null,
    /** True for glasses with a display. Known once the glasses connected at least once. */
    public val display: Boolean? = null,
    /** The battery, from 0 to 100. */
    public val battery: Int? = null,
    public val charging: Boolean? = null,
    /** True while the glasses sit on a face. */
    public val worn: Boolean? = null,
    /** True while the temples are folded. */
    public val folded: Boolean? = null,
    /** True while the glasses record a video. */
    public val recording: Boolean? = null,
    /** The charging case's battery, from 0 to 100, while the glasses sit in the case. */
    public val caseBattery: Int? = null,
    /** How hot the glasses run, by their own measure. */
    public val thermal: ThermalLevel? = null,
    /** True while the glasses slow themselves down to cool off or to save power. */
    public val throttling: Boolean? = null,
)

/** How hot the glasses run, by their own measure, from [NORMAL] up. */
@Serializable
@JvmInline
public value class ThermalLevel(public val id: String) {
    override fun toString(): String = id

    public companion object {
        public val NORMAL: ThermalLevel = ThermalLevel("normal")
        public val LIGHT: ThermalLevel = ThermalLevel("light")
        public val MODERATE: ThermalLevel = ThermalLevel("moderate")
        public val SEVERE: ThermalLevel = ThermalLevel("severe")
        public val CRITICAL: ThermalLevel = ThermalLevel("critical")

        /** About to switch off to cool down. */
        public val SHUTDOWN: ThermalLevel = ThermalLevel("shutdown")
    }
}
