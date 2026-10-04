# Vistara Edit — Phase 3 Performance Baseline (Deliverable 3.1)

## 1. Environment & Runtime Specifications
- **Android Target**: Android 14+ (minSdk 24, compileSdk 36, targetSdk 36)
- **Media3 Framework**: AndroidX Media3 1.5.1
- **UI Architecture**: Jetpack Compose 1.7+ with Material Design 3
- **Concurrency**: Kotlin Coroutines 1.9+, Kotlin Flow, Android Architecture ViewModels

## 2. Baseline Measurements & Known Hotspots
| Metric / System | Baseline Status | Notes / Phase 3 Mitigation |
|---|---|---|
| Timeline Scrubbing | NOT MEASURED (Manual verification shows seek queueing) | Frequent seeking overburdens ExoPlayer decoder pipeline; mitigated by `ScrubController` coalescing + `TimelineFrameCache` |
| Preview Responsiveness | Responsive during steady playback; stutter during rapid multi-track scrub | Single player instance with frame extraction fallback |
| Multi-touch Canvas | Jitter & cumulative scale/rotation drift observed | Addressed by delta calculation anchored on gesture start transform |
| Font System | System fonts only | Addressed by `FontManager` with persistent IDs and safe fallback |
| Speed Engine | Constant speed only (0.1x to 10x) | Addressed by `SpeedCurveEvaluator` & `TimeWarpMapper` |
| Audio Waveforms | None (Icons only) | Chunked off-thread extraction + Canvas downsampled rendering |
| Color Grading | None | GPU GLSL shader pipeline matching preview and Transformer export |
| Memory Management | Bounded Room SQLite; ExoPlayer released in ViewModel `onCleared` | Add LRU bounds to frame cache and waveform cache |
| Export Pipeline | Media3 Transformer MP4 export with hardware-accelerated GL effects | Will incorporate unified speed curve and color shader pipeline |

## 3. Backward Compatibility Commitment
- Existing project files without `ColorFilter`, `SpeedCurve`, or custom `fontId` will deserialize transparently with default values.
- No breaking changes to existing Room schemas.
