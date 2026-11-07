package com.macedonga.visor.kit

import com.macedonga.visor.kit.protocol.Capture
import com.macedonga.visor.kit.protocol.ErrorCode
import com.macedonga.visor.kit.protocol.Feeds
import com.macedonga.visor.kit.protocol.LiveConfig
import com.macedonga.visor.kit.protocol.LiveHeader
import com.macedonga.visor.kit.protocol.LivePhoto
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.PhotoRequest
import com.macedonga.visor.kit.protocol.Scope
import com.macedonga.visor.kit.protocol.VideoKind
import com.macedonga.visor.kit.protocol.VideoRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The glasses' camera. Needs [Scope.CAMERA]. The glasses' capture light turns on for every photo and video, as it
 * does for their own button.
 */
public class Camera internal constructor(private val visor: Visor) {
    /** Takes a photo and returns once it is in the phone's gallery, with its [Capture.uri]. */
    public suspend fun takePhoto(): Capture = takePhoto(import = true)

    /** Takes a photo. With [import] false, returns once the glasses have it, before it reaches the phone. */
    public suspend fun takePhoto(import: Boolean): Capture = visor.call(Methods.CAMERA_PHOTO, PhotoRequest(import))

    /** Starts a video and returns once the glasses record. */
    public suspend fun startVideo(): Unit = startVideo(VideoKind.VIDEO)

    public suspend fun startVideo(kind: VideoKind): Unit = visor.call(Methods.CAMERA_VIDEO_START, VideoRequest(kind))

    /** Stops the video and returns once the glasses stopped. Returns at once when no video runs. */
    public suspend fun stopVideo(): Unit = visor.call(Methods.CAMERA_VIDEO_STOP, Unit)

    /**
     * The camera live, as encoded video and, when [config] asks, audio. Collecting starts the Live, unless it runs
     * already; the Live stops when the last collector stops, if a collector started it. Decode the samples with
     * [LiveVideoDecoder] and [LiveAudioDecoder], or keep them as they are. Visor waits for a slow collector; collect
     * on a background dispatcher and do not hold a sample longer than needed.
     *
     * The flow fails with a [KitException]: [ErrorCode.FAILED] with a [KitException.reason] when the Live does not
     * start or the person stops it, as [Feeds.CAMERA_LIVE] lists, and [ErrorCode.VISOR_GONE] when Visor restarts.
     */
    public fun live(config: LiveConfig): Flow<LiveSample> =
        visor.feed(Feeds.CAMERA_LIVE, config).map { (header, data) -> LiveSample(header, data) }

    /** [live] at medium quality, 24 frames a second, without audio. */
    public fun live(): Flow<LiveSample> = live(LiveConfig {})

    /**
     * Takes a photo while the camera is live, without stopping it: a HEIC at the glasses' photo size. Read it from
     * [LivePhoto.uri] with a ContentResolver, within a few minutes. Fails with [ErrorCode.BUSY] when the Live is not
     * running.
     */
    public suspend fun takeLivePhoto(): LivePhoto = visor.call(Methods.CAMERA_LIVE_PHOTO, Unit)
}

/** One record of [Camera.live]: an access unit of video or audio, or a decoder's config, as [header] says. */
public class LiveSample internal constructor(public val header: LiveHeader, public val data: ByteArray) {
    override fun toString(): String = "LiveSample(${header.kind} ${header.ptsUs} us, ${data.size} bytes)"
}
