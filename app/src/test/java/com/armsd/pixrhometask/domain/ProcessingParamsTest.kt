package com.armsd.pixrhometask.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessingParamsTest {

    @Test
    fun `default constructor produces neutral values`() {
        val params = ProcessingParams()
        assertEquals(0f, params.brightness, 0f)
        assertEquals(1f, params.contrast, 0f)
        assertEquals(0f, params.sharpness, 0f)
    }

    @Test
    fun `DEFAULT companion matches no-arg constructor`() {
        assertEquals(ProcessingParams(), ProcessingParams.DEFAULT)
    }

    @Test
    fun `copy changes only the specified field`() {
        val original = ProcessingParams()
        val modified = original.copy(brightness = 50f)
        assertEquals(50f, modified.brightness, 0f)
        assertEquals(original.contrast, modified.contrast, 0f)
        assertEquals(original.sharpness, modified.sharpness, 0f)
    }
}
