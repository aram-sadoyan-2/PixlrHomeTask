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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.scale

class EnhancementViewModel(
    private val processor: ImageProcessor = BitmapEnhancer(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnhancementUiState())
    val uiState: StateFlow<EnhancementUiState> = _uiState.asStateFlow()

    private var processingJob: Job? = null

    fun loadUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val raw = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }.getOrNull()
            }
            if (raw == null) {
                _uiState.update { it.copy(errorMessage = "Failed to load image") }
                return@launch
            }
            val scaled = withContext(Dispatchers.Default) { scaleToBounds(raw, MAX_DISPLAY_PX) }
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

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val result = withContext(Dispatchers.Default) {
                runCatching { processor.process(source, _uiState.value.params) }
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
        private const val MAX_DISPLAY_PX = 1024

        private fun scaleToBounds(src: Bitmap, maxPx: Int): Bitmap {
            val w = src.width
            val h = src.height
            if (w <= maxPx && h <= maxPx) return src
            val scale = maxPx.toFloat() / maxOf(w, h)
            return src.scale((w * scale).toInt(), (h * scale).toInt())
        }
    }
}
