package com.armsd.pixrhometask.domain

data class ProcessingParams(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val sharpness: Float = 0f,
) {
    companion object {
        val DEFAULT = ProcessingParams()
    }
}
