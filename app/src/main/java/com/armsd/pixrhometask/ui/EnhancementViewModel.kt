package com.armsd.pixrhometask.ui

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armsd.pixrhometask.domain.ImageProcessor
import com.armsd.pixrhometask.processing.BitmapEnhancer
import com.armsd.pixrhometask.domain.ProcessingParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.scale
import kotlin.math.max

class EnhancementViewModel(
    private val processor: ImageProcessor = BitmapEnhancer(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnhancementUiState())
    val uiState: StateFlow<EnhancementUiState> = _uiState.asStateFlow()

    private var processingJob: Job? = null

    fun loadUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val decoded = withContext(Dispatchers.IO) {
                try {
                    decodeSampledBitmap(contentResolver, uri, MAX_DECODE_PX)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
            }
            if (decoded == null) {
                _uiState.update { it.copy(errorMessage = "Failed to load image") }
                return@launch
            }
            val scaled = withContext(Dispatchers.Default) { scaleToBounds(decoded, MAX_DISPLAY_PX) }
            if (scaled !== decoded) decoded.recycle()

            _uiState.update {
                it.copy(sourceBitmap = scaled, processedBitmap = null, errorMessage = null)
            }
            triggerProcessing()
        }
    }

    fun updateBrightness(value: Float) =
        updateParams(_uiState.value.params.copy(brightness = value))

    fun updateContrast(value: Float) =
        updateParams(_uiState.value.params.copy(contrast = value))


    fun updateSharpness(value: Float) =
        updateParams(_uiState.value.params.copy(sharpness = value))

    fun resetParams() =
        updateParams(ProcessingParams.DEFAULT)

    fun dismissError() =
        _uiState.update { it.copy(errorMessage = null) }

    private fun updateParams(params: ProcessingParams) {
        _uiState.update { it.copy(params = params) }
        triggerProcessing()
    }

    private fun triggerProcessing() {
        val source = _uiState.value.sourceBitmap ?: return
        val params = _uiState.value.params

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val result = withContext(Dispatchers.Default) {
                runCatching { processor.process(source, params) }
                    .onFailure { if (it is CancellationException) throw it }
            }
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    processedBitmap = result.getOrElse { _ -> it.processedBitmap },
                    errorMessage = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    companion object {
        // The app processes a bounded preview bitmap instead of the full-resolution source image.
        // This keeps memory usage predictable for large camera photos and panoramas.
        private const val MAX_DISPLAY_PX = 1024
        private const val MAX_DECODE_PX = MAX_DISPLAY_PX * 2  // 2048

        private fun scaleToBounds(src: Bitmap, maxPx: Int): Bitmap {
            val w = src.width
            val h = src.height
            if (w <= maxPx && h <= maxPx) return src
            val scale = maxPx.toFloat() / maxOf(w, h)
            return src.scale((w * scale).toInt(), (h * scale).toInt())
        }

        private fun decodeSampledBitmap(
            contentResolver: ContentResolver,
            uri: Uri,
            maxPx: Int,
        ): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, maxPx)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            return contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        }

        private fun calculateInSampleSize(opts: BitmapFactory.Options, maxPx: Int): Int {
            val rawLong = max(opts.outWidth, opts.outHeight)
            if (rawLong <= 0 || maxPx <= 0) return 1
            var sampleSize = 1
            while (rawLong / sampleSize > maxPx) {
                sampleSize *= 2
            }
            return sampleSize
        }
    }
}
