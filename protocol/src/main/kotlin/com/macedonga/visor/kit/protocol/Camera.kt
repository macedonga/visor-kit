package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** What a capture holds. */
@Serializable
@JvmInline
public value class CaptureType(public val id: String) {
    override fun toString(): String = id

    public companion object {
        public val PHOTO: CaptureType = CaptureType("photo")

        /** A photo with the short clip around it. */
        public val LIVE: CaptureType = CaptureType("live")
        public val VIDEO: CaptureType = CaptureType("video")
    }
}

/** How the glasses film. */
@Serializable
@JvmInline
public value class VideoKind(public val id: String) {
    override fun toString(): String = id

    public companion object {
        public val VIDEO: VideoKind = VideoKind("video")
        public val HYPERLAPSE: VideoKind = VideoKind("hyperlapse")
    }
}

/** A photo or a video, on the glasses, in the phone's gallery, or both. */
@Poko
@Serializable
public class Capture @KitInternal constructor(
    /** The capture's id on the glasses, which [Methods.CAPTURES_IMPORT] takes. */
    public val id: String,
    public val type: CaptureType,
    /** When the glasses took it, in milliseconds since 1970. */
    public val time: Long? = null,
    /** The photo's or the video's size in bytes. */
    public val size: Long? = null,
    /**
     * The `content://` URI in the phone's gallery, null while the capture is only on the glasses. Visor grants the
     * app read access to it, so the app needs no media permission.
     */
    public val uri: String? = null,
)

/** An import on its way, at most once per whole percent. */
@Poko
@Serializable
public class CaptureProgress @KitInternal constructor(
    public val id: String,
    /** Bytes received so far. */
    public val received: Long,
    /** Bytes in all. */
    public val size: Long,
)

/** An import that did not finish. */
@Poko
@Serializable
public class CaptureFailure @KitInternal constructor(
    public val id: String,
    /** Why. Its [Failure.reason] is one of the reasons [Methods.CAPTURES_IMPORT] lists. */
    public val failure: Failure,
)

/** The params of [Methods.CAMERA_PHOTO]. */
@Poko
@Serializable
public class PhotoRequest @KitInternal constructor(
    /** True to answer once the photo is in the phone's gallery, false once the glasses have it. */
    public val import: Boolean = true,
)

/** The params of [Methods.CAMERA_VIDEO_START]. */
@Poko
@Serializable
public class VideoRequest @KitInternal constructor(
    public val kind: VideoKind = VideoKind.VIDEO,
)

/** The params of [Methods.CAPTURES_IMPORT]. */
@Poko
@Serializable
public class ImportRequest @KitInternal constructor(
    /** A [Capture.id]. */
    public val id: String,
)
