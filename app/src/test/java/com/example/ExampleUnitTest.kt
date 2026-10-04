package com.example

import com.example.common.TimeUtils
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.ClipTransform
import com.example.domain.model.EditorFont
import com.example.domain.model.ExportQuality
import com.example.domain.model.ExportResolution
import com.example.domain.model.ExportSettings
import com.example.domain.model.ItemType
import com.example.domain.model.Keyframe
import com.example.domain.model.KeyframeInterpolation
import com.example.domain.model.KeyframeProperty
import com.example.domain.model.MediaAsset
import com.example.domain.model.MediaType
import com.example.domain.model.Project
import com.example.domain.model.TextLayerProperties
import com.example.domain.model.TimelineItem
import com.example.domain.model.Track
import com.example.domain.model.TrackType
import com.example.domain.model.TransitionConfig
import com.example.domain.model.TransitionType
import com.example.editor.composition.CompositionEngine
import com.example.editor.keyframe.KeyframeEvaluator
import com.example.editor.undo.UndoRedoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ExampleUnitTest {

    @Test
    fun testTimeUtilsFormatting() {
        assertEquals("00:05", TimeUtils.formatTimeShort(5000L))
        assertEquals("01:23", TimeUtils.formatTimeShort(83000L))
        assertEquals("00:01.24", TimeUtils.formatTimeDetailed(1240L))
        assertEquals("00:00.00", TimeUtils.formatTimeDetailed(0L))
        assertEquals("45s", TimeUtils.formatDurationHuman(45000L))
        assertEquals("1m 15s", TimeUtils.formatDurationHuman(75000L))
    }

    @Test
    fun testProjectDurationCalculation() {
        val clip1 = TimelineItem(
            id = "c1",
            trackId = "track_video_1",
            type = ItemType.VIDEO,
            timelineStartMs = 0L,
            durationMs = 5000L,
            sourceStartMs = 0L,
            sourceDurationMs = 5000L
        )
        val clip2 = TimelineItem(
            id = "c2",
            trackId = "track_video_1",
            type = ItemType.VIDEO,
            timelineStartMs = 5000L,
            durationMs = 3000L,
            sourceStartMs = 0L,
            sourceDurationMs = 3000L
        )
        val project = Project(
            id = "p1",
            name = "Test Project",
            items = listOf(clip1, clip2)
        )
        assertEquals(8000L, project.totalDurationMs)
        assertEquals(2, project.videoClips.size)
    }

    @Test
    fun testCustomSpeedCalculation() {
        val sourceDuration = 10000L
        val customSpeed = 0.83f
        val calculatedDuration = (sourceDuration / customSpeed).toLong()

        val clip = TimelineItem(
            id = "speed_clip",
            trackId = "track_video_1",
            type = ItemType.VIDEO,
            sourceStartMs = 0L,
            sourceDurationMs = sourceDuration,
            speed = customSpeed,
            durationMs = calculatedDuration
        )
        assertEquals(0.83f, clip.speed)
        assertEquals(12048L, clip.durationMs)
    }

    @Test
    fun testTransitionsAndFonts() {
        val transition = TransitionConfig(
            type = TransitionType.FADE,
            durationMs = 800L
        )
        assertEquals(TransitionType.FADE, transition.type)
        assertEquals(800L, transition.durationMs)

        assertEquals(EditorFont.SANS, EditorFont.fromId("sans"))
        assertEquals(EditorFont.SERIF, EditorFont.fromId("serif"))
        assertEquals(EditorFont.MONO, EditorFont.fromId("mono"))
        assertEquals(EditorFont.CURSIVE, EditorFont.fromId("cursive"))
        assertEquals(EditorFont.BOLD, EditorFont.fromId("bold"))
    }

    @Test
    fun testTextLayerProperties() {
        val textProps = TextLayerProperties(
            text = "Vistara Cinematic",
            fontFamily = "serif",
            fontSizeSp = 32f,
            colorHex = "#38BDF8",
            backgroundColorHex = "#99000000",
            hasShadow = true
        )
        assertEquals("Vistara Cinematic", textProps.text)
        assertEquals("serif", textProps.fontFamily)
        assertEquals(32f, textProps.fontSizeSp)
        assertEquals("#38BDF8", textProps.colorHex)
        assertTrue(textProps.hasShadow)
    }

    @Test
    fun testKeyframeInterpolation() {
        val kfs = listOf(
            Keyframe(id = "k1", property = KeyframeProperty.SCALE, timeMs = 0L, value = 1.0f, interpolation = KeyframeInterpolation.LINEAR),
            Keyframe(id = "k2", property = KeyframeProperty.SCALE, timeMs = 2000L, value = 2.0f, interpolation = KeyframeInterpolation.LINEAR)
        )

        // At midpoint 1000ms: expected value 1.5f
        val midVal = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.SCALE, 1000L, 1.0f)
        assertEquals(1.5f, midVal, 0.01f)

        // Before first keyframe: should clamp to first
        val beforeVal = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.SCALE, -100L, 1.0f)
        assertEquals(1.0f, beforeVal, 0.01f)

        // After last keyframe: should clamp to last
        val afterVal = KeyframeEvaluator.evaluate(kfs, KeyframeProperty.SCALE, 3000L, 1.0f)
        assertEquals(2.0f, afterVal, 0.01f)
    }

    @Test
    fun testCompositionEngineEvaluation() {
        val clip = TimelineItem(
            id = "c1",
            trackId = "track_video_1",
            type = ItemType.VIDEO,
            timelineStartMs = 0L,
            durationMs = 5000L,
            sourceStartMs = 0L,
            sourceDurationMs = 5000L,
            transform = ClipTransform(scale = 1.2f, rotationDegrees = 90),
            transition = TransitionConfig(type = TransitionType.FADE, durationMs = 1000L)
        )
        val text = TimelineItem(
            id = "t1",
            trackId = "track_text",
            type = ItemType.TEXT,
            timelineStartMs = 1000L,
            durationMs = 2000L,
            textProperties = TextLayerProperties(text = "Hello World")
        )

        val project = Project(
            id = "p1",
            name = "Comp Test",
            items = listOf(clip, text)
        )

        // At t=1500ms, text is active, clip is active, transition is NOT active (transition starts at 4000ms)
        val state1500 = CompositionEngine.evaluateAt(project, 1500L)
        assertNotNull(state1500.activeMainVideo)
        assertEquals(1, state1500.activeTexts.size)
        assertEquals("Hello World", state1500.activeTexts.first().textProperties?.text)
        assertEquals(1.2f, state1500.mainVideoTransform.scaleX, 0.01f)
        assertEquals(90f, state1500.mainVideoTransform.rotationDegrees, 0.01f)

        // At t=4500ms (500ms remaining in 1000ms transition), transition should be active
        val state4500 = CompositionEngine.evaluateAt(project, 4500L)
        assertNotNull(state4500.transition)
        assertEquals(TransitionType.FADE, state4500.transition?.type)
        assertEquals(0.5f, state4500.transition?.progress ?: 0f, 0.05f)
    }

    @Test
    fun testUndoRedoStackOperations() {
        val undoRedo = UndoRedoManager()
        val p1 = Project(id = "1", name = "State 1")
        val p2 = Project(id = "1", name = "State 2")
        val p3 = Project(id = "1", name = "State 3")

        assertFalse(undoRedo.canUndo())
        assertFalse(undoRedo.canRedo())

        undoRedo.pushState(p1)
        assertTrue(undoRedo.canUndo())

        undoRedo.pushState(p2)
        val undone = undoRedo.undo(p3)
        assertEquals("State 2", undone?.name)
        assertTrue(undoRedo.canRedo())

        val redone = undoRedo.redo(undone!!)
        assertEquals("State 3", redone?.name)
    }

    @Test
    fun testStressScenarioWith20PlusClips() {
        val items = mutableListOf<TimelineItem>()
        var curTimeline = 0L

        // Generate 25 video clips sequentially
        for (i in 1..25) {
            val duration = 2000L + (i * 100L)
            items.add(
                TimelineItem(
                    id = "clip_$i",
                    trackId = "track_video_1",
                    type = ItemType.VIDEO,
                    timelineStartMs = curTimeline,
                    durationMs = duration,
                    sourceStartMs = 0L,
                    sourceDurationMs = duration,
                    speed = 1.0f
                )
            )
            curTimeline += duration
        }

        // Add 5 overlay stickers and 5 text items
        for (i in 1..5) {
            items.add(
                TimelineItem(
                    id = "text_$i",
                    trackId = "track_text",
                    type = ItemType.TEXT,
                    timelineStartMs = (i * 4000L),
                    durationMs = 2500L,
                    textProperties = TextLayerProperties(text = "Title $i")
                )
            )
            items.add(
                TimelineItem(
                    id = "overlay_$i",
                    trackId = "track_image",
                    type = ItemType.IMAGE,
                    timelineStartMs = (i * 3500L),
                    durationMs = 3000L
                )
            )
        }

        val project = Project(
            id = "stress_test",
            name = "35 Items Multi-Track",
            items = items
        )

        assertEquals(25, project.videoClips.size)
        assertEquals(5, project.textLayers.size)
        assertEquals(5, project.imageLayers.size)
        assertEquals(35, project.items.size)
        assertTrue(project.totalDurationMs > 50000L)

        // Evaluate at various timestamps across the project
        val eval1 = CompositionEngine.evaluateAt(project, 5000L)
        assertNotNull(eval1.activeMainVideo)

        val evalMiddle = CompositionEngine.evaluateAt(project, project.totalDurationMs / 2)
        assertNotNull(evalMiddle.activeMainVideo)

        val evalEnd = CompositionEngine.evaluateAt(project, project.totalDurationMs - 100L)
        assertNotNull(evalEnd.activeMainVideo)
    }

    @Test
    fun testCanvasAspectRatios() {
        assertEquals("9:16", CanvasAspectRatio.RATIO_9_16.label)
        assertEquals(9f / 16f, CanvasAspectRatio.RATIO_9_16.ratio)
        assertEquals("16:9", CanvasAspectRatio.RATIO_16_9.label)
        assertEquals(16f / 9f, CanvasAspectRatio.RATIO_16_9.ratio)
        assertEquals(CanvasAspectRatio.RATIO_1_1, CanvasAspectRatio.fromName("RATIO_1_1"))
        assertEquals("21:9", CanvasAspectRatio.RATIO_21_9.label)
    }

    // --- Phase 3 Deliverable Unit Tests ---

    @Test
    fun testSpeedCurveEvaluator() {
        val points = listOf(
            com.example.domain.model.SpeedPoint(0.0f, 1.0f),
            com.example.domain.model.SpeedPoint(0.5f, 0.25f),
            com.example.domain.model.SpeedPoint(1.0f, 2.0f)
        )
        val curve = com.example.domain.model.SpeedCurve(name = "Test", points = points, isEnabled = true)

        // At start (0.0): 1.0x
        assertEquals(1.0f, com.example.editor.speed.SpeedCurveEvaluator.speedAt(curve, 0.0f), 0.01f)
        // At midpoint (0.5): 0.25x
        assertEquals(0.25f, com.example.editor.speed.SpeedCurveEvaluator.speedAt(curve, 0.5f), 0.01f)
        // At end (1.0): 2.0x
        assertEquals(2.0f, com.example.editor.speed.SpeedCurveEvaluator.speedAt(curve, 1.0f), 0.01f)

        // Out-of-bounds clamped
        assertEquals(1.0f, com.example.editor.speed.SpeedCurveEvaluator.speedAt(curve, -0.5f), 0.01f)
        assertEquals(2.0f, com.example.editor.speed.SpeedCurveEvaluator.speedAt(curve, 1.5f), 0.01f)
    }

    @Test
    fun testTimeWarpMapperMonotonicityAndBoundaries() {
        val heroCurve = com.example.domain.model.SpeedCurve.createPreset(com.example.domain.model.SpeedCurvePreset.HERO)
        val sourceDuration = 10000L
        val timelineDuration = com.example.editor.speed.TimeWarpMapper.calculateTimelineDurationMs(sourceDuration, heroCurve, 1.0f)

        assertTrue("Timeline duration should be positive", timelineDuration > 0L)

        // Boundary tests
        val sourceAtZero = com.example.editor.speed.TimeWarpMapper.timelineToSourceMs(0L, timelineDuration, 0L, sourceDuration, heroCurve)
        assertEquals(0L, sourceAtZero)

        val sourceAtEnd = com.example.editor.speed.TimeWarpMapper.timelineToSourceMs(timelineDuration, timelineDuration, 0L, sourceDuration, heroCurve)
        assertEquals(sourceDuration, sourceAtEnd)

        // Monotonic progression test
        var lastSourceTime = 0L
        for (step in 1..20) {
            val tOffset = (timelineDuration * (step / 20f)).toLong()
            val mappedSource = com.example.editor.speed.TimeWarpMapper.timelineToSourceMs(tOffset, timelineDuration, 0L, sourceDuration, heroCurve)
            assertTrue("Mapping must be monotonic: $mappedSource >= $lastSourceTime", mappedSource >= lastSourceTime)
            lastSourceTime = mappedSource
        }
    }

    @Test
    fun testKeyframeCurveInterpolator() {
        val easeInOut = com.example.domain.model.BezierCurve.EASE_IN_OUT

        // At progress 0: 0
        assertEquals(0.0f, com.example.editor.keyframe.KeyframeCurveInterpolator.evaluateBezier(easeInOut, 0.0f), 0.01f)
        // At progress 1: 1
        assertEquals(1.0f, com.example.editor.keyframe.KeyframeCurveInterpolator.evaluateBezier(easeInOut, 1.0f), 0.01f)
        // S-curve symmetry around midpoint
        val mid = com.example.editor.keyframe.KeyframeCurveInterpolator.evaluateBezier(easeInOut, 0.5f)
        assertEquals(0.5f, mid, 0.05f)

        // KeyframeEvaluator with BezierCurve
        val kf0 = Keyframe(id = "k0", property = KeyframeProperty.SCALE, timeMs = 0L, value = 1.0f, bezierCurve = easeInOut)
        val kf1 = Keyframe(id = "k1", property = KeyframeProperty.SCALE, timeMs = 1000L, value = 3.0f)
        val evaluated = KeyframeEvaluator.evaluate(listOf(kf0, kf1), KeyframeProperty.SCALE, 500L, 1.0f)
        assertEquals(2.0f, evaluated, 0.1f)
    }

    @Test
    fun testColorFilterAndProcessor() {
        val filter = com.example.domain.model.ColorFilter(
            brightness = 0.2f,
            contrast = 0.3f,
            saturation = 0.5f,
            temperature = 0.1f,
            tint = -0.1f,
            highlights = 0.1f,
            shadows = -0.1f
        )
        assertFalse(filter.isDefault)

        // Compose ColorMatrix generation
        val matrix = com.example.editor.color.ColorProcessor.createComposeColorMatrix(filter)
        assertNotNull(matrix)
        assertTrue(matrix.values.isNotEmpty())

        // Media3 Effects generation
        val effects = com.example.editor.color.ColorProcessor.createMedia3Effects(filter)
        assertTrue("Expected non-empty effects for adjusted color filter", effects.isNotEmpty())
    }

    @Test
    fun testLutParserCube() {
        val cubeContent = """
            # Sample Cube LUT for testing
            TITLE "Cinematic Teal"
            LUT_3D_SIZE 2
            0.0 0.0 0.0
            1.0 0.0 0.0
            0.0 1.0 0.0
            1.0 1.0 0.0
            0.0 0.0 1.0
            1.0 0.0 1.0
            0.0 1.0 1.0
            1.0 1.0 1.0
        """.trimIndent()

        val parsed = com.example.editor.color.LutParser.parseCube(
            java.io.ByteArrayInputStream(cubeContent.toByteArray()),
            id = "lut_1",
            name = "Cinematic Teal"
        )

        assertNotNull(parsed)
        assertEquals(2, parsed?.size)
        assertEquals(2 * 2 * 2 * 3, parsed?.tableData?.size)

        // Malformed content test: should safely return null without throwing
        val malformedContent = "NOT A VALID CUBE FILE"
        val malformedResult = com.example.editor.color.LutParser.parseCube(
            java.io.ByteArrayInputStream(malformedContent.toByteArray()),
            id = "lut_bad",
            name = "Bad"
        )
        assertEquals(null, malformedResult)
    }

    @Test
    fun testFontManagerFallback() {
        // Querying non-existent fontId should fall back smoothly without exception
        val fallbackTypeface = com.example.editor.font.FontManager.getTypeface(null, "non_existent_random_font_xyz")
        assertNotNull(fallbackTypeface)

        val serif = com.example.editor.font.FontManager.getTypeface(null, "serif")
        assertNotNull(serif)
    }

    @Test
    fun testTimelineFrameCacheLifecycle() {
        val cache = com.example.editor.player.TimelineFrameCache(10L * 1024L * 1024L)
        val bitmap = android.graphics.Bitmap.createBitmap(32, 32, android.graphics.Bitmap.Config.ARGB_8888)

        cache.putFrame("asset_1", 1000L, bitmap)
        val hit = cache.getFrame("asset_1", 1000L)
        assertNotNull(hit)

        // Nearest bucket lookup (within 300ms)
        val nearHit = cache.getFrame("asset_1", 1150L)
        assertNotNull(nearHit)

        cache.clear()
        val afterClear = cache.getFrame("asset_1", 1000L)
        assertEquals(null, afterClear)
    }
}
