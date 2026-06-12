package com.armsd.pixrhometask.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BeforeAfterSlider(
    before: Bitmap,
    after: Bitmap,
    modifier: Modifier = Modifier,
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }

    val beforeImage = remember(before) { before.asImageBitmap() }
    val afterImage = remember(after) { after.asImageBitmap() }

    val labelStyle = TextStyle(
        color = Color.White,
        fontSize = 12.sp,
        shadow = Shadow(color = Color.Black, blurRadius = 4f),
    )

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        splitFraction = (splitFraction + dragAmount / size.width).coerceIn(0f, 1f)
                    }
                }
        ) {
            val dividerX = size.width * splitFraction
            val canvasW = size.width.toInt()
            val canvasH = size.height.toInt()
            val (ox, oy, dw, dh) = fitCenter(beforeImage.width, beforeImage.height, canvasW, canvasH)

            clipPath(Path().apply { addRect(Rect(0f, 0f, dividerX, size.height)) }) {
                drawImage(beforeImage, dstOffset = IntOffset(ox, oy), dstSize = IntSize(dw, dh))
            }

            clipPath(
                path = Path().apply { addRect(Rect(dividerX, 0f, size.width, size.height)) },
                clipOp = ClipOp.Intersect,
            ) {
                drawImage(afterImage, dstOffset = IntOffset(ox, oy), dstSize = IntSize(dw, dh))
            }

            drawLine(
                color = Color.White,
                start = Offset(dividerX, 0f),
                end = Offset(dividerX, size.height),
                strokeWidth = 2.dp.toPx(),
            )

            val handleCenter = Offset(dividerX, size.height / 2f)
            val handleR = 18.dp.toPx()
            drawCircle(color = Color.White, radius = handleR, center = handleCenter)
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = handleR - 2.dp.toPx(),
                center = handleCenter,
            )
        }

        Text(
            text = "Before",
            style = labelStyle,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
        Text(
            text = "After",
            style = labelStyle,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        )
    }
}

private fun fitCenter(srcW: Int, srcH: Int, dstW: Int, dstH: Int): IntArray {
    val scale = minOf(dstW.toFloat() / srcW, dstH.toFloat() / srcH)
    val sw = (srcW * scale).toInt()
    val sh = (srcH * scale).toInt()
    return intArrayOf((dstW - sw) / 2, (dstH - sh) / 2, sw, sh)
}
