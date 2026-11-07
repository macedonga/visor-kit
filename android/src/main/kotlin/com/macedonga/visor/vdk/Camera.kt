package com.macedonga.visor.vdk

import com.macedonga.visor.vdk.protocol.Capture
import com.macedonga.visor.vdk.protocol.Methods
import com.macedonga.visor.vdk.protocol.PhotoRequest
import com.macedonga.visor.vdk.protocol.Scope
import com.macedonga.visor.vdk.protocol.VideoKind
import com.macedonga.visor.vdk.protocol.VideoRequest

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
}
