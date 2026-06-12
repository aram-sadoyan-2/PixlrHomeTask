package com.armsd.pixrhometask.ui

import android.graphics.Bitmap
import com.armsd.pixrhometask.domain.ProcessingParams

data class EnhancementUiState(
    val sourceBitmap: Bitmap? = null,
    val processedBitmap: Bitmap? = null,
    val params: ProcessingParams = ProcessingParams.DEFAULT,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
)
