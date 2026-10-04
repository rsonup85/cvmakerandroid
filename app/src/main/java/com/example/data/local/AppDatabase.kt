// FIXED: Real migration v3 -> v4 instead of destructive fallback
package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.MediaAssetDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.TimelineItemDao
import com.example.data.local.entity.MediaAssetEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TimelineItemEntity

@Database(
    entities = [
        ProjectEntity::class,
        MediaAssetEntity::class,
        TimelineItemEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun mediaAssetDao(): MediaAssetDao
    abstract fun timelineItemDao(): TimelineItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // FIXED: Real migration from v3 to v4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE timeline_items ADD COLUMN colorFilterData TEXT DEFAULT NULL"
                )
                db.execSQL(
                    "ALTER TABLE timeline_items ADD COLUMN speedCurveData TEXT DEFAULT NULL"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vistara_edit_db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}