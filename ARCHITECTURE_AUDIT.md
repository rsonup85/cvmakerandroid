# Vistara Edit — Architecture Audit (Deliverable 3.1)

## 1. Executive Summary & Existing Architecture
Vistara Edit is built using modern Android architecture:
- **Language**: 100% Kotlin with Kotlin Coroutines and Flows.
- **UI Toolkit**: Jetpack Compose (Material 3 Dark Theme).
- **Playback & Export**: AndroidX Media3 (`1.5.1`) with `ExoPlayer` for real-time timeline preview and `media3-transformer` + `media3-effect` for GPU-accelerated video export.
- **Persistence**: Room Database (`2.7.0-alpha13`) with full project, track, item, keyframe, and media asset relational persistence.
- **State Management**: Android `ViewModel` (`EditorViewModel`, `HomeViewModel`) driving `StateFlow` and immutable `Project` state mutation with an undo/redo stack (`UndoRedoManager`) and crash-recovery draft autosave.

## 2. Component Equivalents Map
| Phase 3 Target Component | Existing Codebase Equivalent | Path |
|---|---|---|
| `TimelinePlayer` | `TimelinePlayer` | `com.example.editor.player.TimelinePlayer` |
| `PreviewCanvas` | `PreviewCanvas` | `com.example.ui.editor.preview.PreviewCanvas` |
| `TimelineView` | `TimelineView` | `com.example.ui.editor.timeline.TimelineView` |
| `TextLayerProperties` | `TextLayerProperties` | `com.example.domain.model.TimelineModels` |
| `TextLayerSheet` | `TextLayerSheet` | `com.example.ui.editor.sheets.EditorSheets` |
| `CompositionEngine` | `CompositionEngine` | `com.example.editor.composition.CompositionEngine` |
| `VideoExporter` | `VideoExporter` | `com.example.editor.export.VideoExporter` |
| `KeyframeSheet` | `KeyframeSheet` | `com.example.ui.editor.sheets.EditorSheets` |
| `ClipTransform` | `ClipTransform` | `com.example.domain.model.TimelineModels` |
| `KeyframeEvaluator` | `KeyframeEvaluator` | `com.example.editor.keyframe.KeyframeEvaluator` |

## 3. Findings & Performance Audit
1. **Scrubbing & Seek Storms (Deliverable 3.2)**:
   - *Current State*: `onSeek` in `TimelineView` directly invokes `viewModel.seekTo(time)`, which calls `TimelinePlayer.seekTo(timelineMs)`. During fast finger gestures, this issues dozens of `exoPlayer.seekTo()` requests per second, creating decode contention and audio glitches.
   - *Action*: Introduce `ScrubController` to throttle/coalesce seek events, maintain an LRU `TimelineFrameCache` for instantaneous cached frame rendering during scrubbing, and only fire precise player seeks on touch release.
2. **Multi-Touch Gestures (Deliverable 3.3 Part A)**:
   - *Current State*: `detectTransformGestures` modifies `activeClip.transform` continuously by compounding deltas: `currentT.scale * zoom`, `pan / width`. This induces floating point drift, jitter, and ±180° rotation discontinuities.
   - *Action*: Implement `TransformGestureState` tracking gesture initial transform + delta accumulator to calculate final transforms deterministically without cumulative error.
3. **Typography & Custom Fonts (Deliverable 3.3 Part B)**:
   - *Current State*: `EditorFont` hardcodes standard system fonts (`sans`, `serif`, `mono`, `cursive`, `bold`). Custom OTF/TTF loading from SAF or app assets is missing.
   - *Action*: Create `FontManager` supporting asset and user SAF fonts with persistent `fontId` and safe fallback.
4. **Speed Curves & Non-linear Playback (Deliverables 3.4 & 3.5)**:
   - *Current State*: Speed is represented as a single constant scalar (`0.1f..10.0f`).
   - *Action*: Build `SpeedCurve`, `SpeedPoint`, `SpeedCurveEvaluator`, and `TimeWarpMapper` to map timeline time to non-linear source media time consistently across preview and export.
5. **Waveforms (Deliverable 3.6)**:
   - *Current State*: Audio clips render a placeholder icon without acoustic peak data.
   - *Action*: Implement `AudioWaveformExtractor` (chunked `MediaExtractor` / `MediaCodec`), `WaveformCache`, and Canvas-based viewport-culled rendering in `TimelineView`.
6. **Color Grading & GPU Pipeline (Deliverables 3.7 & 3.8)**:
   - *Current State*: Transform properties lack color adjustments (brightness, contrast, saturation, temperature, tint, highlights, shadows).
   - *Action*: Build `ColorFilter` model, integrate Media3 `GlEffect` / custom GLSL shader for preview and export consistency, and provide LUT `.cube` parsing architecture.
7. **Keyframe Bezier/Easing Curves (Deliverable 3.9)**:
   - *Current State*: Basic keyframe evaluation supports 4 hardcoded easing types without interactive Bezier curve handle manipulation.
   - *Action*: Add `BezierCurve` and interactive graph editor in `KeyframeSheet`.
