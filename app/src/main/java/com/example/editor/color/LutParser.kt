package com.example.editor.color

import android.util.Log
import com.example.domain.model.CubeLut
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object LutParser {
    private const val TAG = "LutParser"

    fun parseCube(inputStream: InputStream, id: String, name: String): CubeLut? {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            var size = 0
            val floatList = ArrayList<Float>(33 * 33 * 33 * 3)

            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    return@forEachLine
                }

                if (trimmed.startsWith("LUT_3D_SIZE", ignoreCase = true)) {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        size = parts[1].toIntOrNull() ?: 0
                    }
                    return@forEachLine
                }

                if (trimmed.startsWith("TITLE", ignoreCase = true) ||
                    trimmed.startsWith("DOMAIN_", ignoreCase = true)) {
                    return@forEachLine
                }

                // Data row: r g b
                val tokens = trimmed.split("\\s+".toRegex())
                if (tokens.size >= 3) {
                    val r = tokens[0].toFloatOrNull()
                    val g = tokens[1].toFloatOrNull()
                    val b = tokens[2].toFloatOrNull()
                    if (r != null && g != null && b != null) {
                        floatList.add(r)
                        floatList.add(g)
                        floatList.add(b)
                    }
                }
            }

            if (size <= 1) {
                // Infer size if cubic cube
                val entryCount = floatList.size / 3
                val inferred = Math.cbrt(entryCount.toDouble()).toInt()
                if (inferred * inferred * inferred == entryCount && inferred > 1) {
                    size = inferred
                } else {
                    Log.w(TAG, "Invalid cube LUT size: $size (entries: ${floatList.size / 3})")
                    return null
                }
            }

            val expectedFloats = size * size * size * 3
            if (floatList.size < expectedFloats) {
                Log.w(TAG, "Truncated LUT data. Expected $expectedFloats floats, found ${floatList.size}")
                return null
            }

            val tableData = FloatArray(expectedFloats)
            for (i in 0 until expectedFloats) {
                tableData[i] = floatList[i].coerceIn(0f, 1f)
            }

            CubeLut(id = id, name = name, size = size, tableData = tableData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing .cube LUT: $name", e)
            null
        }
    }
}
