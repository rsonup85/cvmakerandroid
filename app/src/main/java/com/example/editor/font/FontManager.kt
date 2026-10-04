// FIXED: Thread-safe init() with lock + isInitialized flag
package com.example.editor.font

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import com.example.domain.model.EditorFont
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class FontSource {
    SYSTEM,
    ASSET,
    USER_FILE
}

data class FontInfo(
    val id: String,
    val name: String,
    val familyName: String? = null,
    val source: FontSource,
    val uriString: String? = null,
    val assetPath: String? = null
)

object FontManager {
    private const val TAG = "FontManager"
    private const val FONTS_DIR = "custom_fonts"

    private val typefaceCache = ConcurrentHashMap<String, Typeface>()
    private val registeredFonts = ConcurrentHashMap<String, FontInfo>()

    // FIXED: Thread-safe initialization
    private val initLock = Any()

    @Volatile
    private var isInitialized = false

    init {
        registerSystemFont("sans", "Sans Serif", Typeface.SANS_SERIF)
        registerSystemFont("serif", "Serif", Typeface.SERIF)
        registerSystemFont("mono", "Monospace", Typeface.MONOSPACE)
        registerSystemFont("bold", "Bold Heavy", Typeface.DEFAULT_BOLD)
        registerSystemFont("cursive", "Cursive / Script", Typeface.create("cursive", Typeface.NORMAL))
        registerSystemFont("light", "Clean Light", Typeface.create("sans-serif-light", Typeface.NORMAL))
    }

    private fun registerSystemFont(id: String, name: String, typeface: Typeface) {
        registeredFonts[id] = FontInfo(id = id, name = name, source = FontSource.SYSTEM)
        typefaceCache[id] = typeface
    }

    // FIXED: Synchronized init to prevent race condition
    fun init(context: Context) {
        synchronized(initLock) {
            if (isInitialized) return
            try {
                val dir = File(context.filesDir, FONTS_DIR)
                if (dir.exists()) {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile && (file.name.endsWith(".ttf", true) ||
                                            file.name.endsWith(".otf", true))) {
                            val fontId = "user_${file.nameWithoutExtension}"
                            val cleanName = file.nameWithoutExtension
                                .replace('_', ' ')
                                .replace('-', ' ')
                            registeredFonts[fontId] = FontInfo(
                                id = fontId,
                                name = cleanName,
                                source = FontSource.USER_FILE,
                                uriString = file.absolutePath
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning custom fonts", e)
            }
            isInitialized = true
        }
    }

    fun getAvailableFonts(): List<FontInfo> {
        return registeredFonts.values.sortedWith(
            compareBy<FontInfo> { it.source != FontSource.SYSTEM }
                .thenBy { it.name }
        )
    }

    fun importFontFromUri(context: Context, uri: Uri): FontInfo? {
        return try {
            val dir = File(context.filesDir, FONTS_DIR)
            if (!dir.exists()) dir.mkdirs()

            var fileName = "font_${System.currentTimeMillis()}"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val resolved = cursor.getString(nameIndex)
                    if (!resolved.isNullOrBlank()) {
                        fileName = resolved
                    }
                }
            }

            if (!fileName.endsWith(".ttf", true) && !fileName.endsWith(".otf", true)) {
                fileName += ".ttf"
            }

            val targetFile = File(dir, "${UUID.randomUUID()}_$fileName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            val typeface = Typeface.createFromFile(targetFile)
            if (typeface == null) {
                targetFile.delete()
                return null
            }

            val fontId = "user_${targetFile.nameWithoutExtension}"
            val displayName = fileName.substringBeforeLast('.')
                .replace('_', ' ')
                .replace('-', ' ')
            val fontInfo = FontInfo(
                id = fontId,
                name = displayName,
                source = FontSource.USER_FILE,
                uriString = targetFile.absolutePath
            )

            registeredFonts[fontId] = fontInfo
            typefaceCache[fontId] = typeface
            fontInfo
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import custom font from uri: $uri", e)
            null
        }
    }

    fun getTypeface(context: Context?, fontId: String?): Typeface {
        if (fontId.isNullOrBlank()) return Typeface.DEFAULT

        typefaceCache[fontId]?.let { return it }

        val fontInfo = registeredFonts[fontId]
        if (fontInfo != null) {
            when (fontInfo.source) {
                FontSource.SYSTEM -> {
                    val tf = when (fontId.lowercase()) {
                        "serif" -> Typeface.SERIF
                        "mono" -> Typeface.MONOSPACE
                        "bold" -> Typeface.DEFAULT_BOLD
                        "cursive" -> Typeface.create("cursive", Typeface.NORMAL)
                        "light" -> Typeface.create("sans-serif-light", Typeface.NORMAL)
                        else -> Typeface.SANS_SERIF
                    }
                    typefaceCache[fontId] = tf
                    return tf
                }
                FontSource.USER_FILE -> {
                    if (fontInfo.uriString != null) {
                        try {
                            val file = File(fontInfo.uriString)
                            if (file.exists()) {
                                val tf = Typeface.createFromFile(file)
                                if (tf != null) {
                                    typefaceCache[fontId] = tf
                                    return tf
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not load font file ${fontInfo.uriString}, falling back", e)
                        }
                    }
                }
                FontSource.ASSET -> {
                    if (context != null && fontInfo.assetPath != null) {
                        try {
                            val tf = Typeface.createFromAsset(context.assets, fontInfo.assetPath)
                            if (tf != null) {
                                typefaceCache[fontId] = tf
                                return tf
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not load font asset ${fontInfo.assetPath}, falling back", e)
                        }
                    }
                }
            }
        }

        val mappedEnum = EditorFont.fromId(fontId)
        val fallback = when (mappedEnum) {
            EditorFont.SERIF -> Typeface.SERIF
            EditorFont.MONO -> Typeface.MONOSPACE
            EditorFont.BOLD -> Typeface.DEFAULT_BOLD
            EditorFont.CURSIVE -> Typeface.create("cursive", Typeface.NORMAL)
            EditorFont.LIGHT -> Typeface.create("sans-serif-light", Typeface.NORMAL)
            else -> Typeface.SANS_SERIF
        }
        typefaceCache[fontId] = fallback
        return fallback
    }
}