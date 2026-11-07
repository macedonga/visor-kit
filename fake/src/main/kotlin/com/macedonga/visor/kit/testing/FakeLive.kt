package com.macedonga.visor.kit.testing

import com.macedonga.visor.kit.KitException
import com.macedonga.visor.kit.protocol.ErrorCode
import com.macedonga.visor.kit.protocol.LiveConfig
import com.macedonga.visor.kit.protocol.LiveHeader
import com.macedonga.visor.kit.protocol.LiveKind
import com.macedonga.visor.kit.protocol.LivePhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.updateAndGet

/**
 * The fake glasses' live camera, for [com.macedonga.visor.kit.Camera.live]. Nothing is filmed: the test sends each
 * sample. Wait for [watching] before sending, as a sample sent while nobody watches is lost.
 *
 * ```
 * fake.live.watching.first { it }
 * fake.live.sendVideoConfig(parameterSets)
 * fake.live.sendVideo(keyframe, ptsUs = 0, key = true)
 * ```
 */
public class FakeLive internal constructor(private val glasses: FakeGlasses) {
    private val samples = MutableSharedFlow<Pair<LiveHeader, ByteArray>?>(extraBufferCapacity = 256)
    private var videoConfig: Pair<LiveHeader, ByteArray>? = null
    private var audioConfig: Pair<LiveHeader, ByteArray>? = null
    private var watchers = 0

    private val _watching = MutableStateFlow(false)

    /** True while an app collects the live camera. */
    public val watching: StateFlow<Boolean> = _watching.asStateFlow()

    /** What the first app to watch asked for, while it watches; the Live as it is serves the others. */
    public var config: LiveConfig? = null
        private set

    private val _photos = MutableStateFlow(0)

    /** How many photos the app took while live. */
    public val photos: StateFlow<Int> = _photos.asStateFlow()

    /** The video's parameter sets, sent now and to each app that starts watching later. */
    public fun sendVideoConfig(config: ByteArray) {
        sendVideoConfig(config, HEVC, width = 504, height = 896)
    }

    public fun sendVideoConfig(config: ByteArray, codec: String, width: Int, height: Int) {
        val sample = LiveHeader(LiveKind.VIDEO, codec, 0, config = true, width = width, height = height) to config
        synchronized(this) { videoConfig = sample }
        samples.tryEmit(sample)
    }

    /** One access unit of video, Annex-B. */
    public fun sendVideo(data: ByteArray, ptsUs: Long, key: Boolean) {
        val codec = synchronized(this) { videoConfig?.first?.codec } ?: HEVC
        samples.tryEmit(LiveHeader(LiveKind.VIDEO, codec, ptsUs, key = key) to data)
    }

    /** The audio's AudioSpecificConfig, sent now and to each app that starts watching later. */
    public fun sendAudioConfig(config: ByteArray, sampleRate: Int, channels: Int) {
        val sample = LiveHeader(LiveKind.AUDIO, AAC, 0, config = true, sampleRate = sampleRate, channels = channels) to config
        synchronized(this) { audioConfig = sample }
        samples.tryEmit(sample)
    }

    /** One raw AAC access unit. */
    public fun sendAudio(data: ByteArray, ptsUs: Long) {
        samples.tryEmit(LiveHeader(LiveKind.AUDIO, AAC, ptsUs) to data)
    }

    /** Stops the Live as the person would: every app's flow fails with the reason `stopped`. */
    public fun stop() {
        samples.tryEmit(null)
    }

    internal fun watch(config: LiveConfig): Flow<Pair<LiveHeader, ByteArray>> {
        glasses.ready()
        return samples
            .onStart {
                val configs = synchronized(this@FakeLive) {
                    if (watchers++ == 0) this@FakeLive.config = config
                    _watching.value = true
                    listOfNotNull(videoConfig, audioConfig)
                }
                configs.forEach { emit(it) }
            }
            .onCompletion {
                synchronized(this@FakeLive) {
                    if (--watchers == 0) {
                        this@FakeLive.config = null
                        _watching.value = false
                    }
                }
            }
            .map { it ?: throw KitException(ErrorCode.FAILED, "The fake Live was stopped.", reason = "stopped") }
            .filter { config.audio || it.first.kind != LiveKind.AUDIO }
    }

    internal fun photo(): LivePhoto {
        if (!_watching.value) throw KitException(ErrorCode.BUSY, "The fake Live is not running.")
        val n = _photos.updateAndGet { it + 1 }
        return LivePhoto(HEIC, uri = "content://$AUTHORITY/live/$n")
    }

    private companion object {
        const val HEVC = "video/hevc"
        const val AAC = "audio/mp4a-latm"
        const val HEIC = "image/heic"
        const val AUTHORITY = "com.macedonga.visor.kit.testing"
    }
}
