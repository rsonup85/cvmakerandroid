// FIXED: Added missing R import + cleaned up unused imports
package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.ProjectRepository
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.ClipTransform
import com.example.domain.model.ExportSettings
import com.example.domain.model.ImageLayerProperties
import com.example.domain.model.ItemType
import com.example.domain.model.Keyframe
import com.example.domain.model.KeyframeInterpolation
import com.example.domain.model.KeyframeProperty
import com.example.domain.model.MediaAsset
import com.example.domain.model.MediaType
import com.example.domain.model.Project
import com.example.domain.model.TextLayerProperties
import com.example.domain.model.TimelineItem
import com.example.domain.model.TransitionConfig
import com.example.domain.model.TransitionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ProjectRepository(db, context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Vistara Edit", appName)
    }

    @Test
    fun `save and load project from repository`() = runBlocking {
        val projectId = UUID.randomUUID().toString()
        val assetId = UUID.randomUUID().toString()
        val asset = MediaAsset(
            id = assetId,
            uriString = "content://media/external/video/media/1",
            fileName = "sample_clip.mp4",
            mediaType = MediaType.VIDEO,
            durationMs = 6000L,
            width = 1920,
            height = 1080
        )
        val item = TimelineItem(
            id = UUID.randomUUID().toString(),
            trackId = "track_video_1",
            assetId = assetId,
            type = ItemType.VIDEO,
            timelineStartMs = 0L,
            durationMs = 6000L,
            sourceStartMs = 0L,
            sourceDurationMs = 6000L,
            speed = 1.0f,
            transform = ClipTransform(rotationDegrees = 90)
        )
        val project = Project(
            id = projectId,
            name = "Vistara Summer Reel",
            canvasRatio = CanvasAspectRatio.RATIO_9_16,
            exportSettings = ExportSettings(),
            items = listOf(item),
            assets = listOf(asset)
        )

        repository.saveProject(project)

        val loaded = repository.getProject(projectId)
        assertNotNull(loaded)
        assertEquals("Vistara Summer Reel", loaded?.name)
        assertEquals(CanvasAspectRatio.RATIO_9_16, loaded?.canvasRatio)
        assertEquals(1, loaded?.assets?.size)
        assertEquals(1, loaded?.items?.size)
        assertEquals(90, loaded?.items?.first()?.transform?.rotationDegrees)
        assertEquals(6000L, loaded?.totalDurationMs)
    }

    @Test
    fun `save and load multi-track project with keyframes and transitions`() = runBlocking {
        val projectId = UUID.randomUUID().toString()
        val videoAssetId = UUID.randomUUID().toString()
        val imageAssetId = UUID.randomUUID().toString()

        val videoAsset = MediaAsset(
            id = videoAssetId,
            uriString = "file:///storage/video.mp4",
            fileName = "main_video.mp4",
            mediaType = MediaType.VIDEO,
            durationMs = 8000L
        )
        val imageAsset = MediaAsset(
            id = imageAssetId,
            uriString = "file:///storage/logo.png",
            fileName = "logo.png",
            mediaType = MediaType.IMAGE,
            durationMs = 3000L
        )

        val keyframe1 = Keyframe(
            id = "kf_1",
            property = KeyframeProperty.SCALE,
            timeMs = 0L,
            value = 1.0f,
            interpolation = KeyframeInterpolation.LINEAR
        )
        val keyframe2 = Keyframe(
            id = "kf_2",
            property = KeyframeProperty.SCALE,
            timeMs = 2000L,
            value = 1.5f,
            interpolation = KeyframeInterpolation.EASE_IN_OUT
        )

        val videoClip = TimelineItem(
            id = "clip_1",
            trackId = "track_video_1",
            assetId = videoAssetId,
            type = ItemType.VIDEO,
            timelineStartMs = 0L,
            durationMs = 5000L,
            sourceStartMs = 1000L,
            sourceDurationMs = 4000L,
            speed = 0.8f,
            transition = TransitionConfig(type = TransitionType.FADE, durationMs = 600L),
            keyframes = listOf(keyframe1, keyframe2)
        )
        val textLayer = TimelineItem(
            id = "text_1",
            trackId = "track_text",
            type = ItemType.TEXT,
            timelineStartMs = 1500L,
            durationMs = 2500L,
            textProperties = TextLayerProperties(
                text = "Cinematic Shot",
                fontFamily = "serif",
                colorHex = "#38BDF8",
                hasShadow = true
            )
        )
        val imageOverlay = TimelineItem(
            id = "overlay_1",
            trackId = "track_image",
            assetId = imageAssetId,
            type = ItemType.IMAGE,
            timelineStartMs = 1000L,
            durationMs = 3000L,
            imageProperties = ImageLayerProperties(
                scale = 1.25f,
                opacity = 0.9f
            )
        )

        val project = Project(
            id = projectId,
            name = "Advanced Multi-Track Project",
            canvasRatio = CanvasAspectRatio.RATIO_16_9,
            canvasBackgroundColorHex = "#16161D",
            isSnapEnabled = true,
            items = listOf(videoClip, textLayer, imageOverlay),
            assets = listOf(videoAsset, imageAsset)
        )

        repository.saveProject(project)

        val loaded = repository.getProject(projectId)
        assertNotNull(loaded)
        assertEquals("Advanced Multi-Track Project", loaded?.name)
        assertEquals("#16161D", loaded?.canvasBackgroundColorHex)
        assertTrue(loaded?.isSnapEnabled == true)
        assertEquals(3, loaded?.items?.size)

        val loadedVideo = loaded?.items?.find { it.id == "clip_1" }
        assertNotNull(loadedVideo)
        assertEquals(0.8f, loadedVideo?.speed)
        assertEquals(TransitionType.FADE, loadedVideo?.transition?.type)
        assertEquals(2, loadedVideo?.keyframes?.size)
        assertEquals(KeyframeProperty.SCALE, loadedVideo?.keyframes?.first()?.property)

        val loadedText = loaded?.items?.find { it.id == "text_1" }
        assertNotNull(loadedText)
        assertEquals("Cinematic Shot", loadedText?.textProperties?.text)
        assertEquals("serif", loadedText?.textProperties?.fontFamily)

        val loadedOverlay = loaded?.items?.find { it.id == "overlay_1" }
        assertNotNull(loadedOverlay)
        assertEquals(1.25f, loadedOverlay?.imageProperties?.scale)
    }

    @Test
    fun `duplicate and delete project`() = runBlocking {
        val projectId = UUID.randomUUID().toString()
        val project = Project(
            id = projectId,
            name = "Reel 1"
        )
        repository.saveProject(project)

        val duplicated = repository.duplicateProject(projectId)
        assertNotNull(duplicated)
        assertEquals("Reel 1 (Copy)", duplicated?.name)

        repository.deleteProject(projectId)
        val loadedOriginal = repository.getProject(projectId)
        assertNull(loadedOriginal)

        val loadedCopy = repository.getProject(duplicated!!.id)
        assertNotNull(loadedCopy)
    }

    @Test
    fun `test crash recovery persistence`() {
        val project = Project(id = "rec_1", name = "Recovery Test")
        repository.saveDraftRecovery(project)

        assertEquals("rec_1", repository.getPendingRecoveryProjectId())
        assertEquals("Recovery Test", repository.getPendingRecoveryProjectName())

        repository.clearDraftRecovery()
        assertNull(repository.getPendingRecoveryProjectId())
        assertNull(repository.getPendingRecoveryProjectName())
    }
}