package com.macedonga.visor.kit.protocol

import kotlinx.serialization.Serializable

/**
 * An icon the Display draws from its own set, by name: the names Meta's DAT has. A Visor that does not know a name
 * leaves the icon out.
 */
@Serializable
@JvmInline
public value class LayoutIconName(public val id: String) {
    override fun toString(): String = id

    public companion object {
        public val AIRPLANE: LayoutIconName = LayoutIconName("airplane")
        public val ARROW_DOWN_SHALLOW_U: LayoutIconName = LayoutIconName("arrow_down_shallow_u")
        public val ARROW_LEFT: LayoutIconName = LayoutIconName("arrow_left")
        public val ARROW_RIGHT: LayoutIconName = LayoutIconName("arrow_right")
        public val ARROW_U_LEFT: LayoutIconName = LayoutIconName("arrow_u_left")
        public val ARROW_UP_SHALLOW_U: LayoutIconName = LayoutIconName("arrow_up_shallow_u")
        public val AVATAR: LayoutIconName = LayoutIconName("avatar")
        public val AVATAR_OFF: LayoutIconName = LayoutIconName("avatar_off")
        public val BED_SIDE: LayoutIconName = LayoutIconName("bed_side")
        public val BELL: LayoutIconName = LayoutIconName("bell")
        public val BELL_DIAGONAL_RIGHT_DOT: LayoutIconName = LayoutIconName("bell_diagonal_right_dot")
        public val BELL_OFF: LayoutIconName = LayoutIconName("bell_off")
        public val BIKE_SHARE: LayoutIconName = LayoutIconName("bike_share")
        public val BUG: LayoutIconName = LayoutIconName("bug")
        public val BULLHORN: LayoutIconName = LayoutIconName("bullhorn")
        public val BUS: LayoutIconName = LayoutIconName("bus")
        public val CALENDAR: LayoutIconName = LayoutIconName("calendar")
        public val CAMPFIRE: LayoutIconName = LayoutIconName("campfire")
        public val CARET_DOWN: LayoutIconName = LayoutIconName("caret_down")
        public val CARET_LEFT: LayoutIconName = LayoutIconName("caret_left")
        public val CARET_RIGHT: LayoutIconName = LayoutIconName("caret_right")
        public val CARET_UP: LayoutIconName = LayoutIconName("caret_up")
        public val CAR_FRONT_VIEW: LayoutIconName = LayoutIconName("car_front_view")
        public val CART: LayoutIconName = LayoutIconName("cart")
        public val CHECKMARK: LayoutIconName = LayoutIconName("checkmark")
        public val CHECKMARK_CIRCLE: LayoutIconName = LayoutIconName("checkmark_circle")
        public val CIRCLE_8_RAYS_LARGE: LayoutIconName = LayoutIconName("circle_8_rays_large")
        public val CIRCLE_HANDLE: LayoutIconName = LayoutIconName("circle_handle")
        public val CLOCK: LayoutIconName = LayoutIconName("clock")
        public val CLOUD: LayoutIconName = LayoutIconName("cloud")
        public val CLOUD_CRESCENT_MOON: LayoutIconName = LayoutIconName("cloud_crescent_moon")
        public val CLOUD_DOT_FOUR_RAYS: LayoutIconName = LayoutIconName("cloud_dot_four_rays")
        public val CLOUD_FIVE_DASHES: LayoutIconName = LayoutIconName("cloud_five_dashes")
        public val CLOUD_HOOK_SWIRL: LayoutIconName = LayoutIconName("cloud_hook_swirl")
        public val CLOUD_LIGHTNING: LayoutIconName = LayoutIconName("cloud_lightning")
        public val COCKTAIL_GLASS: LayoutIconName = LayoutIconName("cocktail_glass")
        public val CODE: LayoutIconName = LayoutIconName("code")
        public val COFFEE_CUP: LayoutIconName = LayoutIconName("coffee_cup")
        public val COMPASS_NORTH_UP_RED: LayoutIconName = LayoutIconName("compass_north_up_red")
        public val CONTAINER_WITH_LID: LayoutIconName = LayoutIconName("container_with_lid")
        public val CROSS_BRIEFCASE: LayoutIconName = LayoutIconName("cross_briefcase")
        public val DROPPER: LayoutIconName = LayoutIconName("dropper")
        public val ENVELOPE_OPEN: LayoutIconName = LayoutIconName("envelope_open")
        public val EXCLAMATION_CIRCLE: LayoutIconName = LayoutIconName("exclamation_circle")
        public val EXCLAMATION_TRIANGLE: LayoutIconName = LayoutIconName("exclamation_triangle")
        public val EYE: LayoutIconName = LayoutIconName("eye")
        public val FORK_KNIFE: LayoutIconName = LayoutIconName("fork_knife")
        public val FOUR_ARCS_UP_FILLED: LayoutIconName = LayoutIconName("four_arcs_up_filled")
        public val FOUR_ARCS_UP_GRAYSCALE: LayoutIconName = LayoutIconName("four_arcs_up_grayscale")
        public val FOUR_CORNER_FRAME: LayoutIconName = LayoutIconName("four_corner_frame")
        public val GEAR: LayoutIconName = LayoutIconName("gear")
        public val GLOBE_WESTERN_HEMISPHERE: LayoutIconName = LayoutIconName("globe_western_hemisphere")
        public val GRADUATION_CAP: LayoutIconName = LayoutIconName("graduation_cap")
        public val HASHTAG: LayoutIconName = LayoutIconName("hashtag")
        public val HEADPHONES: LayoutIconName = LayoutIconName("headphones")
        public val HEART: LayoutIconName = LayoutIconName("heart")
        public val HOUSE: LayoutIconName = LayoutIconName("house")
        public val I_CIRCLE: LayoutIconName = LayoutIconName("i_circle")
        public val LIGHT_BULB: LayoutIconName = LayoutIconName("light_bulb")
        public val MAGIC_WAND: LayoutIconName = LayoutIconName("magic_wand")
        public val META_AI: LayoutIconName = LayoutIconName("meta_ai")
        public val MOUNTAIN_SQUARE: LayoutIconName = LayoutIconName("mountain_square")
        public val MOUNTAIN_SQUARE_STACKED: LayoutIconName = LayoutIconName("mountain_square_stacked")
        public val MUSEUM_BUILDING: LayoutIconName = LayoutIconName("museum_building")
        public val MUSIC_NOTE: LayoutIconName = LayoutIconName("music_note")
        public val NINE_SQUARES_GRID: LayoutIconName = LayoutIconName("nine_squares_grid")
        public val PADLOCK_CLOSED: LayoutIconName = LayoutIconName("padlock_closed")
        public val PADLOCK_OPEN: LayoutIconName = LayoutIconName("padlock_open")
        public val PALETTE: LayoutIconName = LayoutIconName("palette")
        public val PAPER_AIRPLANE: LayoutIconName = LayoutIconName("paper_airplane")
        public val PENCIL: LayoutIconName = LayoutIconName("pencil")
        public val PENCIL_SQUARE: LayoutIconName = LayoutIconName("pencil_square")
        public val PERSON: LayoutIconName = LayoutIconName("person")
        public val PERSON_CIRCLE: LayoutIconName = LayoutIconName("person_circle")
        public val PHONE: LayoutIconName = LayoutIconName("phone")
        public val PHONE_HANDSET_ARROW_DOWN_LEFT: LayoutIconName = LayoutIconName("phone_handset_arrow_down_left")
        public val PHONE_HANDSET_ARROW_UP_RIGHT: LayoutIconName = LayoutIconName("phone_handset_arrow_up_right")
        public val PHONE_SLASH: LayoutIconName = LayoutIconName("phone_slash")
        public val PIZZA_SLICE: LayoutIconName = LayoutIconName("pizza_slice")
        public val PLUS: LayoutIconName = LayoutIconName("plus")
        public val PLUS_CIRCLE: LayoutIconName = LayoutIconName("plus_circle")
        public val SHOPPING_BAG: LayoutIconName = LayoutIconName("shopping_bag")
        public val SLIDERS_HORIZONTAL: LayoutIconName = LayoutIconName("sliders_horizontal")
        public val SMART_GLASSES: LayoutIconName = LayoutIconName("smart_glasses")
        public val SMILEY_CIRCLE: LayoutIconName = LayoutIconName("smiley_circle")
        public val SPEAKER_OFF: LayoutIconName = LayoutIconName("speaker_off")
        public val SPEAKER_WITH_ONE_ARC: LayoutIconName = LayoutIconName("speaker_with_one_arc")
        public val SPEAKER_WITH_THREE_ARCS: LayoutIconName = LayoutIconName("speaker_with_three_arcs")
        public val SPEAKER_WITH_TWO_ARCS: LayoutIconName = LayoutIconName("speaker_with_two_arcs")
        public val SPEECH_BUBBLE: LayoutIconName = LayoutIconName("speech_bubble")
        public val SPEECH_BUBBLE_OFF: LayoutIconName = LayoutIconName("speech_bubble_off")
        public val STADIUM: LayoutIconName = LayoutIconName("stadium")
        public val STAR: LayoutIconName = LayoutIconName("star")
        public val STAR_CIRCLE_TRIANGLE_AI: LayoutIconName = LayoutIconName("star_circle_triangle_ai")
        public val TAXI: LayoutIconName = LayoutIconName("taxi")
        public val THREE_DOT_SPEECH_BUBBLE: LayoutIconName = LayoutIconName("three_dot_speech_bubble")
        public val THREE_DOTS_HORIZONTAL: LayoutIconName = LayoutIconName("three_dots_horizontal")
        public val THREE_HORIZONTAL_LINES: LayoutIconName = LayoutIconName("three_horizontal_lines")
        public val THREE_HORIZONTAL_LINES_STACKED_DESCENDING: LayoutIconName = LayoutIconName("three_horizontal_lines_stacked_descending")
        public val THREE_PEOPLE_OVERLAPPING: LayoutIconName = LayoutIconName("three_people_overlapping")
        public val TRAIN: LayoutIconName = LayoutIconName("train")
        public val TREE: LayoutIconName = LayoutIconName("tree")
        public val TRIANGLE_LEFT_VERTICAL_LINE: LayoutIconName = LayoutIconName("triangle_left_vertical_line")
        public val TRIANGLE_RIGHT: LayoutIconName = LayoutIconName("triangle_right")
        public val TRIANGLE_RIGHT_CIRCLE: LayoutIconName = LayoutIconName("triangle_right_circle")
        public val TRIANGLE_RIGHT_VERTICAL_LINE: LayoutIconName = LayoutIconName("triangle_right_vertical_line")
        public val TWO_ARROWS_CLOCKWISE: LayoutIconName = LayoutIconName("two_arrows_clockwise")
        public val TWO_LINES_PARALLEL: LayoutIconName = LayoutIconName("two_lines_parallel")
        public val TWO_SQUARES_STACKED_RIGHT_DOWN: LayoutIconName = LayoutIconName("two_squares_stacked_right_down")
        public val TWO_TRIANGLES_LEFT: LayoutIconName = LayoutIconName("two_triangles_left")
        public val TWO_TRIANGLES_RIGHT: LayoutIconName = LayoutIconName("two_triangles_right")
        public val VIDEO_CAMERA: LayoutIconName = LayoutIconName("video_camera")
        public val VIDEO_CAMERA_OFF: LayoutIconName = LayoutIconName("video_camera_off")
        public val WRISTBAND: LayoutIconName = LayoutIconName("wristband")
        public val WRISTBAND_SLASH: LayoutIconName = LayoutIconName("wristband_slash")
        public val X: LayoutIconName = LayoutIconName("x")

        /** Every name above, in order. */
        public val all: List<LayoutIconName> = listOf(
            AIRPLANE, ARROW_DOWN_SHALLOW_U, ARROW_LEFT, ARROW_RIGHT, ARROW_U_LEFT, ARROW_UP_SHALLOW_U, AVATAR,
            AVATAR_OFF, BED_SIDE, BELL, BELL_DIAGONAL_RIGHT_DOT, BELL_OFF, BIKE_SHARE, BUG, BULLHORN, BUS, CALENDAR,
            CAMPFIRE, CARET_DOWN, CARET_LEFT, CARET_RIGHT, CARET_UP, CAR_FRONT_VIEW, CART, CHECKMARK,
            CHECKMARK_CIRCLE, CIRCLE_8_RAYS_LARGE, CIRCLE_HANDLE, CLOCK, CLOUD, CLOUD_CRESCENT_MOON,
            CLOUD_DOT_FOUR_RAYS, CLOUD_FIVE_DASHES, CLOUD_HOOK_SWIRL, CLOUD_LIGHTNING, COCKTAIL_GLASS, CODE,
            COFFEE_CUP, COMPASS_NORTH_UP_RED, CONTAINER_WITH_LID, CROSS_BRIEFCASE, DROPPER, ENVELOPE_OPEN,
            EXCLAMATION_CIRCLE, EXCLAMATION_TRIANGLE, EYE, FORK_KNIFE, FOUR_ARCS_UP_FILLED, FOUR_ARCS_UP_GRAYSCALE,
            FOUR_CORNER_FRAME, GEAR, GLOBE_WESTERN_HEMISPHERE, GRADUATION_CAP, HASHTAG, HEADPHONES, HEART, HOUSE,
            I_CIRCLE, LIGHT_BULB, MAGIC_WAND, META_AI, MOUNTAIN_SQUARE, MOUNTAIN_SQUARE_STACKED, MUSEUM_BUILDING,
            MUSIC_NOTE, NINE_SQUARES_GRID, PADLOCK_CLOSED, PADLOCK_OPEN, PALETTE, PAPER_AIRPLANE, PENCIL,
            PENCIL_SQUARE, PERSON, PERSON_CIRCLE, PHONE, PHONE_HANDSET_ARROW_DOWN_LEFT, PHONE_HANDSET_ARROW_UP_RIGHT,
            PHONE_SLASH, PIZZA_SLICE, PLUS, PLUS_CIRCLE, SHOPPING_BAG, SLIDERS_HORIZONTAL, SMART_GLASSES,
            SMILEY_CIRCLE, SPEAKER_OFF, SPEAKER_WITH_ONE_ARC, SPEAKER_WITH_THREE_ARCS, SPEAKER_WITH_TWO_ARCS,
            SPEECH_BUBBLE, SPEECH_BUBBLE_OFF, STADIUM, STAR, STAR_CIRCLE_TRIANGLE_AI, TAXI, THREE_DOT_SPEECH_BUBBLE,
            THREE_DOTS_HORIZONTAL, THREE_HORIZONTAL_LINES, THREE_HORIZONTAL_LINES_STACKED_DESCENDING,
            THREE_PEOPLE_OVERLAPPING, TRAIN, TREE, TRIANGLE_LEFT_VERTICAL_LINE, TRIANGLE_RIGHT,
            TRIANGLE_RIGHT_CIRCLE, TRIANGLE_RIGHT_VERTICAL_LINE, TWO_ARROWS_CLOCKWISE, TWO_LINES_PARALLEL,
            TWO_SQUARES_STACKED_RIGHT_DOWN, TWO_TRIANGLES_LEFT, TWO_TRIANGLES_RIGHT, VIDEO_CAMERA, VIDEO_CAMERA_OFF,
            WRISTBAND, WRISTBAND_SLASH, X,
        )
    }
}
