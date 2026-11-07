package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** What the wearer touched or pressed on the glasses. */
@Serializable
@JvmInline
public value class InputSource(public val id: String) {
    override fun toString(): String = id

    public companion object {
        /** The touchpad on the right temple. */
        public val TOUCHPAD: InputSource = InputSource("touchpad")

        /** The capture button on top of the right temple. */
        public val CAPTURE_BUTTON: InputSource = InputSource("capture_button")

        /** The action button, on glasses that have one. */
        public val ACTION_BUTTON: InputSource = InputSource("action_button")
    }
}

/** What the wearer did, as the glasses report it. Each source has its own actions. */
@Serializable
@JvmInline
public value class InputAction(public val id: String) {
    override fun toString(): String = id

    public companion object {
        // The touchpad.
        public val TAP: InputAction = InputAction("tap")
        public val DOUBLE_TAP: InputAction = InputAction("double_tap")
        public val TRIPLE_TAP: InputAction = InputAction("triple_tap")
        public val TAP_HOLD: InputAction = InputAction("tap_hold")

        /** A swipe towards the front of the glasses. */
        public val SWIPE_FORWARD: InputAction = InputAction("swipe_forward")
        public val SWIPE_BACKWARD: InputAction = InputAction("swipe_backward")

        /** A finger kept on the touchpad, moving towards the front. */
        public val SLIDE_FORWARD: InputAction = InputAction("slide_forward")
        public val SLIDE_BACKWARD: InputAction = InputAction("slide_backward")

        /** A finger came to rest on the touchpad, and [RELEASE] once it left. */
        public val TOUCH: InputAction = InputAction("touch")
        public val PALM_TAP: InputAction = InputAction("palm_tap")
        public val PALM_HOLD: InputAction = InputAction("palm_hold")

        // The buttons.
        public val PRESS: InputAction = InputAction("press")

        /** A button let go of, or the finger off the touchpad. */
        public val RELEASE: InputAction = InputAction("release")
        public val LONG_PRESS: InputAction = InputAction("long_press")
        public val VERY_LONG_PRESS: InputAction = InputAction("very_long_press")
        public val DOUBLE_PRESS: InputAction = InputAction("double_press")

        /** The action button let go of after a long press. */
        public val LONG_RELEASE: InputAction = InputAction("long_release")
    }
}

/** One touch or press, from [Topics.GLASSES_INPUTS]. */
@Poko
@Serializable
public class GlassesInput @KitInternal constructor(
    public val source: InputSource,
    public val action: InputAction,
    /** When Visor heard it, in milliseconds since 1970. */
    public val time: Long,
)
