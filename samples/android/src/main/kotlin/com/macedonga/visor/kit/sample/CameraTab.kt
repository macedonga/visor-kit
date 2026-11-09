package com.macedonga.visor.kit.sample

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.macedonga.visor.kit.LiveAudioDecoder
import com.macedonga.visor.kit.LiveVideoDecoder
import com.macedonga.visor.kit.protocol.Capture
import com.macedonga.visor.kit.protocol.CaptureType
import com.macedonga.visor.kit.protocol.GlassesStatus
import com.macedonga.visor.kit.protocol.LiveConfig
import com.macedonga.visor.kit.protocol.LiveQuality
import com.macedonga.visor.kit.protocol.VideoKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

@Composable
fun CameraTab(lab: Lab, status: GlassesStatus?) {
    CapturePanel(lab, status)
    LivePanel(lab)
    CapturesPanel(lab)
}

@Composable
private fun CapturePanel(lab: Lab, status: GlassesStatus?) {
    val recording = status?.recording == true
    var since by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(0L) }
    var importPhotos by remember { mutableStateOf(true) }
    if (recording) {
        LaunchedEffect(Unit) {
            since = System.currentTimeMillis()
            while (true) {
                now = System.currentTimeMillis()
                delay(500)
            }
        }
    }
    Panel("Photo and video") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button({ lab.run("camera.photo") { it.camera.takePhoto(importPhotos).describe() } }, enabled = !recording) { Text("Take photo") }
            FilterChip(selected = importPhotos, onClick = { importPhotos = !importPhotos }, label = { Text("Import it") })
        }
        if (recording) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tag("Recording", true, Ink.bad)
                Text("  " + clock(now - since), color = Ink.text, fontSize = 18.sp, fontFamily = Mono, modifier = Modifier.weight(1f))
                Button({ lab.run("camera.video.stop") { it.camera.stopVideo() } }) { Text("Stop") }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton({ lab.run("camera.video.start") { it.camera.startVideo(VideoKind.VIDEO) } }) { Text("Video") }
                FilledTonalButton({ lab.run("camera.video.start hyperlapse") { it.camera.startVideo(VideoKind.HYPERLAPSE) } }) { Text("Hyperlapse") }
            }
        }
    }
}

private fun clock(ms: Long): String = "%d:%02d".format(ms / 60_000, ms / 1000 % 60)

@Composable
private fun LivePanel(lab: Lab) {
    var on by remember { mutableStateOf(false) }
    var quality by remember { mutableStateOf(LiveQuality.MEDIUM) }
    var frameRate by remember { mutableStateOf(LiveConfig.DEFAULT_FRAME_RATE) }
    var audio by remember { mutableStateOf(false) }
    Panel("Live") {
        StreamSwitch("Follow camera.live", on) { on = it }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(LiveQuality.LOW, LiveQuality.MEDIUM, LiveQuality.HIGH, LiveQuality.FULL_HD).forEach { q ->
                FilterChip(selected = q == quality, onClick = { quality = q }, label = { Text(q.id.replace('_', ' ')) })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LiveConfig.FRAME_RATES.forEach { r ->
                FilterChip(selected = r == frameRate, onClick = { frameRate = r }, label = { Text("$r fps") })
            }
            FilterChip(selected = audio, onClick = { audio = !audio }, label = { Text("Sound") })
        }
        if (on) {
            LivePreview(lab, LiveConfig { this.quality = quality; this.frameRate = frameRate; this.audio = audio })
            OutlinedButton({ lab.run("camera.live.photo") { it.camera.takeLivePhoto().let { p -> p.uri ?: p.mime } } }, Modifier.fillMaxWidth()) {
                Text("Take a photo while live")
            }
        }
    }
}

/** The Live on a SurfaceView, decoded on the phone, its sound played when asked, for as long as it shows. */
@Composable
private fun LivePreview(lab: Lab, config: LiveConfig) {
    var surface by remember { mutableStateOf<Surface?>(null) }
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        surface = holder.surface
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        surface = null
                    }
                })
            }
        },
        // The glasses film upright.
        modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(16.dp)),
    )
    val target = surface ?: return
    LaunchedEffect(config, target, lab.grants) {
        val speaker = Speaker()
        LiveVideoDecoder(target).use { video ->
            LiveAudioDecoder(speaker::play).use { sound ->
                with(lab) {
                    visor.camera.live(config).logged("camera.live").flowOn(Dispatchers.IO).collect { sample ->
                        video.decode(sample)
                        if (config.audio) sound.decode(sample)
                    }
                }
            }
        }
        speaker.close()
    }
}

