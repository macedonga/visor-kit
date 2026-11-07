package com.macedonga.visor.kit.cli

/**
 * The 7-byte ADTS header that makes raw AAC access units a playable .aac file, from the stream's
 * AudioSpecificConfig: ISO/IEC 14496-3, 1.A.2.
 */
internal class Adts private constructor(private val objectType: Int, private val rateIndex: Int, private val channels: Int) {
    fun header(payloadSize: Int): ByteArray {
        val length = HEADER + payloadSize
        return byteArrayOf(
            0xFF.toByte(),
            // MPEG-4, no CRC.
            0xF1.toByte(),
            (((objectType - 1) shl 6) or (rateIndex shl 2) or (channels shr 2)).toByte(),
            (((channels and 3) shl 6) or (length shr 11)).toByte(),
            ((length shr 3) and 0xFF).toByte(),
            (((length and 7) shl 5) or 0x1F).toByte(),
            0xFC.toByte(),
        )
    }

    companion object {
        private const val HEADER = 7

        /** Null when [asc] is not a plain AAC config, such as one with an explicit sample rate. */
        fun of(asc: ByteArray): Adts? {
            if (asc.size < 2) return null
            val first = asc[0].toInt() and 0xFF
            val second = asc[1].toInt() and 0xFF
            val objectType = first shr 3
            val rateIndex = ((first and 7) shl 1) or (second shr 7)
            val channels = (second shr 3) and 0xF
            if (objectType !in 1..4 || rateIndex > 12) return null
            return Adts(objectType, rateIndex, channels)
        }
    }
}
