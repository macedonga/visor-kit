package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** Which sensor a [MotionSample] comes from. New sources come with new releases. */
@Serializable
@JvmInline
public value class MotionSource(public val id: String) {
    override fun toString(): String = id

    public companion object {
        /** The glasses' own sensors. */
        public val GLASSES: MotionSource = MotionSource("glasses")

        /** The Meta Neural Band's sensors. */
        public val BAND: MotionSource = MotionSource("band")
    }
}

/** A reading on three axes, in the sensor's frame, as Android reports it. */
@Poko
@Serializable
public class Vector3 @KitInternal constructor(
    public val x: Float,
    public val y: Float,
    public val z: Float,
)

/** An orientation as a unit quaternion. */
@Poko
@Serializable
public class Quaternion @KitInternal constructor(
    public val x: Float,
    public val y: Float,
    public val z: Float,
    public val w: Float,
)

/** One reading of the motion sensors. A sensor the glasses did not read this time is null. */
@Poko
@Serializable
public class MotionSample @KitInternal constructor(
    /** When the sensors read it, in nanoseconds on the glasses' own clock: only the differences mean something. */
    public val time: Long,
    /** Acceleration, gravity included, in m/s². */
    public val accelerometer: Vector3? = null,
    /** Rotation speed, in rad/s. */
    public val gyroscope: Vector3? = null,
    /** The magnetic field, in µT. */
    public val magnetometer: Vector3? = null,
    /** The orientation from the sensors combined. */
    public val orientation: Quaternion? = null,
    public val source: MotionSource = MotionSource.GLASSES,
)

/** The params of [Topics.GLASSES_MOTION] and [Topics.BAND_MOTION]. */
@Poko
@Serializable
public class MotionRequest @KitInternal constructor(
    /** Samples a second: one of [RATES]. */
    public val rateHz: Int = DEFAULT_RATE_HZ,
) {
    public companion object {
        public const val DEFAULT_RATE_HZ: Int = 30

        /** The rates the glasses sample at. */
        public val RATES: List<Int> = listOf(5, 10, 15, 24, 30, 60)
    }
}
