package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/**
 * A stream of binary records, such as video: what a [Topic] is for events, too big for JSON. Each record is a header
 * of type [H] and a payload of bytes. The same rules as for a [Method] apply to the params and the header.
 *
 * Over binder, Visor writes the records to a socket the app hands it; see [KitProtocol.FEED_END]. Over JSON-RPC a
 * feed is subscribed like a topic, and each event is `{"header": H, "payload": "<Base64>"}`.
 */
public class Feed<P, H> internal constructor(
    /** The name on the wire, for example `camera.live`. */
    public val name: String,
    /** The scope the app needs, or null when any app Visor lets in may open it. */
    public val scope: Scope?,
    /** The [KitProtocol.VERSION] that added it. */
    public val since: Int,
    /** One sentence for the reference and the OpenRPC document. */
    public val summary: String,
    public val params: KSerializer<P>,
    public val header: KSerializer<H>,
) {
    override fun toString(): String = name
}

/** Every feed Visor sends. */
public object Feeds {
    /**
     * The glasses' camera, live, from Visor's Live view over Wi-Fi Direct: the video, and the microphones' audio when
     * asked. While anyone watches, the glasses film and their capture light is on. Each kind starts with a record
     * that configures its decoder, and the video then starts on a keyframe. Ends with [ErrorCode.NOT_CONNECTED], or
     * with [ErrorCode.FAILED] and a reason: `stopped` when the person stops the Live, else why it did not start or
     * gave up: `in_case`, `wifi_off`, `wifi_direct_denied`, `wifi_busy` (an import holds Wi-Fi), `vpn`, `no_key`,
     * `join_failed`, `session_failed`, `no_video` or `gave_up`.
     */
    public val CAMERA_LIVE: Feed<LiveConfig, LiveHeader> = Feed(
        "camera.live", Scope.CAMERA, 1,
        "The glasses' camera live, with their microphones when asked, as encoded video and audio.",
        LiveConfig.serializer(), LiveHeader.serializer(),
    )

    public val all: List<Feed<*, *>> = listOf(CAMERA_LIVE)
}

/** How big the live video is, upright. */
@Serializable
@JvmInline
public value class LiveQuality(public val id: String) {
    override fun toString(): String = id

    public companion object {
        /** 360 x 640. */
        public val LOW: LiveQuality = LiveQuality("low")

        /** 504 x 896. */
        public val MEDIUM: LiveQuality = LiveQuality("medium")

        /** 720 x 1280. */
        public val HIGH: LiveQuality = LiveQuality("high")

        /** 1080 x 1920. */
        public val FULL_HD: LiveQuality = LiveQuality("full_hd")
    }
}

/**
 * The params of [Feeds.CAMERA_LIVE]. Build one with [LiveConfig]. They apply when the feed starts the Live; while
 * the Live already runs, for Visor's own view or another app, the feed takes it as it is.
 */
@Poko
@Serializable
public class LiveConfig @KitInternal constructor(
    public val quality: LiveQuality = LiveQuality.MEDIUM,
    /** Frames a second, one of [FRAME_RATES]. */
    public val frameRate: Int = DEFAULT_FRAME_RATE,
    /** True to get the microphones' audio too. */
    public val audio: Boolean = false,
) {
    public class Builder {
        public var quality: LiveQuality = LiveQuality.MEDIUM
        public var frameRate: Int = DEFAULT_FRAME_RATE
        public var audio: Boolean = false

        public fun build(): LiveConfig = LiveConfig(quality, frameRate, audio)
    }

    public companion object {
        public const val DEFAULT_FRAME_RATE: Int = 24
        public val FRAME_RATES: List<Int> = listOf(2, 7, 15, 24, 30)
    }
}

/** A [LiveConfig], for example `LiveConfig { quality = LiveQuality.HIGH; audio = true }`. */
public fun LiveConfig(block: LiveConfig.Builder.() -> Unit): LiveConfig = LiveConfig.Builder().apply(block).build()

/** What a live record holds. */
@Serializable
@JvmInline
public value class LiveKind(public val id: String) {
    override fun toString(): String = id

    public companion object {
        public val VIDEO: LiveKind = LiveKind("video")
        public val AUDIO: LiveKind = LiveKind("audio")
    }
}

/** The header of one record of [Feeds.CAMERA_LIVE]. The payload is one access unit, or a decoder's config. */
@Poko
@Serializable
public class LiveHeader @KitInternal constructor(
    public val kind: LiveKind,
    /** The MIME type a MediaCodec takes: `video/hevc` or `video/avc`, Annex-B; `audio/mp4a-latm`, raw AAC-LC. */
    public val codec: String,
    /** The glasses' presentation time, in microseconds. 0 on a config record. */
    public val ptsUs: Long,
    /**
     * True when the payload configures the decoder instead of holding media: the video's parameter sets, or the
     * audio's AudioSpecificConfig. A new one can come mid-stream, when the Live restarts.
     */
    public val config: Boolean = false,
    /** True for a video keyframe. */
    public val key: Boolean = false,
    /** On video config records: the picture's size, from the parameter sets, when Visor could read it. */
    public val width: Int? = null,
    public val height: Int? = null,
    /** On audio config records. */
    public val sampleRate: Int? = null,
    public val channels: Int? = null,
)

/** A photo taken while the camera is live, from [Methods.CAMERA_LIVE_PHOTO]. */
@Poko
@Serializable
public class LivePhoto @KitInternal constructor(
    /** `image/heic`. */
    public val mime: String,
    /** For an app: a `content://` URI that Visor lets the app read for a few minutes. */
    public val uri: String? = null,
    /** For a computer over adb or the network door: the photo itself, Base64. */
    public val data: String? = null,
)
