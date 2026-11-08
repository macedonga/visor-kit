package com.macedonga.visor.kit

import android.graphics.Bitmap
import android.util.Base64
import com.macedonga.visor.kit.protocol.LayoutBox
import com.macedonga.visor.kit.protocol.LayoutImage
import java.io.ByteArrayOutputStream

/**
 * [bitmap] as an image of the layout, sent inside it: scaled to the Display, at most 600 pixels a side, as a JPEG,
 * or a PNG when it has transparency. A layout or a card holds a few such images; past that, give `https://` URLs.
 */
public fun LayoutBox.image(bitmap: Bitmap) {
    image(bitmap) {}
}

public fun LayoutBox.image(bitmap: Bitmap, block: LayoutImage.() -> Unit) {
    image(dataUri(bitmap), block)
}

internal fun dataUri(bitmap: Bitmap): String {
    val side = maxOf(bitmap.width, bitmap.height)
    val scaled = if (side > MAX_SIDE) {
        Bitmap.createScaledBitmap(bitmap, (bitmap.width * MAX_SIDE / side).coerceAtLeast(1), (bitmap.height * MAX_SIDE / side).coerceAtLeast(1), true)
    } else {
        bitmap
    }
    val png = scaled.hasAlpha()
    val bytes = ByteArrayOutputStream().also {
        scaled.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, if (png) 100 else JPEG_QUALITY, it)
    }.toByteArray()
    if (scaled !== bitmap) scaled.recycle()
    return "data:image/${if (png) "png" else "jpeg"};base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}

// The Display draws an image at most 600 pixels a side.
private const val MAX_SIDE = 600
private const val JPEG_QUALITY = 85
