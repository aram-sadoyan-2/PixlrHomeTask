# Image Enhancer — PixlrHomeTask

A focused Android prototype that lets users load a photo and interactively enhance it for readability using a real-time before/after comparison view.

---

## 1. What product problem this solves

People frequently photograph documents, receipts, handwritten notes, labels, and whiteboards that come out washed-out, low-contrast, or soft. Opening those photos in a general-purpose viewer gives no tools to fix them. This feature provides a tight, purpose-built loop: load → adjust → compare, all on-device and in a few seconds.

---

## 2. User flow and rationale

```
App opens  →  Empty state with "Pick Image" CTA
     ↓
User taps +  →  System gallery picker
     ↓
Image loads and is immediately processed with default params
     ↓
Before/After comparison slider appears  (drag the divider left/right)
     ↓
User adjusts Brightness / Contrast / Sharpness sliders
     →  Background processing starts; stale result stays visible with a small spinner
     →  New result appears when ready
     ↓
Reset button returns all sliders to defaults
```

The split-slider pattern was chosen because it makes the effect of adjustments immediately tangible without requiring the user to switch screens or toggle a mode — the improvement is always visually anchored against the original.

---

## 3. Image-processing pipeline

All processing is CPU-only (no cloud, no NDK, no RenderScript) using Android's built-in `android.graphics` APIs.

### Step 0 — Scale to display size
On load, the source image is downsampled to a maximum of 1024 px on the longest side. This bounds memory usage regardless of camera megapixel count and keeps the convolution step responsive on typical devices.

### Step 1 — Brightness + Contrast  *(ColorMatrix, single pass)*
Both adjustments are expressed as `ColorMatrix` transformations and concatenated with `postConcat`, so both are applied in a single `Canvas.drawBitmap` call — no intermediate bitmap allocation.

- **Contrast** scales each channel around mid-grey (128): `v' = contrast × v + 128 × (1 − contrast)`
- **Brightness** adds a constant offset: `v' = v + brightness`

### Step 2 — Sharpening  *(3×3 Laplacian convolution)*
A Laplacian edge-enhancing kernel is applied via bulk `Bitmap.getPixels` / `setPixels`:

```
[ 0  -1   0 ]
[-1   5  -1 ]
[ 0  -1   0 ]
```

The sharpened result is blended back with the input by the `sharpness` factor, so `sharpness = 0` is a no-op and `sharpness = 1` applies the full kernel. Edge pixels use replication padding.

`getPixels` / `setPixels` are used instead of per-pixel `getPixel` / `setPixel` calls to reduce overhead from repeated JNI crossings.

---

## 4. Architecture

```
:domain  (Android library)
├── ProcessingParams.kt      # immutable value object
└── ImageProcessor.kt        # interface; implementations are swappable

:app
├── processing/
│   └── BitmapEnhancer.kt    # CPU implementation of ImageProcessor
└── ui/
    ├── EnhancementUiState.kt
    ├── EnhancementViewModel.kt
    ├── screen/EnhancementScreen.kt
    └── components/
        ├── BeforeAfterSlider.kt
        └── ControlPanel.kt
```

### Why a `:domain` module?

`:domain` is an Android library (not a pure JVM module) because `ImageProcessor` takes `Bitmap`, which is an Android class. The separation still has value: the processing contract is isolated from UI, and `BitmapEnhancer` can be replaced (e.g. with a GPU or ML implementation) without touching the ViewModel or screen. The module boundary is about separating the *contract* from the *implementation*, not achieving platform independence.

### State management

A single `StateFlow<EnhancementUiState>` in the ViewModel is the only source of truth. Each param change calls `triggerProcessing()`, which cancels the in-flight coroutine job before starting a new one. The sharpening loop checks `coroutineContext.ensureActive()` on each row, so cancellation actually stops the CPU work rather than just suppressing the result. Rapid slider movement never queues up stale work — the latest params always win without needing a debounce.

### Dispatcher split

- `Dispatchers.IO` — image decoding (`BitmapFactory.decodeStream`) is I/O-bound
- `Dispatchers.Default` — scaling and pixel convolution are CPU-bound

Image decoding, scaling, and processing are moved off the main thread.

### Before/After labels

Labels ("Before" / "After") are regular Compose `Text` composables overlaid in a `Box`. This keeps styling, accessibility, and theming in the Compose layer rather than delegating to native canvas text drawing.

### Image picker permissions

`ActivityResultContracts.GetContent()` grants temporary URI access to the chosen file without requiring `READ_MEDIA_IMAGES` or any broad storage permission.

---

## 5. Libraries used

| Library | Why |
|---|---|
| Jetpack Compose + Material 3 | Declarative UI |
| `lifecycle-viewmodel-compose` | Lifecycle-aware state collection in Compose |
| `kotlinx-coroutines-test` | ViewModel unit tests with controlled main dispatcher |

No third-party image-processing library was used. Adding OpenCV would be appropriate if the pipeline needed noise reduction, morphological ops, or perspective correction.

---

## 6. Trade-offs made in the timebox

- **No debounce on sliders.** Rapid drags cancel-and-restart processing. A 150 ms debounce would reduce wasted CPU cycles but adds complexity. At 1024 px this keeps the implementation simple while still feeling responsive for a prototype.
- **ColorMatrix chaining instead of a proper ICC pipeline.** The brightness + contrast combo behaves reasonably but doesn't account for perceptual gamma. A production tool would apply gamma-aware transforms.
- **No camera capture.** The image source is the gallery picker only.
- **Single-kernel sharpening.** A proper unsharp mask blurs first then subtracts. The Laplacian kernel is a close approximation but can clip highlights more aggressively.
- **No save / export.** The processed bitmap exists only in memory.

---

## 7. What I would improve for production

1. **GPU-accelerated processing** via AGSL (Android 13+) or `RenderEffect`. Convolution on a shader can be significantly faster and unlock real-time preview at higher resolutions.
2. **Debounce** slider changes (150–200 ms) to avoid churning on quick drags.
3. **Export to gallery** via `MediaStore.Images`.
4. **Histogram display** next to each slider to give users feedback on clipping.
5. **Additional filters**: auto-levels, perspective correction, deskew — high-value for document use cases.
6. **Instrumented tests** for `BitmapEnhancer` (known pixel values in, assert expected outputs).
7. **Accessibility**: content descriptions on all interactive elements, minimum touch target sizes verified.
8. **Handle EXIF orientation** during decoding.

---

## 8. Known limitations

- The sharpening convolution is O(w × h) on `Dispatchers.Default`. For images larger than 1024 px this can take several seconds on low-end devices.
- Very high contrast values (> 2.0) combined with high brightness cause channel clipping — expected behavior of the linear ColorMatrix model.
- No EXIF orientation handling: images shot in portrait on some devices may display rotated.
