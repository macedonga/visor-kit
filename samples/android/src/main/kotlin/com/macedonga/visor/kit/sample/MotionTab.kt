package com.macedonga.visor.kit.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macedonga.visor.kit.protocol.MotionRequest
import com.macedonga.visor.kit.protocol.MotionSample
import com.macedonga.visor.kit.protocol.Quaternion
import com.macedonga.visor.kit.protocol.Vector3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MotionTab(lab: Lab) {
    var on by remember { mutableStateOf(false) }
    var rate by remember { mutableIntStateOf(MotionRequest.DEFAULT_RATE_HZ) }
    val trace = remember { MotionTrace() }

    Panel("The glasses' motion") {
        StreamSwitch("Follow glasses.motion", on) { on = it }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MotionRequest.RATES.forEach { r ->
                FilterChip(selected = r == rate, onClick = { rate = r }, label = { Text("$r Hz") })
            }
        }
        if (on) {
            LaunchedEffect(rate, lab.grants) {
                trace.clear()
                with(lab) { visor.glasses.motion(rate).logged("glasses.motion").collect(trace::add) }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Stat(if (trace.total == 0L) "-" else "%.1f".format(trace.rate), "Samples a second", Modifier.weight(1f))
            Stat(trace.total.toString(), "Samples", Modifier.weight(1f))
        }
    }

    Panel("Orientation", trailing = {
        OutlinedButton({ trace.zero() }, enabled = trace.hasOrientation) { Text("Set straight ahead") }
    }) {
        if (!trace.hasOrientation) Absent(if (on) "Waiting for an orientation" else "Off")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Horizon(trace, Modifier.weight(1f).aspectRatio(1f))
            Compass(trace, Modifier.weight(1f).aspectRatio(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            Stat(degrees(trace.pitch), "Pitch", Modifier.weight(1f))
            Stat(degrees(trace.roll), "Roll", Modifier.weight(1f))
            Stat(degrees(trace.heading), "Heading", Modifier.weight(1f))
        }
    }

    Panel("Sensors") {
        Trace("Accelerometer", "m/s²", trace, trace.accel, floor = 12f)
        Trace("Gyroscope", "rad/s", trace, trace.gyro, floor = 2f)
        if (trace.total > 30 && !trace.hasMagnetometer) {
            Text("Magnetometer", color = Ink.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Absent("None on these glasses: the heading drifts.")
        } else {
            Trace("Magnetometer", "µT", trace, trace.mag, floor = 60f)
        }
    }
}

private fun degrees(radians: Float) = "${(radians * 180 / PI).roundToInt()}°"

private val SKY = Color(0xFF1C2A36)
private val GROUND = Color(0xFF2B251E)

/** The last samples of each sensor in rings, and the orientation as angles, for the drawings to read. */
@Stable
class MotionTrace(val size: Int = 240) {
    val accel = FloatArray(size * 3)
    val gyro = FloatArray(size * 3)
    val mag = FloatArray(size * 3)
    var head = 0
        private set
    var count = 0
        private set

    /** Raised on each sample: the drawings read it, so they draw again. */
    var frame by mutableLongStateOf(0L)
        private set
    var total by mutableLongStateOf(0L)
        private set
    var rate by mutableFloatStateOf(0f)
        private set
    var pitch by mutableFloatStateOf(0f)
        private set
    var roll by mutableFloatStateOf(0f)
        private set
    private var yaw by mutableFloatStateOf(0f)
    private var zero = floatArrayOf(0f, 0f, 0f)
    var hasOrientation by mutableStateOf(false)
        private set
    var hasMagnetometer by mutableStateOf(false)
        private set

    /** Where the head points, clockwise from where it pointed when the stream started or was set straight ahead. */
    val heading: Float get() = wrap(zero[2] - yaw)

    private var windowStart = -1L
    private var windowSamples = 0

    fun clear() {
        head = 0
        count = 0
        total = 0
        rate = 0f
        windowStart = -1
        windowSamples = 0
        hasOrientation = false
        hasMagnetometer = false
        zero = floatArrayOf(0f, 0f, 0f)
    }

    fun add(sample: MotionSample) {
        put(accel, sample.accelerometer)
        put(gyro, sample.gyroscope)
        put(mag, sample.magnetometer)
        head = (head + 1) % size
        count = minOf(count + 1, size)
        if (sample.magnetometer != null) hasMagnetometer = true
        sample.orientation?.let(::orient)
        total++
        measure(sample.time)
        frame++
    }

    /** Takes the pose now as level and straight ahead: the fit of the frame on the face goes with it. */
    fun zero() {
        zero = floatArrayOf(zero[0] + pitch, zero[1] + roll, yaw)
    }

    private fun put(ring: FloatArray, v: Vector3?) {
        val i = head * 3
        ring[i] = v?.x ?: Float.NaN
        ring[i + 1] = v?.y ?: Float.NaN
        ring[i + 2] = v?.z ?: Float.NaN
    }

    /**
     * The head's angles from the orientation, which turns the sensor's frame into an earth frame with z up. The
     * sensors sit in the temple: x points down, y back and z to the wearer's left, as measured on Ray-Ban Meta (Gen 2).
     */
    private fun orient(q: Quaternion) {
        val forward = rotate(q, 0f, -1f, 0f)
        val right = rotate(q, 0f, 0f, -1f)
        val up = rotate(q, -1f, 0f, 0f)
        val first = !hasOrientation
        yaw = atan2(forward[1], forward[0])
        if (first) zero = floatArrayOf(0f, 0f, yaw)
        pitch = asin(forward[2].coerceIn(-1f, 1f)) - zero[0]
        roll = atan2(-right[2], up[2]) - zero[1]
        hasOrientation = true
    }

    // The vector v in the sensor's frame, in the earth frame.
    private fun rotate(q: Quaternion, vx: Float, vy: Float, vz: Float): FloatArray {
        val x = q.x
        val y = q.y
        val z = q.z
        val w = q.w
        return floatArrayOf(
            (1 - 2 * (y * y + z * z)) * vx + 2 * (x * y - w * z) * vy + 2 * (x * z + w * y) * vz,
            2 * (x * y + w * z) * vx + (1 - 2 * (x * x + z * z)) * vy + 2 * (y * z - w * x) * vz,
            2 * (x * z - w * y) * vx + 2 * (y * z + w * x) * vy + (1 - 2 * (x * x + y * y)) * vz,
        )
    }

    // Samples a second on the glasses' own clock, over windows of a second.
    private fun measure(time: Long) {
        if (windowStart < 0) {
            windowStart = time
            windowSamples = 0
            return
        }
        windowSamples++
        val span = time - windowStart
        if (span >= 1_000_000_000L) {
            rate = windowSamples * 1e9f / span
            windowStart = time
            windowSamples = 0
        }
    }

    private fun wrap(a: Float): Float {
        var r = a
        while (r > PI) r -= (2 * PI).toFloat()
        while (r < -PI) r += (2 * PI).toFloat()
        return r
    }
}

/** An attitude indicator: the horizon tilts with the roll and moves with the pitch. */
@Composable
private fun Horizon(trace: MotionTrace, modifier: Modifier) {
    Canvas(modifier) {
        trace.frame
        val r = size.minDimension / 2
        val c = center
        val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
        clipPath(circle) {
            drawRect(SKY)
            rotate(-trace.roll * 180f / PI.toFloat(), c) {
                val shift = (trace.pitch / (PI.toFloat() / 2)) * r
                drawRect(GROUND, topLeft = Offset(c.x - 2 * r, c.y + shift), size = Size(4 * r, 2 * r))
                drawLine(Ink.live, Offset(c.x - 2 * r, c.y + shift), Offset(c.x + 2 * r, c.y + shift), strokeWidth = 2.dp.toPx())
                for (step in listOf(-2, -1, 1, 2)) {
                    val y = c.y + shift - step * r / 4.5f
                    val half = if (step % 2 == 0) r * 0.28f else r * 0.16f
                    drawLine(Ink.faint, Offset(c.x - half, y), Offset(c.x + half, y), strokeWidth = 1.dp.toPx())
                }
            }
        }
        drawCircle(Ink.line, r, c, style = Stroke(1.dp.toPx()))
        // The reticle stays still: the glasses' own level.
        drawLine(Ink.warn, Offset(c.x - r * 0.45f, c.y), Offset(c.x - r * 0.12f, c.y), strokeWidth = 3.dp.toPx())
        drawLine(Ink.warn, Offset(c.x + r * 0.12f, c.y), Offset(c.x + r * 0.45f, c.y), strokeWidth = 3.dp.toPx())
        drawCircle(Ink.warn, 3.dp.toPx(), c)
    }
}

/** A heading dial that turns as the head turns, from where it was zeroed. */
@Composable
private fun Compass(trace: MotionTrace, modifier: Modifier) {
    Canvas(modifier) {
        trace.frame
        val r = size.minDimension / 2
        val c = center
        drawCircle(Ink.raised, r, c)
        drawCircle(Ink.line, r, c, style = Stroke(1.dp.toPx()))
        rotate(-trace.heading * 180f / PI.toFloat(), c) {
            for (deg in 0 until 360 step 15) {
                val a = deg * PI.toFloat() / 180f
                val long = deg % 90 == 0
                val inner = r * if (long) 0.72f else 0.84f
                val from = Offset(c.x + inner * sin(a), c.y - inner * cos(a))
                val to = Offset(c.x + r * 0.94f * sin(a), c.y - r * 0.94f * cos(a))
                drawLine(if (deg == 0) Ink.live else if (long) Ink.dim else Ink.faint, from, to, strokeWidth = if (long) 3.dp.toPx() else 1.dp.toPx())
            }
        }
        // Where the glasses face.
        val tip = Offset(c.x, c.y - r * 0.6f)
        drawPath(Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(c.x - r * 0.1f, c.y)
            lineTo(c.x + r * 0.1f, c.y)
            close()
        }, Ink.warn)
        drawCircle(Ink.panel, r * 0.12f, c)
    }
}

/** One sensor's three axes over the last samples, with the latest values. */
@Composable
internal fun Trace(name: String, unit: String, trace: MotionTrace, ring: FloatArray, floor: Float) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(name, color = Ink.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            val i = ((trace.head - 1 + trace.size) % trace.size) * 3
            trace.frame
            if (trace.count > 0 && !ring[i].isNaN()) {
                Text("%.2f  %.2f  %.2f %s".format(ring[i], ring[i + 1], ring[i + 2], unit), color = Ink.dim, fontSize = 12.sp, fontFamily = Mono)
            } else {
                Text("-", color = Ink.faint, fontSize = 12.sp)
            }
        }
        Canvas(Modifier.fillMaxWidth().height(72.dp)) {
            trace.frame
            drawLine(Ink.line, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1f)
            if (trace.count < 2) return@Canvas
            var top = floor
            for (k in 0 until trace.count * 3) if (!ring[k].isNaN()) top = max(top, abs(ring[k]))
            listOf(Ink.x, Ink.y, Ink.z).forEachIndexed { axis, color -> axisLine(trace, ring, axis, top, color) }
        }
    }
}

private fun DrawScope.axisLine(trace: MotionTrace, ring: FloatArray, axis: Int, top: Float, color: Color) {
    val path = Path()
    var started = false
    val oldest = (trace.head - trace.count + trace.size) % trace.size
    for (n in 0 until trace.count) {
        val v = ring[((oldest + n) % trace.size) * 3 + axis]
        if (v.isNaN()) continue
        val x = size.width * n / (trace.size - 1)
        val y = size.height / 2 - (v / top) * (size.height / 2 - 2f)
        if (started) path.lineTo(x, y) else path.moveTo(x, y).also { started = true }
    }
    drawPath(path, color, style = Stroke(1.5.dp.toPx()))
}
