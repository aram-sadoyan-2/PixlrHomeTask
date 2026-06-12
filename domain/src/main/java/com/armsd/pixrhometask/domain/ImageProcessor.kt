package com.armsd.pixrhometask.domain

import android.graphics.Bitmap

interface ImageProcessor {
    suspend fun process(source: Bitmap, params: ProcessingParams): Bitmap
}
