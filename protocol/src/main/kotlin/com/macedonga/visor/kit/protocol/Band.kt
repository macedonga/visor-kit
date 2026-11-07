package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** The Neural Band as Visor knows it now. */
@Poko
@Serializable
public class BandStatus @KitInternal constructor(
    /** False when no band is paired with the glasses. Every other field is then empty. */
    public val paired: Boolean,
    /** The phone's own link to the band, which is up only while an app or Visor borrows it. */
    public val link: Link,
    /** The battery, from 0 to 100. */
    public val battery: Int? = null,
    public val charging: Boolean? = null,
    /** True when the band is set for the left wrist. */
    public val leftHanded: Boolean? = null,
    /** True while the phone holds the band: its gestures come to the phone and the Display does not get them. */
    public val borrowed: Boolean = false,
)

/** A finger the band reads. */
@Serializable
@JvmInline
public value class Finger(public val id: String) {
    override fun toString(): String = id

    public companion object {
        public val THUMB: Finger = Finger("thumb")
        public val INDEX: Finger = Finger("index")
        public val MIDDLE: Finger = Finger("middle")
    }
}

/** What a finger did, as the band reports it, or what the band made of a series of such events. */
@Serializable
@JvmInline
public value class GestureAction(public val id: String) {
    override fun toString(): String = id

    public companion object {
        public val PRESS: GestureAction = GestureAction("press")
        public val RELEASE: GestureAction = GestureAction("release")
        public val TAP: GestureAction = GestureAction("tap")
        public val DOUBLE_TAP: GestureAction = GestureAction("double_tap")
        public val CLICK: GestureAction = GestureAction("click")
        public val HOLD: GestureAction = GestureAction("hold")

        /** A thumb swipe, by its direction. */
        public val UP: GestureAction = GestureAction("up")
        public val DOWN: GestureAction = GestureAction("down")
        public val LEFT: GestureAction = GestureAction("left")
        public val RIGHT: GestureAction = GestureAction("right")

        /** A value the band sent that Visor cannot name. */
        public val UNKNOWN: GestureAction = GestureAction("unknown")
    }
}

/** One gesture from the band. */
@Poko
@Serializable
public class Gesture @KitInternal constructor(
    public val finger: Finger,
    /** What the finger did. */
    public val action: GestureAction,
    /** What the band made of it, for example [GestureAction.DOUBLE_TAP] for the second of two taps. */
    public val derived: GestureAction? = null,
    /** When the phone got it, in milliseconds since 1970. */
    public val time: Long,
)

/** The params of [Methods.BAND_BUZZ]. */
@Poko
@Serializable
public class BuzzRequest @KitInternal constructor(
    /** From [MIN_DURATION_MS] to [MAX_DURATION_MS]. */
    public val durationMs: Int = DEFAULT_DURATION_MS,
    /** From 1 to [MAX_STRENGTH]. */
    public val strength: Int = MAX_STRENGTH,
) {
    public companion object {
        public const val MIN_DURATION_MS: Int = 10
        public const val MAX_DURATION_MS: Int = 1000

        /** Long enough that the wearer cannot miss it. The Display's own buzzes are 20 and 50 ms. */
        public const val DEFAULT_DURATION_MS: Int = 400
        public const val MAX_STRENGTH: Int = 255
    }
}
