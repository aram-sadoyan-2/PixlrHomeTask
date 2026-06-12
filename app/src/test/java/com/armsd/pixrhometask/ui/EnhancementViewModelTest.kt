package com.armsd.pixrhometask.ui

import android.graphics.Bitmap
import com.armsd.pixrhometask.domain.ImageProcessor
import com.armsd.pixrhometask.domain.ProcessingParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EnhancementViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeImageProcessor : ImageProcessor {
        var callCount = 0
        override suspend fun process(source: Bitmap, params: ProcessingParams): Bitmap {
            callCount++
            return source
        }
    }

    private lateinit var processor: FakeImageProcessor
    private lateinit var viewModel: EnhancementViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        processor = FakeImageProcessor()
        viewModel = EnhancementViewModel(processor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty with default params`() {
        val state = viewModel.uiState.value
        assertEquals(ProcessingParams.DEFAULT, state.params)
        assertNull(state.sourceBitmap)
        assertNull(state.processedBitmap)
        assertFalse(state.isProcessing)
        assertNull(state.errorMessage)
    }

    @Test
    fun `updateBrightness updates brightness only`() {
        viewModel.updateBrightness(75f)
        val params = viewModel.uiState.value.params
        assertEquals(75f, params.brightness, 0f)
        assertEquals(1f, params.contrast, 0f)
        assertEquals(0f, params.sharpness, 0f)
    }

    @Test
    fun `updateContrast updates contrast`() {
        viewModel.updateContrast(2f)
        assertEquals(2f, viewModel.uiState.value.params.contrast, 0f)
    }

    @Test
    fun `updateSharpness updates sharpness`() {
        viewModel.updateSharpness(0.8f)
        assertEquals(0.8f, viewModel.uiState.value.params.sharpness, 0f)
    }

    @Test
    fun `resetParams restores default params`() {
        viewModel.updateBrightness(50f)
        viewModel.updateContrast(2f)
        viewModel.updateSharpness(1f)
        viewModel.resetParams()
        assertEquals(ProcessingParams.DEFAULT, viewModel.uiState.value.params)
    }

    @Test
    fun `param update without source bitmap does not invoke processor`() {
        viewModel.updateBrightness(10f)
        viewModel.updateContrast(2f)
        viewModel.updateSharpness(0.5f)
        assertEquals(0, processor.callCount)
    }
}
