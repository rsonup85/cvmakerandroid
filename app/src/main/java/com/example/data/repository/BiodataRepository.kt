package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.*
import com.example.data.model.*
import com.example.engine.PdfGenerator
import com.example.engine.TemplateRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class BiodataRepository(private val context: Context) {

    private val db = BiodataDatabase.getDatabase(context)
    private val dao = db.biodataDao()

    fun getAllDocuments(): Flow<List<BiodataEntity>> = dao.getAllBiodatas()

    fun getAllPayments(): Flow<List<PaymentEntity>> = dao.getAllPayments()

    suspend fun getBiodataById(id: String): BiodataEntity? = withContext(Dispatchers.IO) {
        dao.getBiodataById(id)
    }

    suspend fun saveMarriageDraft(biodata: MarriageBiodata): String = withContext(Dispatchers.IO) {
        val id = if (biodata.id.isNotBlank()) biodata.id else "m_draft_${System.currentTimeMillis()}"
        val updated = biodata.copy(id = id, updatedAt = System.currentTimeMillis())
        val json = BiodataJsonParser.marriageToJson(updated)
        val entity = BiodataEntity(
            id = id,
            type = "MARRIAGE",
            templateId = updated.templateId,
            fullName = if (updated.fullName.isNotBlank()) updated.fullName else "Marriage Biodata Draft",
            jsonData = json,
            pdfPath = null,
            createdAt = updated.createdAt,
            updatedAt = updated.updatedAt,
            isCompleted = false
        )
        dao.insertBiodata(entity)
        id
    }

    suspend fun saveJobDraft(biodata: JobBiodata): String = withContext(Dispatchers.IO) {
        val id = if (biodata.id.isNotBlank()) biodata.id else "j_draft_${System.currentTimeMillis()}"
        val updated = biodata.copy(id = id, updatedAt = System.currentTimeMillis())
        val json = BiodataJsonParser.jobToJson(updated)
        val entity = BiodataEntity(
            id = id,
            type = "JOB",
            templateId = updated.templateId,
            fullName = if (updated.fullName.isNotBlank()) updated.fullName else "Job Biodata Draft",
            jsonData = json,
            pdfPath = null,
            createdAt = updated.createdAt,
            updatedAt = updated.updatedAt,
            isCompleted = false
        )
        dao.insertBiodata(entity)
        id
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        dao.deleteBiodataById(id)
    }

    suspend fun recordPayment(
        orderId: String,
        itemId: String,
        itemName: String,
        itemType: String,
        amount: Int,
        paymentId: String,
        userEmail: String
    ) = withContext(Dispatchers.IO) {
        val entity = PaymentEntity(
            id = orderId,
            itemId = itemId,
            itemName = itemName,
            itemType = itemType,
            amount = amount,
            paymentId = paymentId,
            userEmail = userEmail,
            status = "SUCCESS",
            timestamp = System.currentTimeMillis()
        )
        dao.insertPayment(entity)
    }

    suspend fun generateAndSaveMarriagePdf(biodata: MarriageBiodata, template: BiodataTemplate): File = withContext(Dispatchers.IO) {
        val pdfFile = PdfGenerator.generateMarriagePdf(context, biodata, template)
        val id = if (biodata.id.isNotBlank()) biodata.id else "m_doc_${System.currentTimeMillis()}"
        val updated = biodata.copy(id = id, updatedAt = System.currentTimeMillis())
        val json = BiodataJsonParser.marriageToJson(updated)
        val entity = BiodataEntity(
            id = id,
            type = "MARRIAGE",
            templateId = template.id,
            fullName = if (updated.fullName.isNotBlank()) updated.fullName else "Marriage Biodata",
            jsonData = json,
            pdfPath = pdfFile.absolutePath,
            createdAt = updated.createdAt,
            updatedAt = System.currentTimeMillis(),
            isCompleted = true
        )
        dao.insertBiodata(entity)
        pdfFile
    }

    suspend fun generateAndSaveJobPdf(biodata: JobBiodata, template: BiodataTemplate): File = withContext(Dispatchers.IO) {
        val pdfFile = PdfGenerator.generateJobPdf(context, biodata, template)
        val id = if (biodata.id.isNotBlank()) biodata.id else "j_doc_${System.currentTimeMillis()}"
        val updated = biodata.copy(id = id, updatedAt = System.currentTimeMillis())
        val json = BiodataJsonParser.jobToJson(updated)
        val entity = BiodataEntity(
            id = id,
            type = "JOB",
            templateId = template.id,
            fullName = if (updated.fullName.isNotBlank()) updated.fullName else "Job Biodata",
            jsonData = json,
            pdfPath = pdfFile.absolutePath,
            createdAt = updated.createdAt,
            updatedAt = System.currentTimeMillis(),
            isCompleted = true
        )
        dao.insertBiodata(entity)
        pdfFile
    }

    fun sharePdf(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Biodata PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun emailPdf(file: File, userEmail: String, recipientName: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                if (userEmail.isNotBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(userEmail))
                }
                putExtra(Intent.EXTRA_SUBJECT, "Your Biodata PDF - $recipientName")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Dear $recipientName,\n\nPlease find attached your professionally generated Biodata PDF created with Biodata Maker.\n\nBest regards,\nBiodata Maker Team"
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Send Biodata via Email").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open email app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