/** Plays decoded PCM as it comes, the track made for the first sample's rate and channels. */
private class Speaker : AutoCloseable {
    private var track: AudioTrack? = null

    fun play(samples: ShortArray, sampleRate: Int, channels: Int, @Suppress("UNUSED_PARAMETER") ptsUs: Long) {
        val t = track ?: AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setAudioFormat(
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate)
                    .setChannelMask(if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO).build(),
            )
            .build().also {
                it.play()
                track = it
            }
        t.write(samples, 0, samples.size)
    }

    override fun close() {
        track?.release()
        track = null
    }
}

@Composable
private fun CapturesPanel(lab: Lab) {
    val waiting = remember { mutableStateMapOf<String, Capture>() }
    val progress = remember { mutableStateMapOf<String, Float>() }
    val done = remember { mutableStateMapOf<String, Capture>() }
    val failed = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(lab.grants) {
        with(lab) {
            launch { visor.captures.available.logged("captures.available").collect { waiting[it.id] = it } }
            launch {
                visor.captures.progress.logged("captures.progress").collect {
                    if (it.size > 0) progress[it.id] = it.received.toFloat() / it.size
                }
            }
            launch {
                visor.captures.imported.logged("captures.imported").collect {
                    waiting.remove(it.id)
                    progress.remove(it.id)
                    failed.remove(it.id)
                    done[it.id] = it
                }
            }
            launch {
                visor.captures.failed.logged("captures.failed").collect {
                    progress.remove(it.id)
                    failed[it.id] = it.failure.reason ?: it.failure.code.toString()
                }
            }
        }
    }

    Panel("Captures on the glasses", trailing = {
        OutlinedButton({
            lab.run("captures.list") { v -> v.captures.list().also { list -> list.forEach { waiting[it.id] = it } }.size.let { "$it waiting" } }
        }) { Text("Refresh") }
    }) {
        if (waiting.isEmpty()) Absent("None waiting. New photos and videos show up here as the glasses take them.")
        waiting.values.sortedByDescending { it.time ?: 0 }.forEachIndexed { i, capture ->
            if (i > 0) HorizontalDivider(color = Ink.line)
            CaptureRow(capture, progress[capture.id], failed[capture.id]) {
                failed.remove(capture.id)
                progress[capture.id] = 0f
                lab.run("captures.import") { it.captures.import(capture.id).describe() }
            }
        }
    }
    if (done.isNotEmpty()) {
        Panel("Imported this session") {
            done.values.forEach { EventLine(it.describe(), it.size?.let(::bytes), Ink.dim) }
        }
    }
}

@Composable
private fun CaptureRow(capture: Capture, progress: Float?, failure: String?, onImport: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(kind(capture.type), color = Ink.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(listOfNotNull(capture.time?.let(::time), capture.size?.let(::bytes)).joinToString(", "), color = Ink.dim, fontSize = 13.sp)
            }
            if (progress == null) FilledTonalButton(onImport) { Text(if (failure != null) "Try again" else "Import") }
        }
        if (progress != null) LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth())
        if (failure != null) Text(failure.replace('_', ' '), color = Ink.bad, fontSize = 13.sp)
    }
}

private fun Capture.describe(): String = listOfNotNull(kind(type), uri).joinToString(": ")

private fun kind(type: CaptureType) = when (type) {
    CaptureType.PHOTO -> "Photo"
    CaptureType.VIDEO -> "Video"
    CaptureType.LIVE -> "Live recording"
    else -> type.id
}

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault())

private fun time(ms: Long): String = TIME.format(Instant.ofEpochMilli(ms))

private fun bytes(n: Long): String = when {
    n >= 1 shl 20 -> "%.1f MB".format(n / 1048576.0)
    n >= 1 shl 10 -> "${n / 1024} KB"
    else -> "$n B"
}
