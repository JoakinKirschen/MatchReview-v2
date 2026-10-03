package be.matchreview.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Team logos are stored as small Base64 PNGs in the database, so they are included in
 * encrypted backups and PDF exports without depending on a gallery file that may move.
 */
object TeamLogoCodec {
    const val MAX_SIZE_PX = 256

    /** Reads, downscales and encodes the picked image. Call off the main thread. */
    fun encodeFromUri(context: Context, uri: Uri): String? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIZE_PX) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val longest = max(decoded.width, decoded.height)
        val scaled = if (longest > MAX_SIZE_PX) {
            val factor = MAX_SIZE_PX.toFloat() / longest
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * factor).toInt().coerceAtLeast(1),
                (decoded.height * factor).toInt().coerceAtLeast(1),
                true
            )
        } else decoded

        val bytes = ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    }.getOrNull()

    fun decode(logoPng: String?): Bitmap? {
        if (logoPng.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(logoPng, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}
