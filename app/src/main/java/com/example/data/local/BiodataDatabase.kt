package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.data.model.BiodataType
import com.example.data.model.JobBiodata
import com.example.data.model.MarriageBiodata
import com.example.data.model.OrderRecord
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "biodata_documents")
data class BiodataEntity(
    @PrimaryKey val id: String,
    val type: String, // "MARRIAGE" or "JOB"
    val templateId: String,
    val fullName: String,
    val jsonData: String,
    val pdfPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isCompleted: Boolean = false
)

@Entity(tableName = "payment_records")
data class PaymentEntity(
    @PrimaryKey val id: String, // orderId
    val itemId: String,
    val itemName: String,
    val itemType: String,
    val amount: Int,
    val paymentId: String,
    val userEmail: String,
    val status: String,
    val timestamp: Long
)

@Dao
interface BiodataDao {
    @Query("SELECT * FROM biodata_documents ORDER BY updatedAt DESC")
    fun getAllBiodatas(): Flow<List<BiodataEntity>>

    @Query("SELECT * FROM biodata_documents WHERE id = :id")
    suspend fun getBiodataById(id: String): BiodataEntity?

    @Query("SELECT * FROM biodata_documents WHERE type = :type ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestDraft(type: String): BiodataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBiodata(entity: BiodataEntity)

    @Delete
    suspend fun deleteBiodata(entity: BiodataEntity)

    @Query("DELETE FROM biodata_documents WHERE id = :id")
    suspend fun deleteBiodataById(id: String)

    @Query("SELECT * FROM payment_records ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)
}

@Database(entities = [BiodataEntity::class, PaymentEntity::class], version = 1, exportSchema = false)
abstract class BiodataDatabase : RoomDatabase() {
    abstract fun biodataDao(): BiodataDao

    companion object {
        @Volatile
        private var INSTANCE: BiodataDatabase? = null

        fun getDatabase(context: Context): BiodataDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BiodataDatabase::class.java,
                    "biodata_maker_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object BiodataJsonParser {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val marriageAdapter = moshi.adapter(MarriageBiodata::class.java)
    private val jobAdapter = moshi.adapter(JobBiodata::class.java)

    fun marriageToJson(biodata: MarriageBiodata): String = marriageAdapter.toJson(biodata)
    fun marriageFromJson(json: String): MarriageBiodata? = try {
        marriageAdapter.fromJson(json)
    } catch (e: Exception) {
        null
    }

    fun jobToJson(biodata: JobBiodata): String = jobAdapter.toJson(biodata)
    fun jobFromJson(json: String): JobBiodata? = try {
        jobAdapter.fromJson(json)
    } catch (e: Exception) {
        null
    }
}
