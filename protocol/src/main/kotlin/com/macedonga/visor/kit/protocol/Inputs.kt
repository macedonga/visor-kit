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

        /**
         * The Neural Band, as the Display's navigation: its moves, selects and drags. Its every gesture is on
         * [Topics.BAND_GESTURES].
         */
        public val BAND: InputSource = InputSource("band")
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

        // The Display's navigation, from the touchpad or the band: what DAT calls Nav, Select, Back and Drag.
        public val NAV_UP: InputAction = InputAction("nav_up")
        public val NAV_DOWN: InputAction = InputAction("nav_down")
        public val NAV_LEFT: InputAction = InputAction("nav_left")
        public val NAV_RIGHT: InputAction = InputAction("nav_right")
        public val SELECT: InputAction = InputAction("select")
        public val BACK: InputAction = InputAction("back")

        /** A drag began, moved and ended; [GlassesInput.x] and the others say where. */
        public val DRAG_START: InputAction = InputAction("drag_start")
        public val DRAG_MOVE: InputAction = InputAction("drag_move")
        public val DRAG_END: InputAction = InputAction("drag_end")
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
    /** A drag's position and its move since the last event, in the glasses' own units, as DAT gives them. */
    public val x: Double? = null,
    public val y: Double? = null,
    public val dx: Double? = null,
    public val dy: Double? = null,
)

/** The params of [Topics.GLASSES_INPUTS]. Build one with [InputsRequest]. */
@Poko
@Serializable
public class InputsRequest @KitInternal constructor(
    /** The sources to hear; null for all of them. */
    public val sources: List<InputSource>? = null,
    /**
     * True to keep the wearer's back for the app, as DAT's consumeBack: it comes here as [InputAction.BACK] and the
     * Display stays on the app's screen. Only the Display can; on other glasses the glasses still act on it.
     */
    public val keepBack: Boolean = false,
) {
    /** Sets an [InputsRequest]'s fields. */
    public class Builder {
        public var sources: List<InputSource>? = null
        public var keepBack: Boolean = false

        public fun build(): InputsRequest = InputsRequest(sources, keepBack)
    }
}

/** An [InputsRequest], for example `InputsRequest { sources = listOf(InputSource.BAND) }`. */
public fun InputsRequest(block: InputsRequest.Builder.() -> Unit): InputsRequest = InputsRequest.Builder().apply(block).build()
