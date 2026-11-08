package com.macedonga.visor.kit.testing

import com.macedonga.visor.kit.protocol.MotionSample
import com.macedonga.visor.kit.protocol.Quaternion
import com.macedonga.visor.kit.protocol.Vector3
import java.io.InputStream
import java.io.Reader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Plays DAT's motion CSV into [FakeGlasses.motion], at the pace of its timestamps, as DAT's MockMotionKit plays it:
 * no header, 14 numbers a row: the time in nanoseconds, the accelerometer's x, y and z, the gyroscope's, the
 * magnetometer's, and the orientation's w, x, y and z. With [loop], it starts again at the end until cancelled.
 */
public suspend fun FakeGlasses.playMotion(csv: Reader, loop: Boolean) {
    val rows = csv.readLines().filter { it.isNotBlank() }.mapIndexed { i, line ->
        val n = line.split(',').map { it.trim().toDoubleOrNull() ?: throw IllegalArgumentException("Row ${i + 1} holds something that is not a number.") }
        require(n.size == 14) { "Row ${i + 1} has ${n.size} numbers; DAT's motion CSV has 14." }
        n
    }
    require(rows.isNotEmpty()) { "The motion CSV is empty." }
    val span = rows.last()[0].toLong() - rows.first()[0].toLong()
    val step = span / maxOf(1, rows.size - 1)
    var offset = 0L
    do {
        // A lap after the first waits one sample's step, as between any two samples.
        var before = rows.first()[0].toLong() - if (offset > 0) step else 0
        for (n in rows) {
            val time = n[0].toLong()
            delay((time - before) / 1_000_000)
            before = time
            fun v(at: Int) = Vector3(n[at].toFloat(), n[at + 1].toFloat(), n[at + 2].toFloat())
            motion(MotionSample(time + offset, v(1), v(4), v(7), Quaternion(n[11].toFloat(), n[12].toFloat(), n[13].toFloat(), n[10].toFloat())))
        }
        // The next lap goes on from the last sample, one sample's step later.
        offset += span + step
    } while (loop)
}

/**
 * Plays a recorded camera into the Live, as DAT's mock plays a video file: an Annex-B H.264 or H.265 stream, such
 * as `ffmpeg -i clip.mp4 -c:v copy -bsf:v hevc_mp4toannexb clip.h265` makes. [codec] is `video/avc` or
 * `video/hevc`. It waits for an app to watch, then sends a frame every 1/[fps] of a second; with [loop], it starts
 * again at the end until cancelled.
 */
public suspend fun FakeLive.play(stream: InputStream, codec: String, width: Int, height: Int, fps: Int, loop: Boolean) {
    require(codec == AVC || codec == HEVC) { "The codec is $AVC or $HEVC, not $codec." }
    require(fps in 1..60) { "fps is 1 to 60." }
    val cut = AnnexB.frames(stream.readBytes(), codec == HEVC)
    require(cut.config.isNotEmpty() && cut.frames.isNotEmpty()) { "No parameter sets or no frames in the stream." }
    watching.first { it }
    sendVideoConfig(cut.config, codec, width, height)
    val stepUs = 1_000_000L / fps
    var pts = 0L
    do {
        for ((frame, key) in cut.frames) {
            sendVideo(frame, pts, key)
            pts += stepUs
            delay(stepUs / 1000)
        }
    } while (loop)
}

private const val AVC = "video/avc"
private const val HEVC = "video/hevc"

/** An Annex-B stream cut into its parameter sets and its frames, each frame with whether it is a key frame. */
private class AnnexB(val config: ByteArray, val frames: List<Pair<ByteArray, Boolean>>) {
    companion object {
        private val START = byteArrayOf(0, 0, 0, 1)

        fun frames(data: ByteArray, hevc: Boolean): AnnexB {
            var config = ByteArray(0)
            val frames = mutableListOf<Pair<ByteArray, Boolean>>()
            var frame = ByteArray(0)
            var key = false
            var pending = ByteArray(0)
            for (nal in units(data)) {
                if (nal.size < 3) continue
                val type = if (hevc) (nal[0].toInt() shr 1) and 0x3F else nal[0].toInt() and 0x1F
                val parameterSet = if (hevc) type in 32..34 else type == 7 || type == 8
                val slice = if (hevc) type in 0..31 else type in 1..5
                when {
                    // The first parameter sets configure the decoder; later repeats ride along with their frame.
                    parameterSet && frames.isEmpty() && frame.isEmpty() -> config += START + nal
                    slice -> {
                        // A picture's first slice: first_slice_segment_in_pic_flag, or first_mb_in_slice == 0.
                        val first = (if (hevc) nal[2] else nal[1]).toInt() and 0x80 != 0
                        if (first && frame.isNotEmpty()) {
                            frames += frame to key
                            frame = ByteArray(0)
                            key = false
                        }
                        frame += pending + START + nal
                        pending = ByteArray(0)
                        key = key || if (hevc) type in 16..21 else type == 5
                    }
                    else -> pending += START + nal
                }
            }
            if (frame.isNotEmpty()) frames += frame to key
            return AnnexB(config, frames)
        }

        /** The NAL units between start codes, without them. */
        private fun units(data: ByteArray): List<ByteArray> {
            val starts = mutableListOf<Pair<Int, Int>>()
            var i = 0
            while (i + 2 < data.size) {
                if (data[i].toInt() == 0 && data[i + 1].toInt() == 0 && data[i + 2].toInt() == 1) {
                    starts += i to i + 3
                    i += 3
                } else {
                    i++
                }
            }
            return starts.mapIndexed { n, (_, from) ->
                var to = if (n + 1 < starts.size) starts[n + 1].first else data.size
                // A four-byte start code leaves a zero at the end of the unit before it.
                while (to > from && data[to - 1].toInt() == 0 && n + 1 < starts.size) to--
                data.copyOfRange(from, to)
            }
        }
    }
}
