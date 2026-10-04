// FIXED: Added ColorFilter + SpeedCurve serialization, aligned track names
package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.MediaAssetEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TimelineItemEntity
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.ClipTransform
import com.example.domain.model.ColorFilter
import com.example.domain.model.ExportQuality
import com.example.domain.model.ExportResolution
import com.example.domain.model.ExportSettings
import com.example.domain.model.ImageLayerProperties
import com.example.domain.model.ItemType
import com.example.domain.model.Keyframe
import com.example.domain.model.KeyframeInterpolation
import com.example.domain.model.KeyframeProperty
import com.example.domain.model.MediaAsset
import com.example.domain.model.MediaType
import com.example.domain.model.Project
import com.example.domain.model.SpeedCurve
import com.example.domain.model.SpeedCurvePreset
import com.example.domain.model.SpeedPoint
import com.example.domain.model.TextLayerProperties
import com.example.domain.model.TimelineItem
import com.example.domain.model.Track
import com.example.domain.model.TrackType
import com.example.domain.model.TransitionConfig
import com.example.domain.model.TransitionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ProjectRepository(
    private val database: AppDatabase,
    private val context: Context? = null
) {
    private val projectDao = database.projectDao()
    private val mediaAssetDao = database.mediaAssetDao()
    private val timelineItemDao = database.timelineItemDao()

    fun getAllProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }

    suspend fun getProject(projectId: String): Project? = withContext(Dispatchers.IO) {
        val projectEntity = projectDao.getProjectById(projectId) ?: return@withContext null
        val assetEntities = mediaAssetDao.getAssetsForProject(projectId)
        val itemEntities = timelineItemDao.getItemsForProject(projectId)

        val assets = assetEntities.map { it.toDomain() }
        val items = itemEntities.map { it.toDomain() }

        val tracks = deserializeTracks(projectEntity.tracksJson)

        projectEntity.toDomain(assets = assets, items = items, tracks = tracks)
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val projectEntity = project.toEntity()
        val assetEntities = project.assets.map { it.toEntity(project.id) }
        val itemEntities = project.items.map { it.toEntity(project.id) }

        projectDao.insertProject(projectEntity)
        mediaAssetDao.deleteAssetsForProject(project.id)
        if (assetEntities.isNotEmpty()) {
            mediaAssetDao.insertAssets(assetEntities)
        }
        timelineItemDao.deleteItemsForProject(project.id)
        if (itemEntities.isNotEmpty()) {
            timelineItemDao.insertItems(itemEntities)
        }
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(projectId)
        mediaAssetDao.deleteAssetsForProject(projectId)
        timelineItemDao.deleteItemsForProject(projectId)
    }

    suspend fun renameProject(projectId: String, newName: String) = withContext(Dispatchers.IO) {
        val existing = projectDao.getProjectById(projectId) ?: return@withContext
        projectDao.updateProject(existing.copy(name = newName, updatedAt = System.currentTimeMillis()))
    }

    suspend fun duplicateProject(projectId: String): Project? = withContext(Dispatchers.IO) {
        val original = getProject(projectId) ?: return@withContext null
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val cloned = original.copy(
            id = newId,
            name = "${original.name} (Copy)",
            createdAt = now,
            updatedAt = now,
            items = original.items.map { it.copy(id = UUID.randomUUID().toString()) }
        )
        saveProject(cloned)
        cloned
    }

    // --- Crash Recovery & Autosave Support ---
    fun saveDraftRecovery(project: Project) {
        if (context == null) return
        try {
            val prefs = context.getSharedPreferences("vistara_recovery", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("active_project_id", project.id)
                .putString("active_project_name", project.name)
                .putLong("saved_timestamp", System.currentTimeMillis())
                .apply()
        } catch (_: Exception) {}
    }

    fun clearDraftRecovery() {
        if (context == null) return
        try {
            val prefs = context.getSharedPreferences("vistara_recovery", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (_: Exception) {}
    }

    fun getPendingRecoveryProjectId(): String? {
        if (context == null) return null
        val prefs = context.getSharedPreferences("vistara_recovery", Context.MODE_PRIVATE)
        return prefs.getString("active_project_id", null)
    }

    fun getPendingRecoveryProjectName(): String? {
        if (context == null) return null
        val prefs = context.getSharedPreferences("vistara_recovery", Context.MODE_PRIVATE)
        return prefs.getString("active_project_name", null)
    }

    // --- Entity Mappers ---

    private fun ProjectEntity.toDomain(
        assets: List<MediaAsset> = emptyList(),
        items: List<TimelineItem> = emptyList(),
        tracks: List<Track> = defaultTracks()
    ): Project {
        return Project(
            id = id,
            name = name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            canvasRatio = CanvasAspectRatio.fromName(canvasRatio),
            canvasBackgroundColorHex = canvasBackgroundColorHex,
            isSnapEnabled = isSnapEnabled,
            exportSettings = ExportSettings(
                resolution = runCatching { ExportResolution.valueOf(exportResolution) }
                    .getOrDefault(ExportResolution.RES_1080P),
                fps = exportFps,
                quality = runCatching { ExportQuality.valueOf(exportQuality) }
                    .getOrDefault(ExportQuality.MEDIUM)
            ),
            tracks = if (tracks.isNotEmpty()) tracks else defaultTracks(),
            items = items,
            assets = assets
        )
    }

    private fun Project.toEntity(): ProjectEntity {
        return ProjectEntity(
            id = id,
            name = name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            canvasRatio = canvasRatio.name,
            canvasBackgroundColorHex = canvasBackgroundColorHex,
            isSnapEnabled = isSnapEnabled,
            exportResolution = exportSettings.resolution.name,
            exportFps = exportSettings.fps,
            exportQuality = exportSettings.quality.name,
            tracksJson = serializeTracks(tracks)
        )
    }

    private fun MediaAssetEntity.toDomain(): MediaAsset {
        return MediaAsset(
            id = id,
            uriString = uriString,
            fileName = fileName,
            mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.VIDEO),
            durationMs = durationMs,
            width = width,
            height = height,
            rotationDegrees = rotationDegrees,
            fileSizeBytes = fileSizeBytes,
            fps = fps,
            thumbnailPath = thumbnailPath
        )
    }

    private fun MediaAsset.toEntity(projectId: String): MediaAssetEntity {
        return MediaAssetEntity(
            id = id,
            projectId = projectId,
            uriString = uriString,
            fileName = fileName,
            mediaType = mediaType.name,
            durationMs = durationMs,
            width = width,
            height = height,
            rotationDegrees = rotationDegrees,
            fileSizeBytes = fileSizeBytes,
            fps = fps,
            thumbnailPath = thumbnailPath
        )
    }

    private fun TimelineItemEntity.toDomain(): TimelineItem {
        val parsedType = runCatching { ItemType.valueOf(type) }.getOrDefault(ItemType.VIDEO)
        val textProps = if (parsedType == ItemType.TEXT) {
            TextLayerProperties(
                text = text ?: "Text",
                fontFamily = textFontFamily ?: "sans",
                fontSizeSp = textFontSizeSp ?: 24f,
                colorHex = textColorHex ?: "#FFFFFF",
                backgroundColorHex = textBackgroundColorHex,
                strokeColorHex = textStrokeColorHex,
                strokeWidth = textStrokeWidth ?: 0f,
                hasShadow = textHasShadow ?: false,
                alignment = textAlignment ?: "CENTER",
                positionX = textPositionX ?: 0.5f,
                positionY = textPositionY ?: 0.5f,
                scale = textScale ?: 1.0f,
                rotationDegrees = textRotationDegrees ?: 0.0f,
                opacity = textOpacity ?: 1.0f
            )
        } else null

        val imageProps = if (parsedType == ItemType.IMAGE) {
            ImageLayerProperties(
                positionX = imagePositionX ?: 0.5f,
                positionY = imagePositionY ?: 0.5f,
                scale = imageScale ?: 1.0f,
                rotationDegrees = imageRotationDegrees ?: 0.0f,
                opacity = imageOpacity ?: 1.0f
            )
        } else null

        val keyframes = deserializeKeyframes(keyframesData)
        // FIXED: Parse colorFilter + speedCurve
        val colorFilter = deserializeColorFilter(colorFilterData)
        val speedCurve = deserializeSpeedCurve(speedCurveData)

        return TimelineItem(
            id = id,
            trackId = trackId,
            assetId = assetId,
            name = name,
            type = parsedType,
            timelineStartMs = timelineStartMs,
            durationMs = durationMs,
            sourceStartMs = sourceStartMs,
            sourceDurationMs = sourceDurationMs,
            speed = speed,
            volume = volume,
            isMuted = isMuted,
            fadeInMs = fadeInMs,
            fadeOutMs = fadeOutMs,
            isFrozen = isFrozen,
            isReversed = isReversed,
            transform = ClipTransform(
                rotationDegrees = rotationDegrees,
                flipHorizontal = flipHorizontal,
                flipVertical = flipVertical,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                cropLeft = cropLeft,
                cropTop = cropTop,
                cropRight = cropRight,
                cropBottom = cropBottom,
                opacity = opacity
            ),
            transition = TransitionConfig(
                type = runCatching { TransitionType.valueOf(transitionType) }
                    .getOrDefault(TransitionType.NONE),
                durationMs = transitionDurationMs,
                fromClipId = fromClipId,
                toClipId = toClipId,
                easing = transitionEasing
            ),
            textProperties = textProps,
            imageProperties = imageProps,
            keyframes = keyframes,
            colorFilter = colorFilter,
            speedCurve = speedCurve
        )
    }

    private fun TimelineItem.toEntity(projectId: String): TimelineItemEntity {
        return TimelineItemEntity(
            id = id,
            projectId = projectId,
            trackId = trackId,
            assetId = assetId,
            name = name,
            type = type.name,
            timelineStartMs = timelineStartMs,
            durationMs = durationMs,
            sourceStartMs = sourceStartMs,
            sourceDurationMs = sourceDurationMs,
            speed = speed,
            volume = volume,
            isMuted = isMuted,
            fadeInMs = fadeInMs,
            fadeOutMs = fadeOutMs,
            isFrozen = isFrozen,
            isReversed = isReversed,
            rotationDegrees = transform.rotationDegrees,
            flipHorizontal = transform.flipHorizontal,
            flipVertical = transform.flipVertical,
            scale = transform.scale,
            offsetX = transform.offsetX,
            offsetY = transform.offsetY,
            cropLeft = transform.cropLeft,
            cropTop = transform.cropTop,
            cropRight = transform.cropRight,
            cropBottom = transform.cropBottom,
            opacity = transform.opacity,
            transitionType = transition.type.name,
            transitionDurationMs = transition.durationMs,
            fromClipId = transition.fromClipId,
            toClipId = transition.toClipId,
            transitionEasing = transition.easing,
            text = textProperties?.text,
            textFontFamily = textProperties?.fontFamily,
            textFontSizeSp = textProperties?.fontSizeSp,
            textColorHex = textProperties?.colorHex,
            textBackgroundColorHex = textProperties?.backgroundColorHex,
            textStrokeColorHex = textProperties?.strokeColorHex,
            textStrokeWidth = textProperties?.strokeWidth,
            textHasShadow = textProperties?.hasShadow,
            textAlignment = textProperties?.alignment,
            textPositionX = textProperties?.positionX,
            textPositionY = textProperties?.positionY,
            textScale = textProperties?.scale,
            textRotationDegrees = textProperties?.rotationDegrees,
            textOpacity = textProperties?.opacity,
            imagePositionX = imageProperties?.positionX,
            imagePositionY = imageProperties?.positionY,
            imageScale = imageProperties?.scale,
            imageRotationDegrees = imageProperties?.rotationDegrees,
            imageOpacity = imageProperties?.opacity,
            keyframesData = serializeKeyframes(keyframes),
            // FIXED: Persist colorFilter + speedCurve
            colorFilterData = serializeColorFilter(colorFilter),
            speedCurveData = serializeSpeedCurve(speedCurve)
        )
    }

    // FIXED: Aligned track names with HomeViewModel
    private fun defaultTracks(): List<Track> {
        return listOf(
            Track(id = "track_text", type = TrackType.TEXT, name = "Text", order = 0),
            Track(id = "track_image", type = TrackType.OVERLAY, name = "Sticker", order = 1),
            Track(id = "track_video_2", type = TrackType.VIDEO, name = "Video 2", order = 2),
            Track(id = "track_video_1", type = TrackType.VIDEO, name = "Video 1", order = 3),
            Track(id = "track_audio_1", type = TrackType.AUDIO, name = "Audio 1", order = 4),
            Track(id = "track_audio_2", type = TrackType.AUDIO, name = "Audio 2", order = 5)
        )
    }

    private fun serializeTracks(tracks: List<Track>): String {
        return tracks.joinToString(";") {
            "${it.id}|${it.type.name}|${it.name}|${it.order}|${it.isMuted}|${it.isLocked}|${it.isVisible}"
        }
    }

    private fun deserializeTracks(data: String?): List<Track> {
        if (data.isNullOrBlank()) return defaultTracks()
        return try {
            data.split(";").mapNotNull { part ->
                val tokens = part.split("|")
                if (tokens.size >= 7) {
                    Track(
                        id = tokens[0],
                        type = runCatching { TrackType.valueOf(tokens[1]) }
                            .getOrDefault(TrackType.VIDEO),
                        name = tokens[2],
                        order = tokens[3].toIntOrNull() ?: 0,
                        isMuted = tokens[4].toBooleanStrictOrNull() ?: false,
                        isLocked = tokens[5].toBooleanStrictOrNull() ?: false,
                        isVisible = tokens[6].toBooleanStrictOrNull() ?: true
                    )
                } else null
            }.ifEmpty { defaultTracks() }
        } catch (_: Exception) {
            defaultTracks()
        }
    }

    private fun serializeKeyframes(keyframes: List<Keyframe>): String? {
        if (keyframes.isEmpty()) return null
        return keyframes.joinToString(";") {
            "${it.id}|${it.property.name}|${it.timeMs}|${it.value}|${it.interpolation.name}"
        }
    }

    private fun deserializeKeyframes(data: String?): List<Keyframe> {
        if (data.isNullOrBlank()) return emptyList()
        return try {
            data.split(";").mapNotNull { part ->
                val tokens = part.split("|")
                if (tokens.size >= 5) {
                    Keyframe(
                        id = tokens[0],
                        property = runCatching { KeyframeProperty.valueOf(tokens[1]) }
                            .getOrNull() ?: return@mapNotNull null,
                        timeMs = tokens[2].toLongOrNull() ?: return@mapNotNull null,
                        value = tokens[3].toFloatOrNull() ?: return@mapNotNull null,
                        interpolation = runCatching { KeyframeInterpolation.valueOf(tokens[4]) }
                            .getOrDefault(KeyframeInterpolation.LINEAR)
                    )
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // FIXED: ColorFilter serialization
    private fun serializeColorFilter(filter: ColorFilter): String? {
        if (filter.isDefault) return null
        return "${filter.brightness}|${filter.contrast}|${filter.saturation}|" +
               "${filter.temperature}|${filter.tint}|${filter.highlights}|${filter.shadows}|" +
               "${filter.lutId ?: ""}|${filter.lutIntensity}"
    }

    private fun deserializeColorFilter(data: String?): ColorFilter {
        if (data.isNullOrBlank()) return ColorFilter()
        return try {
            val t = data.split("|")
            if (t.size < 9) return ColorFilter()
            ColorFilter(
                brightness = t[0].toFloatOrNull() ?: 0f,
                contrast = t[1].toFloatOrNull() ?: 0f,
                saturation = t[2].toFloatOrNull() ?: 0f,
                temperature = t[3].toFloatOrNull() ?: 0f,
                tint = t[4].toFloatOrNull() ?: 0f,
                highlights = t[5].toFloatOrNull() ?: 0f,
                shadows = t[6].toFloatOrNull() ?: 0f,
                lutId = t[7].ifBlank { null },
                lutIntensity = t[8].toFloatOrNull() ?: 1.0f
            )
        } catch (_: Exception) {
            ColorFilter()
        }
    }

    // FIXED: SpeedCurve serialization
    private fun serializeSpeedCurve(curve: SpeedCurve?): String? {
        if (curve == null || !curve.isEnabled) return null
        val points = curve.points.joinToString(",") { "${it.timeNormalized}:${it.speed}" }
        return "${curve.id}|${curve.name}|${curve.preset.name}|$points|${curve.isEnabled}"
    }

    private fun deserializeSpeedCurve(data: String?): SpeedCurve? {
        if (data.isNullOrBlank()) return null
        return try {
            val t = data.split("|")
            if (t.size < 5) return null
            val points = t[3].split(",").mapNotNull { pt ->
                val parts = pt.split(":")
                if (parts.size == 2) {
                    val time = parts[0].toFloatOrNull() ?: return@mapNotNull null
                    val speed = parts[1].toFloatOrNull() ?: return@mapNotNull null
                    SpeedPoint(time.coerceIn(0f, 1f), speed.coerceIn(0.05f, 15f))
                } else null
            }
            if (points.isEmpty()) return null
            SpeedCurve(
                id = t[0],
                name = t[1],
                preset = runCatching { SpeedCurvePreset.valueOf(t[2]) }
                    .getOrDefault(SpeedCurvePreset.CUSTOM),
                points = points,
                isEnabled = t[4].toBooleanStrictOrNull() ?: true
            )
        } catch (_: Exception) {
            null
        }
    }
}