package com.armsd.pixrhometask.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armsd.pixrhometask.ui.EnhancementViewModel
import com.armsd.pixrhometask.ui.components.BeforeAfterSlider
import com.armsd.pixrhometask.ui.components.ControlPanel

@Composable
fun EnhancementScreen(
    viewModel: EnhancementViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pickerOpen by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        pickerOpen = false
        uri?.let { viewModel.loadUri(it, context.contentResolver) }
    }

    val launchPicker = {
        if (!pickerOpen) {
            pickerOpen = true
            imagePicker.launch("image/*")
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                val source = uiState.sourceBitmap
                val processed = uiState.processedBitmap

                when {
                    source == null -> EmptyState(onPickImage = { launchPicker() })
                    processed == null -> CircularProgressIndicator(color = Color.White)
                    else -> {
                        BeforeAfterSlider(
                            before = source,
                            after = processed,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (uiState.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,

                    )
                    .verticalScroll(rememberScrollState()),
            ) {
                ControlPanel(
                    params = uiState.params,
                    onBrightnessChange = viewModel::updateBrightness,
                    onContrastChange = viewModel::updateContrast,
                    onSharpnessChange = viewModel::updateSharpness,
                    onReset = viewModel::resetParams,
                )
                Button(
                    onClick = { launchPicker() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(onPickImage: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No image loaded",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Pick a photo to get started",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPickImage) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}
