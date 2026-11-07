package com.macedonga.visor.kit

import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.view.Surface
import com.macedonga.visor.kit.protocol.LiveHeader
import com.macedonga.visor.kit.protocol.LiveKind
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes the video of [Camera.live] with the phone's decoder. Hand it every sample, on one thread: it skips the
 * audio, waits for the config and a keyframe, and starts over on a new config. [close] it when done.
 */
public class LiveVideoDecoder private constructor(
    private val surface: Surface?,
    private val onImage: ((Image, Long) -> Unit)?,
) : AutoCloseable {
    /** Draws each picture on [surface] as soon as it is decoded: a SurfaceView's, a TextureView's, an ImageReader's. */
    public constructor(surface: Surface) : this(surface, null)

    /**
     * Hands each picture to [onImage] as a YUV_420_888 [Image], with its presentation time in microseconds, on the
     * thread that calls [decode]. The image is valid only until [onImage] returns.
     */
    public constructor(onImage: (image: Image, ptsUs: Long) -> Unit) : this(null, onImage)

    private val decoder = Decoder(LiveKind.VIDEO) { header, config ->
        val format = MediaFormat.createVideoFormat(header.codec, header.width ?: MAX_SIDE, header.height ?: MAX_SIDE)
        // Room for any size the glasses send: a decoder fed a larger stream than it was set up for draws garbage.
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, MAX_SIDE)
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, MAX_SIDE)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INPUT)
        if (Build.VERSION.SDK_INT >= 30) format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
        if (surface == null) format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        MediaCodec.createDecoderByType(header.codec).apply {
            configure(format, surface, null, 0)
            start()
            queue(this, config, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
        }
    }

    public fun decode(sample: LiveSample) {
        decoder.decode(sample) { codec, index, info ->
            if (surface != null) {
                // Now, not at the presentation time: that is on the glasses' clock.
                codec.releaseOutputBuffer(index, System.nanoTime())
            } else {
                codec.getOutputImage(index)?.use { onImage?.invoke(it, info.presentationTimeUs) }
                codec.releaseOutputBuffer(index, false)
            }
        }
    }

    override fun close(): Unit = decoder.close()

    private companion object {
        const val MAX_SIDE = 1920
        const val MAX_INPUT = 4 shl 20
    }
}

/**
 * Decodes the audio of [Camera.live], which needs [com.macedonga.visor.kit.protocol.LiveConfig.audio], to 16-bit
 * PCM. Hand it every sample, on one thread: it skips the video. [onPcm] gets interleaved samples, at the sample
 * rate and channel count of the audio's config record, and their presentation time in microseconds.
 */
public class LiveAudioDecoder(
    private val onPcm: (samples: ShortArray, sampleRate: Int, channels: Int, ptsUs: Long) -> Unit,
) : AutoCloseable {
    private var sampleRate = 0
    private var channels = 0

    private val decoder = Decoder(LiveKind.AUDIO) { header, config ->
        sampleRate = header.sampleRate ?: DEFAULT_RATE
        channels = header.channels ?: DEFAULT_CHANNELS
        val format = MediaFormat.createAudioFormat(header.codec, sampleRate, channels)
        format.setByteBuffer("csd-0", ByteBuffer.wrap(config))
        format.setInteger(MediaFormat.KEY_IS_ADTS, 0)
        MediaCodec.createDecoderByType(header.codec).apply {
            configure(format, null, null, 0)
            start()
        }
    }

    public fun decode(sample: LiveSample) {
        decoder.decode(sample) { codec, index, info ->
            val buffer = codec.getOutputBuffer(index)
            if (buffer != null && info.size > 0) {
                buffer.position(info.offset).limit(info.offset + info.size)
                val samples = ShortArray(info.size / 2)
                buffer.order(ByteOrder.nativeOrder()).asShortBuffer().get(samples)
                onPcm(samples, sampleRate, channels, info.presentationTimeUs)
            }
            codec.releaseOutputBuffer(index, false)
        }
    }

    override fun close(): Unit = decoder.close()

    private companion object {
        const val DEFAULT_RATE = 48_000
        const val DEFAULT_CHANNELS = 2
    }
}

/** A MediaCodec fed one kind of live sample, in synchronous mode, on the caller's thread. */
private class Decoder(
    private val kind: LiveKind,
    private val create: (LiveHeader, ByteArray) -> MediaCodec,
) {
    private var codec: MediaCodec? = null
    private var config: LiveSample? = null
    private var waitingKey = true
    private val info = MediaCodec.BufferInfo()

    fun decode(sample: LiveSample, output: (MediaCodec, Int, MediaCodec.BufferInfo) -> Unit) {
        val header = sample.header
        if (header.kind != kind) return
        if (header.config) {
            config = sample
            restart()
            return
        }
        val codec = codec ?: return
        if (kind == LiveKind.VIDEO && waitingKey && !header.key) return
        try {
            waitingKey = !queue(codec, sample.data, header.ptsUs, 0) { drain(codec, output) }
            drain(codec, output)
        } catch (e: IllegalStateException) {
            // The codec failed (a CodecException is one): a fresh one from the last config, from the next keyframe.
            restart()
        }
    }

    fun close() {
        codec?.let(::release)
        codec = null
        config = null
    }

    private fun restart() {
        codec?.let(::release)
        codec = null
        waitingKey = true
        val config = config ?: return
        codec = try {
            create(config.header, config.data)
        } catch (e: Exception) {
            // No decoder for this codec on this phone, or it would not start: nothing to decode with until a new config.
            null
        }
    }

    private fun drain(codec: MediaCodec, output: (MediaCodec, Int, MediaCodec.BufferInfo) -> Unit) {
        while (true) {
            val index = codec.dequeueOutputBuffer(info, 0)
            when {
                index >= 0 -> output(codec, index, info)
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> return
            }
        }
    }

    private fun release(codec: MediaCodec) {
        try {
            codec.stop()
        } catch (e: IllegalStateException) {
            // Failed already: release it all the same.
        }
        codec.release()
    }
}

/** Queues [data]; false when the decoder took none of it, and the video must wait for a keyframe. */
private fun queue(codec: MediaCodec, data: ByteArray, ptsUs: Long, flags: Int, makeRoom: () -> Unit = {}): Boolean {
    repeat(INPUT_TRIES) {
        val index = codec.dequeueInputBuffer(INPUT_WAIT_US)
        if (index < 0) return@repeat makeRoom()
        val buffer = codec.getInputBuffer(index)
        if (buffer == null || buffer.capacity() < data.size) {
            codec.queueInputBuffer(index, 0, 0, ptsUs, 0)
            return false
        }
        buffer.clear()
        buffer.put(data)
        codec.queueInputBuffer(index, 0, data.size, ptsUs, flags)
        return true
    }
    return false
}

private const val INPUT_TRIES = 10
private const val INPUT_WAIT_US = 10_000L
