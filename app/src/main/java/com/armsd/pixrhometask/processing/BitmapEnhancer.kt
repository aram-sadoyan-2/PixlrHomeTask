package com.armsd.pixrhometask.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.armsd.pixrhometask.domain.ImageProcessor
import com.armsd.pixrhometask.domain.ProcessingParams
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class BitmapEnhancer : ImageProcessor {

    override suspend fun process(source: Bitmap, params: ProcessingParams): Bitmap {
        var current = applyColorMatrix(source, params)
        if (params.sharpness > 0f) {
            current = applySharpen(current, params.sharpness)
        }
        return current
    }

    // Single Canvas pass — avoids an intermediate bitmap per adjustment
    private fun applyColorMatrix(src: Bitmap, params: ProcessingParams): Bitmap {
        val combined = ColorMatrix()

        if (params.contrast != 1f) {
            val c = params.contrast
            val shift = 128f * (1f - c)
            combined.postConcat(ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, shift,
                0f, c, 0f, 0f, shift,
                0f, 0f, c, 0f, shift,
                0f, 0f, 0f, 1f, 0f,
            )))
        }

        if (params.brightness != 0f) {
            val b = params.brightness
            combined.postConcat(ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, b,
                0f, 1f, 0f, 0f, b,
                0f, 0f, 1f, 0f, b,
                0f, 0f, 0f, 1f, 0f,
            )))
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(combined)
        }
        val out = createBitmap(src.width, src.height)
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        return out
    }

    // getPixels/setPixels in bulk — reduces overhead from repeated JNI crossings.
    private suspend fun applySharpen(src: Bitmap, amount: Float): Bitmap {
        val w = src.width
        val h = src.height
        val srcPixels = IntArray(w * h)
        src.getPixels(srcPixels, 0, w, 0, 0, w, h)
        val dstPixels = IntArray(w * h)

        for (y in 0 until h) {
            currentCoroutineContext().ensureActive()
            for (x in 0 until w) {
                val center = srcPixels[y * w + x]
                val top    = srcPixels[y.dec().coerceAtLeast(0) * w + x]
                val bottom = srcPixels[y.inc().coerceAtMost(h - 1) * w + x]
                val left   = srcPixels[y * w + x.dec().coerceAtLeast(0)]
                val right  = srcPixels[y * w + x.inc().coerceAtMost(w - 1)]

                val sharpR = 5 * Color.red(center)   - Color.red(top)   - Color.red(bottom)   - Color.red(left)   - Color.red(right)
                val sharpG = 5 * Color.green(center) - Color.green(top) - Color.green(bottom) - Color.green(left) - Color.green(right)
                val sharpB = 5 * Color.blue(center)  - Color.blue(top)  - Color.blue(bottom)  - Color.blue(left)  - Color.blue(right)

                val r = (Color.red(center)   + amount * (sharpR - Color.red(center))).toInt().coerceIn(0, 255)
                val g = (Color.green(center) + amount * (sharpG - Color.green(center))).toInt().coerceIn(0, 255)
                val b = (Color.blue(center)  + amount * (sharpB - Color.blue(center))).toInt().coerceIn(0, 255)

                dstPixels[y * w + x] = Color.argb(Color.alpha(center), r, g, b)
            }
        }

        val out = createBitmap(w, h)
        out.setPixels(dstPixels, 0, w, 0, 0, w, h)
        return out
    }
}
