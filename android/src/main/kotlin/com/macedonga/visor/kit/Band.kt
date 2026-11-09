package com.macedonga.visor.kit

import com.macedonga.visor.kit.protocol.BandStatus
import com.macedonga.visor.kit.protocol.BuzzRequest
import com.macedonga.visor.kit.protocol.Gesture
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.MotionRequest
import com.macedonga.visor.kit.protocol.MotionSample
import com.macedonga.visor.kit.protocol.Scope
import com.macedonga.visor.kit.protocol.Topics
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

/** The Meta Neural Band paired with the Display. Needs [Scope.BAND]. */
public class Band internal constructor(private val visor: Visor) {
    /** The status now, then each change. */
    public val status: Flow<BandStatus>
        get() = visor.subscribe(Topics.BAND_STATUS, Unit)

    /**
     * Every gesture the band reads, while collected. The Display gets each one too and acts on it as usual. A band
     * the person shares with the phone ([BandStatus.shared]) gives them straight from its link.
     */
    public val gestures: Flow<Gesture>
        get() = visor.subscribe(Topics.BAND_GESTURES, Unit)

    /**
     * The band's motion sensors on the wrist, 30 samples a second, while collected. Needs [Scope.MOTION] and a band
     * the person shares with the phone ([BandStatus.shared]).
     */
    public val motion: Flow<MotionSample>
        get() = visor.subscribe(Topics.BAND_MOTION, MotionRequest())

    /** [motion] at [rateHz] samples a second, one of [MotionRequest.RATES]. */
    public fun motion(rateHz: Int): Flow<MotionSample> = visor.subscribe(Topics.BAND_MOTION, MotionRequest(rateHz))

    /** Vibrates the band once, long and strong enough that the wearer notices. */
    public suspend fun buzz(): Unit = visor.call(Methods.BAND_BUZZ, BuzzRequest())

    /**
     * Vibrates the band for [duration], from 10 ms to 1 s, at [strength], from just above 0 to 1.
     *
     * @throws IllegalArgumentException when either is out of range.
     */
    public suspend fun buzz(duration: Duration, strength: Float) {
        val millis = duration.inWholeMilliseconds
        require(millis in BuzzRequest.MIN_DURATION_MS..BuzzRequest.MAX_DURATION_MS) { "duration must be 10 ms to 1 s, was $duration" }
        require(strength > 0f && strength <= 1f) { "strength must be above 0 and at most 1, was $strength" }
        val amplitude = (strength * BuzzRequest.MAX_STRENGTH).roundToInt().coerceAtLeast(1)
        visor.call(Methods.BAND_BUZZ, BuzzRequest(millis.toInt(), amplitude))
    }
}
