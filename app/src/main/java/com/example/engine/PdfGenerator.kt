package com.example.engine

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import com.example.data.model.BiodataTemplate
import com.example.data.model.JobBiodata
import com.example.data.model.MarriageBiodata
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {
    private const val TAG = "PdfGenerator"

    // Standard A4 dimensions in points (72 points per inch)
    // 8.27 x 11.69 inches = 595 x 842 points
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun generateMarriagePdf(
        context: Context,
        biodata: MarriageBiodata,
        template: BiodataTemplate
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        try {
            val primaryColor = parseColor(template.primaryColorHex, Color.parseColor("#9A7B38"))
            val secondaryColor = parseColor(template.secondaryColorHex, Color.parseColor("#FBF9F5"))
            val accentColor = parseColor(template.accentColorHex, Color.parseColor("#C5A059"))
            val bgColor = parseColor(template.backgroundColorHex, Color.WHITE)

            // Draw Base Background
            val bgPaint = Paint().apply { color = bgColor; style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

            // Draw Decorative Border according to layout & borderStyle
            drawMarriageBorders(canvas, template, primaryColor, accentColor, secondaryColor)

            // Load profile photo if provided
            val photoBitmap = loadAndCropPhoto(context, biodata.profilePhotoUri, 110, 130)

            when (template.layoutType) {
                "SIDEBAR_LEFT" -> renderMarriageSidebarLeft(canvas, biodata, template, primaryColor, secondaryColor, accentColor, photoBitmap)
                "SIDEBAR_RIGHT" -> renderMarriageSidebarRight(canvas, biodata, template, primaryColor, secondaryColor, accentColor, photoBitmap)
                "TWO_COLUMN" -> renderMarriageTwoColumn(canvas, biodata, template, primaryColor, secondaryColor, accentColor, photoBitmap)
                "DARK_LUXURY" -> renderMarriageDarkLuxury(canvas, biodata, template, primaryColor, secondaryColor, accentColor, photoBitmap)
                else -> renderMarriageClassicRoyal(canvas, biodata, template, primaryColor, secondaryColor, accentColor, photoBitmap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering Marriage PDF page", e)
        }

        pdfDocument.finishPage(page)

        // Save PDF to app-internal documents directory
        val sanitizedName = if (biodata.fullName.isNotBlank()) {
            biodata.fullName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        } else {
            "Marriage"
        }
        val pdfDir = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }
        val outputFile = File(pdfDir, "${sanitizedName}_Marriage_Biodata.pdf")

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    fun generateJobPdf(
        context: Context,
        biodata: JobBiodata,
        template: BiodataTemplate
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        try {
            val primaryColor = parseColor(template.primaryColorHex, Color.parseColor("#1E3A8A"))
            val secondaryColor = parseColor(template.secondaryColorHex, Color.parseColor("#F8FAFC"))
            val accentColor = parseColor(template.accentColorHex, Color.parseColor("#3B82F6"))
            val bgColor = parseColor(template.backgroundColorHex, Color.WHITE)

            // Draw Background
            val bgPaint = Paint().apply { color = bgColor; style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

            val photoBitmap = loadAndCropPhoto(context, biodata.profilePhotoUri, 100, 100)

            when (template.layoutType) {
                "SIDEBAR_LEFT" -> renderJobSidebarLeft(canvas, biodata, template, primaryColor, secondaryColor, accentColor, photoBitmap)
                "SIDEBAR_RIGHT" -> renderJobSidebarRight(canvas, biodata, template, primaryColor, secondaryColor, accentColor, photoBitmap)
                "ATS_CLEAN" -> renderJobAtsClean(canvas, biodata, template, primaryColor, secondaryColor, accentColor, photoBitmap)
                "CREATIVE_ACCENT" -> renderJobCreativeAccent(canvas, biodata, template, primaryColor, secondaryColor, accentColor, photoBitmap)
                else -> renderJobExecutiveCorporate(canvas, biodata, template, primaryColor, secondaryColor, accentColor, photoBitmap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering Job PDF page", e)
        }

        pdfDocument.finishPage(page)

        val sanitizedName = if (biodata.fullName.isNotBlank()) {
            biodata.fullName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        } else {
            "Job"
        }
        val pdfDir = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }
        val outputFile = File(pdfDir, "${sanitizedName}_Job_Biodata.pdf")

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    // ==========================================
    // MARRIAGE TEMPLATE RENDERERS
    // ==========================================

    private fun renderMarriageClassicRoyal(
        canvas: Canvas,
        data: MarriageBiodata,
        template: BiodataTemplate,
        primary: Int,
        secondary: Int,
        accent: Int,
        photo: Bitmap?
    ) {
        val isSerif = template.fontStyle == "SERIF"
        val headerTypeface = if (isSerif) Typeface.SERIF else Typeface.DEFAULT_BOLD

        var currentY = 55f

        // Top Devotional / Auspicious Inscription
        if (template.headerStyle == "GANESHA") {
            val invocationPaint = Paint().apply {
                color = primary
                textSize = 14f
                typeface = Typeface.create(headerTypeface, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("|| श्री गणेशाय नमः ||", PAGE_WIDTH / 2f, currentY, invocationPaint)
            currentY += 26f
        } else if (template.headerStyle == "CREST") {
            val crestPaint = Paint().apply {
                color = accent
                textSize = 12f
                typeface = Typeface.create(headerTypeface, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.2f
                isAntiAlias = true
            }
            canvas.drawText("❖  BIODATA  ❖", PAGE_WIDTH / 2f, currentY, crestPaint)
            currentY += 26f
        } else {
            currentY += 10f
        }

        // Title: BIODATA
        val titlePaint = Paint().apply {
            color = primary
            textSize = 24f
            typeface = Typeface.create(headerTypeface, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("BIODATA", PAGE_WIDTH / 2f, currentY, titlePaint)
        currentY += 12f

        // Decorative line
        val linePaint = Paint().apply {
            color = accent
            strokeWidth = 2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(PAGE_WIDTH / 2f - 90f, currentY, PAGE_WIDTH / 2f + 90f, currentY, linePaint)
        currentY += 25f

        // Header Block: Name & Photo
        val contentLeft = 45f
        val contentRight = PAGE_WIDTH - 45f

        if (photo != null) {
            // Draw photo on top right
            val photoX = contentRight - 110f
            val photoY = currentY
            drawPhotoWithFrame(canvas, photo, photoX, photoY, 100f, 120f, accent, primary)

            // Name on left
            val namePaint = Paint().apply {
                color = primary
                textSize = 20f
                typeface = Typeface.create(headerTypeface, Typeface.BOLD)
                isAntiAlias = true
            }
            val displayName = if (data.fullName.isNotBlank()) data.fullName else "Full Name"
            canvas.drawText(displayName, contentLeft, currentY + 30f, namePaint)

            if (data.occupation.isNotBlank()) {
                val occPaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 11.5f
                    typeface = Typeface.DEFAULT
                    isAntiAlias = true
                }
                canvas.drawText(data.occupation, contentLeft, currentY + 50f, occPaint)
            }

            if (data.education.isNotBlank()) {
                val eduPaint = Paint().apply {
                    color = Color.GRAY
                    textSize = 10.5f
                    typeface = Typeface.DEFAULT
                    isAntiAlias = true
                }
                canvas.drawText(data.education, contentLeft, currentY + 68f, eduPaint)
            }

            currentY += 135f
        } else {
            // Name centered
            val namePaint = Paint().apply {
                color = primary
                textSize = 22f
                typeface = Typeface.create(headerTypeface, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val displayName = if (data.fullName.isNotBlank()) data.fullName else "Full Name"
            canvas.drawText(displayName, PAGE_WIDTH / 2f, currentY + 10f, namePaint)
            currentY += 40f
        }

        // Section 1: Personal Details
        currentY = drawMarriageSectionHeader(canvas, "Personal Details", contentLeft, contentRight, currentY, primary, secondary, accent, isSerif)
        currentY += 16f

        val personalFields = listOf(
            "Date of Birth" to data.dateOfBirth,
            "Birth Time & Place" to (data.birthTime + (if (data.birthPlace.isNotBlank()) ", ${data.birthPlace}" else "")),
            "Rashi / Nakshatra" to (data.rashi + (if (data.nakshatra.isNotBlank()) " / ${data.nakshatra}" else "")),
            "Gotra / Manglik" to (data.gotra + (if (data.manglik.isNotBlank()) " (Manglik: ${data.manglik})" else "")),
            "Religion & Caste" to (data.religion + (if (data.caste.isNotBlank()) " - ${data.caste}" else "")),
            "Height / Weight" to (data.height + (if (data.weight.isNotBlank()) " / ${data.weight}" else "")),
            "Complexion" to data.complexion,
            "Blood Group" to data.bloodGroup,
            "Education" to data.education,
            "Occupation" to data.occupation,
            "Annual Income" to data.annualIncome
        ).filter { it.second.isNotBlank() }

        currentY = drawKeyValueTable(canvas, personalFields, contentLeft + 10f, contentRight - 10f, currentY, 150f, primary)
        currentY += 14f

        // Section 2: Family Details
        currentY = drawMarriageSectionHeader(canvas, "Family Details", contentLeft, contentRight, currentY, primary, secondary, accent, isSerif)
        currentY += 16f

        val familyFields = listOf(
            "Father's Name" to data.fatherName,
            "Father's Occupation" to data.fatherOccupation,
            "Mother's Name" to data.motherName,
            "Mother's Occupation" to data.motherOccupation,
            "Brother(s)" to data.brothers,
            "Sister(s)" to data.sisters,
            "Family Type & Values" to "${data.familyType}, ${data.familyValues}",
            "Native Place" to data.nativePlace,
            "Family Status" to data.familyStatus
        ).filter { it.second.isNotBlank() }

        currentY = drawKeyValueTable(canvas, familyFields, contentLeft + 10f, contentRight - 10f, currentY, 150f, primary)
        currentY += 14f

        // Section 3: Contact Details (if allowed)
        if (data.showContactOnPdf) {
            currentY = drawMarriageSectionHeader(canvas, "Contact Details", contentLeft, contentRight, currentY, primary, secondary, accent, isSerif)
            currentY += 16f

            val contactFields = listOf(
                "Contact Number" to data.mobile,
                "Alternate Number" to data.alternateMobile,
                "Email Address" to data.email,
                "Residential Address" to data.residentialAddress
            ).filter { it.second.isNotBlank() }

            currentY = drawKeyValueTable(canvas, contactFields, contentLeft + 10f, contentRight - 10f, currentY, 150f, primary)
        }

        // About Me / Notes at bottom if space permits
        if (data.aboutMe.isNotBlank() && currentY < PAGE_HEIGHT - 90f) {
            currentY += 12f
            currentY = drawMarriageSectionHeader(canvas, "About Me & Expectations", contentLeft, contentRight, currentY, primary, secondary, accent, isSerif)
            currentY += 16f

            val aboutPaint = Paint().apply {
                color = Color.parseColor("#333333")
                textSize = 9.5f
                typeface = if (isSerif) Typeface.SERIF else Typeface.DEFAULT
                isAntiAlias = true
            }
            drawWrappedText(canvas, data.aboutMe, contentLeft + 10f, currentY, contentRight - contentLeft - 20f, 13f, aboutPaint)
        }
    }

    private fun renderMarriageSidebarLeft(
        canvas: Canvas,
        biodata: MarriageBiodata,
        template: BiodataTemplate,
        primary: Int,
        secondary: Int,
        accent: Int,
        photo: Bitmap?
    ) {
        val sidebarWidth = 190f

        // Fill Sidebar Background
        val sidePaint = Paint().apply { color = primary; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, sidebarWidth, PAGE_HEIGHT.toFloat(), sidePaint)

        var sideY = 50f

        // Photo in sidebar
        if (photo != null) {
            val photoX = (sidebarWidth - 110f) / 2f
            drawPhotoWithFrame(canvas, photo, photoX, sideY, 110f, 130f, accent, Color.WHITE)
            sideY += 145f
        } else {
            sideY += 30f
        }

        // Sidebar: Name & Auspicious symbol
        val namePaint = Paint().apply {
            color = Color.WHITE
            textSize = 17f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val displayName = if (biodata.fullName.isNotBlank()) biodata.fullName else "Full Name"
        canvas.drawText(displayName, sidebarWidth / 2f, sideY, namePaint)
        sideY += 28f

        // Sidebar Contact section
        val sideHeaderPaint = Paint().apply {
            color = accent
            textSize = 12f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val sideTextPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            textSize = 9.5f
            isAntiAlias = true
        }

        if (biodata.showContactOnPdf) {
            canvas.drawText("CONTACT DETAILS", 16f, sideY, sideHeaderPaint)
            sideY += 16f
            if (biodata.mobile.isNotBlank()) {
                canvas.drawText("Mobile: ${biodata.mobile}", 16f, sideY, sideTextPaint)
                sideY += 15f
            }
            if (biodata.email.isNotBlank()) {
                canvas.drawText("Email: ${biodata.email}", 16f, sideY, sideTextPaint)
                sideY += 15f
            }
            if (biodata.residentialAddress.isNotBlank()) {
                canvas.drawText("Address:", 16f, sideY, sideTextPaint)
                sideY += 13f
                sideY = drawWrappedText(canvas, biodata.residentialAddress, 16f, sideY, sidebarWidth - 32f, 12f, sideTextPaint)
                sideY += 10f
            }
        }

        // Main Content Area (Right Side)
        val mainLeft = sidebarWidth + 28f
        val mainRight = PAGE_WIDTH - 30f
        var mainY = 55f

        val mainTitlePaint = Paint().apply {
            color = primary
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("MATRIMONIAL BIODATA", mainLeft, mainY, mainTitlePaint)
        mainY += 18f

        // Personal Details Header
        mainY = drawMarriageSectionHeader(canvas, "Personal Details", mainLeft, mainRight, mainY, primary, secondary, accent, true)
        mainY += 16f

        val personalFields = listOf(
            "Date of Birth" to biodata.dateOfBirth,
            "Birth Time / Place" to (biodata.birthTime + (if (biodata.birthPlace.isNotBlank()) ", ${biodata.birthPlace}" else "")),
            "Rashi / Nakshatra" to (biodata.rashi + (if (biodata.nakshatra.isNotBlank()) " / ${biodata.nakshatra}" else "")),
            "Gotra / Manglik" to (biodata.gotra + (if (biodata.manglik.isNotBlank()) " (${biodata.manglik})" else "")),
            "Religion & Caste" to (biodata.religion + (if (biodata.caste.isNotBlank()) " - ${biodata.caste}" else "")),
            "Height / Weight" to (biodata.height + (if (biodata.weight.isNotBlank()) " / ${biodata.weight}" else "")),
            "Complexion / Blood" to (biodata.complexion + (if (biodata.bloodGroup.isNotBlank()) " / ${biodata.bloodGroup}" else "")),
            "Qualification" to biodata.education,
            "Profession / Job" to biodata.occupation,
            "Annual Income" to biodata.annualIncome
        ).filter { it.second.isNotBlank() }

        mainY = drawKeyValueTable(canvas, personalFields, mainLeft, mainRight, mainY, 130f, primary)
        mainY += 16f

        // Family Details
        mainY = drawMarriageSectionHeader(canvas, "Family Background", mainLeft, mainRight, mainY, primary, secondary, accent, true)
        mainY += 16f

        val familyFields = listOf(
            "Father's Name" to biodata.fatherName,
            "Father's Profession" to biodata.fatherOccupation,
            "Mother's Name" to biodata.motherName,
            "Mother's Profession" to biodata.motherOccupation,
            "Brothers" to biodata.brothers,
            "Sisters" to biodata.sisters,
            "Family Structure" to "${biodata.familyType}, ${biodata.familyValues}",
            "Native Place" to biodata.nativePlace
        ).filter { it.second.isNotBlank() }

        mainY = drawKeyValueTable(canvas, familyFields, mainLeft, mainRight, mainY, 130f, primary)
        mainY += 16f

        if (biodata.aboutMe.isNotBlank()) {
            mainY = drawMarriageSectionHeader(canvas, "Partner Expectations", mainLeft, mainRight, mainY, primary, secondary, accent, true)
            mainY += 16f

            val aboutPaint = Paint().apply {
                color = Color.parseColor("#333333")
                textSize = 9.5f
                typeface = Typeface.SERIF
                isAntiAlias = true
            }
            drawWrappedText(canvas, biodata.aboutMe, mainLeft, mainY, mainRight - mainLeft, 13f, aboutPaint)
        }
    }

    private fun renderMarriageSidebarRight(
        canvas: Canvas,
        biodata: MarriageBiodata,
        template: BiodataTemplate,
        primary: Int,
        secondary: Int,
        accent: Int,
        photo: Bitmap?
    ) {
        val sidebarWidth = 185f
        val sideLeft = PAGE_WIDTH - sidebarWidth

        // Sidebar on right
        val sidePaint = Paint().apply { color = secondary; style = Paint.Style.FILL }
        canvas.drawRect(sideLeft, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), sidePaint)

        // Accent divider line
        val divPaint = Paint().apply { color = accent; strokeWidth = 2f }
        canvas.drawLine(sideLeft, 0f, sideLeft, PAGE_HEIGHT.toFloat(), divPaint)

        // Photo on right top
        var sideY = 50f
        if (photo != null) {
            val photoX = sideLeft + (sidebarWidth - 110f) / 2f
            drawPhotoWithFrame(canvas, photo, photoX, sideY, 110f, 130f, accent, primary)
            sideY += 145f
        }

        // Contact info in sidebar
        if (biodata.showContactOnPdf) {
            val sideTitlePaint = Paint().apply {
                color = primary
                textSize = 12f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val sideBodyPaint = Paint().apply {
                color = Color.parseColor("#2D3748")
                textSize = 9.5f
                isAntiAlias = true
            }
            canvas.drawText("CONTACT DETAILS", sideLeft + 16f, sideY, sideTitlePaint)
            sideY += 16f
            if (biodata.mobile.isNotBlank()) {
                canvas.drawText("Mobile: ${biodata.mobile}", sideLeft + 16f, sideY, sideBodyPaint)
                sideY += 15f
            }
            if (biodata.email.isNotBlank()) {
                canvas.drawText("Email: ${biodata.email}", sideLeft + 16f, sideY, sideBodyPaint)
                sideY += 15f
            }
            if (biodata.residentialAddress.isNotBlank()) {
                canvas.drawText("Address:", sideLeft + 16f, sideY, sideBodyPaint)
                sideY += 13f
                drawWrappedText(canvas, biodata.residentialAddress, sideLeft + 16f, sideY, sidebarWidth - 32f, 12f, sideBodyPaint)
            }
        }

        // Main left body
        val mainLeft = 35f
        val mainRight = sideLeft - 20f
        var mainY = 55f

        val titlePaint = Paint().apply {
            color = primary
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val nameToDraw = if (biodata.fullName.isNotBlank()) biodata.fullName else "Full Name"
        canvas.drawText(nameToDraw, mainLeft, mainY, titlePaint)
        mainY += 26f

        mainY = drawMarriageSectionHeader(canvas, "Personal Details", mainLeft, mainRight, mainY, primary, secondary, accent, true)
        mainY += 16f

        val personalFields = listOf(
            "Date of Birth" to biodata.dateOfBirth,
            "Birth Time / Place" to (biodata.birthTime + (if (biodata.birthPlace.isNotBlank()) ", ${biodata.birthPlace}" else "")),
            "Rashi / Nakshatra" to (biodata.rashi + (if (biodata.nakshatra.isNotBlank()) " / ${biodata.nakshatra}" else "")),
            "Gotra / Manglik" to (biodata.gotra + (if (biodata.manglik.isNotBlank()) " (${biodata.manglik})" else "")),
            "Religion & Caste" to (biodata.religion + (if (biodata.caste.isNotBlank()) " - ${biodata.caste}" else "")),
            "Height / Complexion" to (biodata.height + (if (biodata.complexion.isNotBlank()) " / ${biodata.complexion}" else "")),
            "Qualification" to biodata.education,
            "Profession" to biodata.occupation,
            "Annual Income" to biodata.annualIncome
        ).filter { it.second.isNotBlank() }

        mainY = drawKeyValueTable(canvas, personalFields, mainLeft, mainRight, mainY, 130f, primary)
        mainY += 16f

        mainY = drawMarriageSectionHeader(canvas, "Family Details", mainLeft, mainRight, mainY, primary, secondary, accent, true)
        mainY += 16f

        val familyFields = listOf(
            "Father's Name" to biodata.fatherName,
            "Occupation" to biodata.fatherOccupation,
            "Mother's Name" to biodata.motherName,
            "Occupation" to biodata.motherOccupation,
            "Brothers / Sisters" to "${biodata.brothers} / ${biodata.sisters}",
            "Native Place" to biodata.nativePlace
        ).filter { it.second.isNotBlank() }

        mainY = drawKeyValueTable(canvas, familyFields, mainLeft, mainRight, mainY, 130f, primary)
    }

    private fun renderMarriageTwoColumn(
        canvas: Canvas,
        biodata: MarriageBiodata,
        template: BiodataTemplate,
        primary: Int,
        secondary: Int,
        accent: Int,
        photo: Bitmap?
    ) {
        var topY = 50f
        val isSerif = template.fontStyle == "SERIF"

        // Center header
        val headerPaint = Paint().apply {
            color = primary
            textSize = 24f
            typeface = Typeface.create(if (isSerif) Typeface.SERIF else Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val displayName = if (biodata.fullName.isNotBlank()) biodata.fullName else "Marriage Biodata"
        canvas.drawText(displayName, PAGE_WIDTH / 2f, topY, headerPaint)
        topY += 28f

        // Photo centered if present
        if (photo != null) {
            val photoX = (PAGE_WIDTH - 90f) / 2f
            drawPhotoWithFrame(canvas, photo, photoX, topY, 90f, 105f, accent, primary)
            topY += 120f
        }

        val col1Left = 40f
        val col1Right = PAGE_WIDTH / 2f - 15f
        val col2Left = PAGE_WIDTH / 2f + 15f
        val col2Right = PAGE_WIDTH - 40f

        // Column 1: Personal
        var col1Y = topY
        col1Y = drawMarriageSectionHeader(canvas, "Personal Details", col1Left, col1Right, col1Y, primary, secondary, accent, isSerif)
        col1Y += 15f

        val personalFields = listOf(
            "DOB" to biodata.dateOfBirth,
            "Time" to biodata.birthTime,
            "Place" to biodata.birthPlace,
            "Rashi" to biodata.rashi,
            "Nakshatra" to biodata.nakshatra,
            "Gotra" to biodata.gotra,
            "Manglik" to biodata.manglik,
            "Religion" to biodata.religion,
            "Caste" to biodata.caste,
            "Height" to biodata.height,
            "Complexion" to biodata.complexion,
            "Education" to biodata.education,
            "Income" to biodata.annualIncome
        ).filter { it.second.isNotBlank() }

        drawKeyValueTable(canvas, personalFields, col1Left, col1Right, col1Y, 80f, primary)

        // Column 2: Family & Contact
        var col2Y = topY
        col2Y = drawMarriageSectionHeader(canvas, "Family Background", col2Left, col2Right, col2Y, primary, secondary, accent, isSerif)
        col2Y += 15f

        val familyFields = listOf(
            "Father" to biodata.fatherName,
            "Occupation" to biodata.fatherOccupation,
            "Mother" to biodata.motherName,
            "Occupation" to biodata.motherOccupation,
            "Brothers" to biodata.brothers,
            "Sisters" to biodata.sisters,
            "Native Place" to biodata.nativePlace
        ).filter { it.second.isNotBlank() }

        col2Y = drawKeyValueTable(canvas, familyFields, col2Left, col2Right, col2Y, 85f, primary)
        col2Y += 15f

        if (biodata.showContactOnPdf) {
            col2Y = drawMarriageSectionHeader(canvas, "Contact Details", col2Left, col2Right, col2Y, primary, secondary, accent, isSerif)
            col2Y += 15f

            val contactFields = listOf(
                "Mobile" to biodata.mobile,
                "Email" to biodata.email,
                "Address" to biodata.residentialAddress
            ).filter { it.second.isNotBlank() }

            drawKeyValueTable(canvas, contactFields, col2Left, col2Right, col2Y, 85f, primary)
        }
    }

    private fun renderMarriageDarkLuxury(
        canvas: Canvas,
        biodata: MarriageBiodata,
        template: BiodataTemplate,
        primary: Int,
        secondary: Int,
        accent: Int,
        photo: Bitmap?
    ) {
        // Deep background already drawn
        val contentLeft = 45f
        val contentRight = PAGE_WIDTH - 45f
        var currentY = 55f

        // Top Gold Crest
        val crestPaint = Paint().apply {
            color = accent
            textSize = 13f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.25f
            isAntiAlias = true
        }
        canvas.drawText("✦  MATRIMONIAL PROFILE  ✦", PAGE_WIDTH / 2f, currentY, crestPaint)
        currentY += 28f

        // Gold Name
        val namePaint = Paint().apply {
            color = accent
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val displayName = if (biodata.fullName.isNotBlank()) biodata.fullName else "Full Name"
        canvas.drawText(displayName, PAGE_WIDTH / 2f, currentY, namePaint)
        currentY += 25f

        if (photo != null) {
            val photoX = (PAGE_WIDTH - 100f) / 2f
            drawPhotoWithFrame(canvas, photo, photoX, currentY, 100f, 115f, accent, Color.BLACK)
            currentY += 130f
        }

        // Section Cards on Dark Background
        currentY = drawMarriageSectionHeader(canvas, "Personal Details", contentLeft, contentRight, currentY, accent, Color.parseColor("#222222"), accent, true)
        currentY += 16f

        val personalFields = listOf(
            "Date of Birth" to biodata.dateOfBirth,
            "Birth Time / Place" to (biodata.birthTime + (if (biodata.birthPlace.isNotBlank()) ", ${biodata.birthPlace}" else "")),
            "Rashi / Nakshatra" to (biodata.rashi + (if (biodata.nakshatra.isNotBlank()) " / ${biodata.nakshatra}" else "")),
            "Religion & Caste" to (biodata.religion + (if (biodata.caste.isNotBlank()) " - ${biodata.caste}" else "")),
            "Height / Complexion" to (biodata.height + (if (biodata.complexion.isNotBlank()) " / ${biodata.complexion}" else "")),
            "Education" to biodata.education,
            "Profession" to biodata.occupation,
            "Income" to biodata.annualIncome
        ).filter { it.second.isNotBlank() }

        currentY = drawKeyValueTable(canvas, personalFields, contentLeft + 10f, contentRight - 10f, currentY, 150f, accent, labelColor = accent, valueColor = Color.WHITE)
        currentY += 16f

        currentY = drawMarriageSectionHeader(canvas, "Family Background", contentLeft, contentRight, currentY, accent, Color.parseColor("#222222"), accent, true)
        currentY += 16f

        val familyFields = listOf(
            "Father's Name" to biodata.fatherName,
            "Father's Occupation" to biodata.fatherOccupation,
            "Mother's Name" to biodata.motherName,
            "Mother's Occupation" to biodata.motherOccupation,
            "Brothers / Sisters" to "${biodata.brothers} / ${biodata.sisters}",
            "Native Place" to biodata.nativePlace
        ).filter { it.second.isNotBlank() }

        currentY = drawKeyValueTable(canvas, familyFields, contentLeft + 10f, contentRight - 10f, currentY, 150f, accent, labelColor = accent, valueColor = Color.WHITE)
        currentY += 16f

        if (biodata.showContactOnPdf) {
            currentY = drawMarriageSectionHeader(canvas, "Contact Details", contentLeft, contentRight, currentY, accent, Color.parseColor("#222222"), accent, true)
            currentY += 16f

            val contactFields = listOf(
                "Mobile" to biodata.mobile,
                "Email" to biodata.email,
                "Address" to biodata.residentialAddress
            ).filter { it.second.isNotBlank() }

            drawKeyValueTable(canvas, contactFields, contentLeft + 10f, contentRight - 10f, currentY, 150f, accent, labelColor = accent, valueColor = Color.WHITE)
        }
    }

    // ==========================================
    // JOB TEMPLATE RENDERERS
    // ==========================================

    private fun renderJobExecutiveCorporate(
        canvas: Canvas,
        data: JobBiodata,
        template: BiodataTemplate,
        primary: Int,
        secondary: Int,
        accent: Int,
        photo: Bitmap?
    ) {
        // Executive Corporate Top Banner
        val bannerHeight = 110f
        val bannerPaint = Paint().apply { color = primary; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), bannerHeight, bannerPaint)

        // Accent strip under banner
        val stripPaint = Paint().apply { color = accent; style = Paint.Style.FILL }
        canvas.drawRect(0f, bannerHeight, PAGE_WIDTH.toFloat(), bannerHeight + 5f, stripPaint)

        // Text inside banner
        val namePaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val displayName = if (data.fullName.isNotBlank()) data.fullName else "Full Name"
        canvas.drawText(displayName, 45f, 50f, namePaint)

        val rolePaint = Paint().apply {
            color = Color.parseColor("#E0E7FF")
            textSize = 13f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val targetRole = if (data.targetRole.isNotBlank()) data.targetRole else "Professional"
        canvas.drawText(targetRole, 45f, 72f, rolePaint)

        // Contact info right under role or on right
        val contactSummary = listOf(data.mobile, data.email, data.address).filter { it.isNotBlank() }.joinToString("  |  ")
        val contactPaint = Paint().apply {
            color = Color.parseColor("#C7D2FE")
            textSize = 9.5f
            isAntiAlias = true
        }
        canvas.drawText(contactSummary, 45f, 92f, contactPaint)

        // Photo on banner right if present
        if (photo != null) {
            drawPhotoWithFrame(canvas, photo, PAGE_WIDTH - 125f, 20f, 80f, 80f, Color.WHITE, primary)
        }

        var currentY = bannerHeight + 25f
        val contentLeft = 45f
        val contentRight = PAGE_WIDTH - 45f

        // Career Objective / Summary
        if (data.careerObjective.isNotBlank()) {
            currentY = drawJobSectionHeader(canvas, "CAREER OBJECTIVE", contentLeft, contentRight, currentY, primary, accent)
            currentY += 15f
            val objPaint = Paint().apply { color = Color.parseColor("#334155"); textSize = 10f; isAntiAlias = true }
            currentY = drawWrappedText(canvas, data.careerObjective, contentLeft, currentY, contentRight - contentLeft, 14f, objPaint)
            currentY += 16f
        }

        // Work Experience
        if (data.workExperience.isNotEmpty()) {
            currentY = drawJobSectionHeader(canvas, "WORK EXPERIENCE", contentLeft, contentRight, currentY, primary, accent)
            currentY += 16f

            data.workExperience.forEach { exp ->
                val titlePaint = Paint().apply { color = primary; textSize = 11.5f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                canvas.drawText(exp.jobTitle, contentLeft, currentY, titlePaint)

                if (exp.duration.isNotBlank()) {
                    val datePaint = Paint().apply { color = Color.GRAY; textSize = 10f; textAlign = Paint.Align.RIGHT; isAntiAlias = true }
                    canvas.drawText(exp.duration, contentRight, currentY, datePaint)
                }
                currentY += 14f

                if (exp.company.isNotBlank()) {
                    val compPaint = Paint().apply { color = Color.DKGRAY; textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                    canvas.drawText(exp.company, contentLeft, currentY, compPaint)
                    currentY += 14f
                }

                if (exp.responsibilities.isNotBlank()) {
                    val respPaint = Paint().apply { color = Color.parseColor("#475569"); textSize = 9.5f; isAntiAlias = true }
                    currentY = drawWrappedText(canvas, exp.responsibilities, contentLeft + 8f, currentY, contentRight - contentLeft - 8f, 13f, respPaint)
                }
                currentY += 10f
            }
            currentY += 8f
        }

        // Education
        currentY = drawJobSectionHeader(canvas, "EDUCATION & CREDENTIALS", contentLeft, contentRight, currentY, primary, accent)
        currentY += 16f

        val eduList = listOf(
            "Post Graduation" to data.postGraduation,
            "Graduation" to data.graduation,
            "Intermediate (12th)" to data.intermediate,
            "High School (10th)" to data.highSchool,
            "Other Certifications" to data.otherQualifications
        ).filter { it.second.isNotBlank() }

        currentY = drawKeyValueTable(canvas, eduList, contentLeft, contentRight, currentY, 140f, primary)
        currentY += 16f

        // Skills, Languages, Hobbies
        currentY = drawJobSectionHeader(canvas, "SKILLS & ADDITIONAL INFO", contentLeft, contentRight, currentY, primary, accent)
        currentY += 16f

        val additional = listOf(
            "Core Skills" to data.skills,
            "Languages Known" to data.languages,
            "Hobbies & Interests" to data.hobbies,
            "Certifications" to data.certifications
        ).filter { it.second.isNotBlank() }

        drawKeyValueTable(canvas, additional, contentLeft, contentRight, currentY, 140f, primary)
    }

    private fun renderJobSidebarLeft(
        canvas: Canvas,
        data: JobBiodata,
        template: BiodataTemplate,
        primary: Int,
        secondary: Int,
        accent: Int,
        photo: Bitmap?
    ) {
        val sidebarWidth = 195f

        // Fill Sidebar
        val sidePaint = Paint().apply { color = primary; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, sidebarWidth, PAGE_HEIGHT.toFloat(), sidePaint)

        var sideY = 45f

        // Photo in sidebar
        if (photo != null) {
            val photoX = (sidebarWidth - 100f) / 2f
            drawPhotoWithFrame(canvas, photo, photoX, sideY, 100f, 100f, accent, Color.WHITE)
            sideY += 115f
        }

        // Contact info in sidebar
        val sideHeaderPaint = Paint().apply {
            color = accent
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val sideTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            isAntiAlias = true
        }

        canvas.drawText("CONTACT INFO", 18f, sideY, sideHeaderPaint)
        sideY += 15f
        if (data.mobile.isNotBlank()) {
            canvas.drawText("📞 ${data.mobile}", 18f, sideY, sideTextPaint)
            sideY += 14f
        }
        if (data.email.isNotBlank()) {
            canvas.drawText("✉ ${data.email}", 18f, sideY, sideTextPaint)
            sideY += 14f
        }
        if (data.portfolioOrLinkedin.isNotBlank()) {
            canvas.drawText("🔗 ${data.portfolioOrLinkedin}", 18f, sideY, sideTextPaint)
            sideY += 14f
        }
        if (data.address.isNotBlank()) {
            sideY = drawWrappedText(canvas, "📍 ${data.address}", 18f, sideY, sidebarWidth - 36f, 12f, sideTextPaint)
            sideY += 10f
        }
        sideY += 12f

        // Skills in sidebar
        if (data.skills.isNotBlank()) {
            canvas.drawText("KEY SKILLS", 18f, sideY, sideHeaderPaint)
            sideY += 15f
            val skillsList = data.skills.split(",").map { it.trim() }
            skillsList.forEach { skill ->
                canvas.drawText("• $skill", 18f, sideY, sideTextPaint)
                sideY += 13f
            }
            sideY += 10f
        }

        // Languages in sidebar
        if (data.languages.isNotBlank()) {
            canvas.drawText("LANGUAGES", 18f, sideY, sideHeaderPaint)
            sideY += 15f
            canvas.drawText(data.languages, 18f, sideY, sideTextPaint)
            sideY += 20f
        }

        // Main Body (Right side)
        val mainLeft = sidebarWidth + 25f
        val mainRight = PAGE_WIDTH - 30f
        var mainY = 50f

        val namePaint = Paint().apply {
            color = primary
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val displayName = if (data.fullName.isNotBlank()) data.fullName else "Full Name"
        canvas.drawText(displayName, mainLeft, mainY, namePaint)
        mainY += 20f

        val rolePaint = Paint().apply {
            color = accent
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val targetRole = if (data.targetRole.isNotBlank()) data.targetRole else "Professional Profile"
        canvas.drawText(targetRole, mainLeft, mainY, rolePaint)
        mainY += 25f

        // Career Objective
        if (data.careerObjective.isNotBlank()) {
            mainY = drawJobSectionHeader(canvas, "PROFILE SUMMARY", mainLeft, mainRight, mainY, primary, accent)
            mainY += 15f
            val objPaint = Paint().apply { color = Color.parseColor("#334155"); textSize = 9.5f; isAntiAlias = true }
            mainY = drawWrappedText(canvas, data.careerObjective, mainLeft, mainY, mainRight - mainLeft, 13f, objPaint)
            mainY += 16f
        }

        // Work Experience
        if (data.workExperience.isNotEmpty()) {
            mainY = drawJobSectionHeader(canvas, "PROFESSIONAL EXPERIENCE", mainLeft, mainRight, mainY, primary, accent)
            mainY += 16f

            data.workExperience.forEach { exp ->
                val titlePaint = Paint().apply { color = primary; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                canvas.drawText(exp.jobTitle, mainLeft, mainY, titlePaint)

                if (exp.duration.isNotBlank()) {
                    val datePaint = Paint().apply { color = Color.GRAY; textSize = 9.5f; textAlign = Paint.Align.RIGHT; isAntiAlias = true }
                    canvas.drawText(exp.duration, mainRight, mainY, datePaint)
                }
                mainY += 13f

                if (exp.company.isNotBlank()) {
                    val compPaint = Paint().apply { color = Color.DKGRAY; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                    canvas.drawText(exp.company, mainLeft, mainY, compPaint)
                    mainY += 13f
                }

                if (exp.responsibilities.isNotBlank()) {
                    val respPaint = Paint().apply { color = Color.parseColor("#475569"); textSize = 9f; isAntiAlias = true }
                    mainY = drawWrappedText(canvas, exp.responsibilities, mainLeft + 6f, mainY, mainRight - mainLeft - 6f, 12f, respPaint)
                }
                mainY += 10f
            }
            mainY += 8f
        }

        // Education
        mainY = drawJobSectionHeader(canvas, "EDUCATION", mainLeft, mainRight, mainY, primary, accent)
        mainY += 15f

        val eduList = listOf(
            "Post Graduation" to data.postGraduation,
            "Graduation" to data.graduation,
            "Intermediate" to data.intermediate,
            "High School" to data.highSchool
        ).filter { it.second.isNotBlank() }

        drawKeyValueTable(canvas, eduList, mainLeft, mainRight, mainY, 110f, primary)
    }

    private fun renderJobSidebarRight(
        canvas: Canvas,
        data: JobBiodata,
        template: BiodataTemplate,
        primary: Int,
        secondary: Int,
        accent: Int,
        photo: Bitmap?
    ) {
        val sidebarWidth = 190f
        val sideLeft = PAGE_WIDTH - sidebarWidth

        // Sidebar on right
        val sidePaint = Paint().apply { color = secondary; style = Paint.Style.FILL }
        canvas.drawRect(sideLeft, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), sidePaint)

        var sideY = 45f
        if (photo != null) {
            val photoX = sideLeft + (sidebarWidth - 100f) / 2f
            drawPhotoWithFrame(canvas, photo, photoX, sideY, 100f, 100f, accent, primary)
            sideY += 115f
        }

        // Skills in right sidebar
        val sideHeaderPaint = Paint().apply { color = primary; textSize = 11.5f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val sideBodyPaint = Paint().apply { color = Color.parseColor("#334155"); textSize = 9f; isAntiAlias = true }

        if (data.skills.isNotBlank()) {
            canvas.drawText("TECHNICAL SKILLS", sideLeft + 16f, sideY, sideHeaderPaint)
            sideY += 15f
            data.skills.split(",").forEach { s ->
                canvas.drawText("• ${s.trim()}", sideLeft + 16f, sideY, sideBodyPaint)
                sideY += 13f
            }
            sideY += 14f
        }

        // Languages & Personal in right sidebar
        canvas.drawText("ADDITIONAL INFO", sideLeft + 16f, sideY, sideHeaderPaint)
        sideY += 15f
        if (data.languages.isNotBlank()) {
            canvas.drawText("Languages: ${data.languages}", sideLeft + 16f, sideY, sideBodyPaint)
            sideY += 14f
        }
        if (data.nationality.isNotBlank()) {
            canvas.drawText("Nationality: ${data.nationality}", sideLeft + 16f, sideY, sideBodyPaint)
            sideY += 14f
        }
        if (data.hobbies.isNotBlank()) {
            canvas.drawText("Hobbies: ${data.hobbies}", sideLeft + 16f, sideY, sideBodyPaint)
            sideY += 14f
        }

        // Main left body
        val mainLeft = 40f
        val mainRight = sideLeft - 20f
        var mainY = 50f

        val namePaint = Paint().apply { color = primary; textSize = 24f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val displayName = if (data.fullName.isNotBlank()) data.fullName else "Full Name"
        canvas.drawText(displayName, mainLeft, mainY, namePaint)
        mainY += 18f

        val contactLine = listOf(data.mobile, data.email, data.portfolioOrLinkedin).filter { it.isNotBlank() }.joinToString(" • ")
        val contactPaint = Paint().apply { color = Color.GRAY; textSize = 9f; isAntiAlias = true }
        canvas.drawText(contactLine, mainLeft, mainY, contactPaint)
        mainY += 25f

        if (data.careerObjective.isNotBlank()) {
            mainY = drawJobSectionHeader(canvas, "CAREER SUMMARY", mainLeft, mainRight, mainY, primary, accent)
            mainY += 14f
            val objPaint = Paint().apply { color = Color.parseColor("#334155"); textSize = 9.5f; isAntiAlias = true }
            mainY = drawWrappedText(canvas, data.careerObjective, mainLeft, mainY, mainRight - mainLeft, 13f, objPaint)
            mainY += 16f
        }

        if (data.workExperience.isNotEmpty()) {
            mainY = drawJobSectionHeader(canvas, "EXPERIENCE", mainLeft, mainRight, mainY, primary, accent)
            mainY += 16f

            data.workExperience.forEach { exp ->
                val titlePaint = Paint().apply { color = primary; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                canvas.drawText(exp.jobTitle, mainLeft, mainY, titlePaint)
                mainY += 13f
                if (exp.company.isNotBlank()) {
                    val compPaint = Paint().apply { color = Color.DKGRAY; textSize = 10f; isAntiAlias = true }
                    canvas.drawText(exp.company, mainLeft, mainY, compPaint)
                    mainY += 13f
                }
                if (exp.responsibilities.isNotBlank()) {
                    val respPaint = Paint().apply { color = Color.parseColor("#475569"); textSize = 9f; isAntiAlias = true }
                    mainY = drawWrappedText(canvas, exp.responsibilities, mainLeft, mainY, mainRight - mainLeft, 12f, respPaint)
                }
                mainY += 10f
            }
            mainY += 8f
        }

        mainY = drawJobSectionHeader(canvas, "EDUCATION", mainLeft, mainRight, mainY, primary, accent)
        mainY += 15f
        val eduList = listOf(
            "Graduation" to data.graduation,
            "12th" to data.intermediate,
            "10th" to data.highSchool
        ).filter { it.second.isNotBlank() }
        drawKeyValueTable(canvas, eduList, mainLeft, mainRight, mainY, 100f, primary)
    }

    private fun renderJobAtsClean(
        canvas: Canvas,
        data: JobBiodata,
        template: BiodataTemplate,
        primary: Int,
        secondary: Int,
        accent: Int,
        photo: Bitmap?
    ) {
        // High ATS scannable format: Minimal decorative elements, strict linear typography
        val contentLeft = 45f
        val contentRight = PAGE_WIDTH - 45f
        var currentY = 55f

        val namePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val displayName = if (data.fullName.isNotBlank()) data.fullName else "Full Name"
        canvas.drawText(displayName, PAGE_WIDTH / 2f, currentY, namePaint)
        currentY += 18f

        val contactLine = listOf(data.mobile, data.email, data.address, data.portfolioOrLinkedin).filter { it.isNotBlank() }.joinToString(" | ")
        val contactPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(contactLine, PAGE_WIDTH / 2f, currentY, contactPaint)
        currentY += 22f

        // Objective
        if (data.careerObjective.isNotBlank()) {
            currentY = drawJobSectionHeader(canvas, "SUMMARY", contentLeft, contentRight, currentY, Color.BLACK, Color.BLACK)
            currentY += 14f
            val objPaint = Paint().apply { color = Color.BLACK; textSize = 9.5f; isAntiAlias = true }
            currentY = drawWrappedText(canvas, data.careerObjective, contentLeft, currentY, contentRight - contentLeft, 13f, objPaint)
            currentY += 16f
        }

        // Experience
        if (data.workExperience.isNotEmpty()) {
            currentY = drawJobSectionHeader(canvas, "WORK EXPERIENCE", contentLeft, contentRight, currentY, Color.BLACK, Color.BLACK)
            currentY += 16f

            data.workExperience.forEach { exp ->
                val titlePaint = Paint().apply { color = Color.BLACK; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                canvas.drawText(exp.jobTitle, contentLeft, currentY, titlePaint)

                if (exp.duration.isNotBlank()) {
                    val datePaint = Paint().apply { color = Color.BLACK; textSize = 9.5f; textAlign = Paint.Align.RIGHT; isAntiAlias = true }
                    canvas.drawText(exp.duration, contentRight, currentY, datePaint)
                }
                currentY += 13f

                if (exp.company.isNotBlank()) {
                    val compPaint = Paint().apply { color = Color.DKGRAY; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                    canvas.drawText(exp.company, contentLeft, currentY, compPaint)
                    currentY += 13f
                }

                if (exp.responsibilities.isNotBlank()) {
                    val respPaint = Paint().apply { color = Color.BLACK; textSize = 9.5f; isAntiAlias = true }
                    currentY = drawWrappedText(canvas, exp.responsibilities, contentLeft, currentY, contentRight - contentLeft, 12.5f, respPaint)
                }
                currentY += 12f
            }
            currentY += 6f
        }

        // Education
        currentY = drawJobSectionHeader(canvas, "EDUCATION", contentLeft, contentRight, currentY, Color.BLACK, Color.BLACK)
        currentY += 15f
        val eduList = listOf(
            "Post Graduation" to data.postGraduation,
            "Graduation" to data.graduation,
            "Intermediate (12th)" to data.intermediate,
            "High School (10th)" to data.highSchool
        ).filter { it.second.isNotBlank() }
        currentY = drawKeyValueTable(canvas, eduList, contentLeft, contentRight, currentY, 130f, Color.BLACK)
        currentY += 16f

        // Skills
        currentY = drawJobSectionHeader(canvas, "SKILLS & COMPETENCIES", contentLeft, contentRight, currentY, Color.BLACK, Color.BLACK)
        currentY += 15f
        val addList = listOf(
            "Technical Skills" to data.skills,
            "Languages" to data.languages,
            "Certifications" to data.certifications
        ).filter { it.second.isNotBlank() }
        drawKeyValueTable(canvas, addList, contentLeft, contentRight, currentY, 130f, Color.BLACK)
    }

    private fun renderJobCreativeAccent(
        canvas: Canvas,
        data: JobBiodata,
        template: BiodataTemplate,
        primary: Int,
        secondary: Int,
        accent: Int,
        photo: Bitmap?
    ) {
        // Modern geometric header angle
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(PAGE_WIDTH.toFloat(), 0f)
            lineTo(PAGE_WIDTH.toFloat(), 95f)
            lineTo(0f, 130f)
            close()
        }
        val headerPaint = Paint().apply { color = primary; style = Paint.Style.FILL }
        canvas.drawPath(path, headerPaint)

        // Accent strip
        val accentPath = Path().apply {
            moveTo(0f, 130f)
            lineTo(PAGE_WIDTH.toFloat(), 95f)
            lineTo(PAGE_WIDTH.toFloat(), 102f)
            lineTo(0f, 137f)
            close()
        }
        val stripPaint = Paint().apply { color = accent; style = Paint.Style.FILL }
        canvas.drawPath(accentPath, stripPaint)

        val namePaint = Paint().apply { color = Color.WHITE; textSize = 24f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val displayName = if (data.fullName.isNotBlank()) data.fullName else "Full Name"
        canvas.drawText(displayName, 45f, 50f, namePaint)

        val rolePaint = Paint().apply { color = accent; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val targetRole = if (data.targetRole.isNotBlank()) data.targetRole else "Creative Professional"
        canvas.drawText(targetRole, 45f, 72f, rolePaint)

        val contactLine = listOf(data.mobile, data.email, data.address).filter { it.isNotBlank() }.joinToString(" • ")
        val contactPaint = Paint().apply { color = Color.WHITE; textSize = 9.5f; isAntiAlias = true }
        canvas.drawText(contactLine, 45f, 92f, contactPaint)

        if (photo != null) {
            drawPhotoWithFrame(canvas, photo, PAGE_WIDTH - 130f, 25f, 85f, 85f, Color.WHITE, primary)
        }

        var currentY = 160f
        val contentLeft = 45f
        val contentRight = PAGE_WIDTH - 45f

        if (data.careerObjective.isNotBlank()) {
            currentY = drawJobSectionHeader(canvas, "ABOUT ME", contentLeft, contentRight, currentY, primary, accent)
            currentY += 15f
            val objPaint = Paint().apply { color = Color.parseColor("#334155"); textSize = 9.5f; isAntiAlias = true }
            currentY = drawWrappedText(canvas, data.careerObjective, contentLeft, currentY, contentRight - contentLeft, 13f, objPaint)
            currentY += 16f
        }

        if (data.workExperience.isNotEmpty()) {
            currentY = drawJobSectionHeader(canvas, "EXPERIENCE", contentLeft, contentRight, currentY, primary, accent)
            currentY += 16f
            data.workExperience.forEach { exp ->
                val titlePaint = Paint().apply { color = primary; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
                canvas.drawText(exp.jobTitle, contentLeft, currentY, titlePaint)
                currentY += 13f
                if (exp.company.isNotBlank()) {
                    val compPaint = Paint().apply { color = Color.DKGRAY; textSize = 10f; isAntiAlias = true }
                    canvas.drawText(exp.company, contentLeft, currentY, compPaint)
                    currentY += 13f
                }
                if (exp.responsibilities.isNotBlank()) {
                    val respPaint = Paint().apply { color = Color.parseColor("#475569"); textSize = 9f; isAntiAlias = true }
                    currentY = drawWrappedText(canvas, exp.responsibilities, contentLeft, currentY, contentRight - contentLeft, 12f, respPaint)
                }
                currentY += 10f
            }
            currentY += 8f
        }

        currentY = drawJobSectionHeader(canvas, "EDUCATION & QUALIFICATIONS", contentLeft, contentRight, currentY, primary, accent)
        currentY += 15f
        val eduList = listOf(
            "Graduation" to data.graduation,
            "Intermediate" to data.intermediate,
            "High School" to data.highSchool
        ).filter { it.second.isNotBlank() }
        currentY = drawKeyValueTable(canvas, eduList, contentLeft, contentRight, currentY, 130f, primary)
        currentY += 16f

        currentY = drawJobSectionHeader(canvas, "SKILLS & LANGUAGES", contentLeft, contentRight, currentY, primary, accent)
        currentY += 15f
        val addList = listOf(
            "Core Skills" to data.skills,
            "Languages" to data.languages,
            "Hobbies" to data.hobbies
        ).filter { it.second.isNotBlank() }
        drawKeyValueTable(canvas, addList, contentLeft, contentRight, currentY, 130f, primary)
    }

    // ==========================================
    // DRAWING HELPERS
    // ==========================================

    private fun drawMarriageBorders(
        canvas: Canvas,
        template: BiodataTemplate,
        primary: Int,
        accent: Int,
        secondary: Int
    ) {
        val margin = 20f
        when (template.borderStyle) {
            "DOUBLE_GOLD" -> {
                val outerPaint = Paint().apply {
                    color = primary
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                }
                canvas.drawRect(margin, margin, PAGE_WIDTH - margin, PAGE_HEIGHT - margin, outerPaint)

                val innerPaint = Paint().apply {
                    color = accent
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                }
                canvas.drawRect(margin + 5f, margin + 5f, PAGE_WIDTH - margin - 5f, PAGE_HEIGHT - margin - 5f, innerPaint)

                // Corner flourishes
                drawCornerMotif(canvas, margin + 5f, margin + 5f, accent)
                drawCornerMotif(canvas, PAGE_WIDTH - margin - 5f, margin + 5f, accent)
                drawCornerMotif(canvas, margin + 5f, PAGE_HEIGHT - margin - 5f, accent)
                drawCornerMotif(canvas, PAGE_WIDTH - margin - 5f, PAGE_HEIGHT - margin - 5f, accent)
            }
            "ORNATE" -> {
                val outerPaint = Paint().apply {
                    color = primary
                    strokeWidth = 2.5f
                    style = Paint.Style.STROKE
                }
                canvas.drawRoundRect(margin, margin, PAGE_WIDTH - margin, PAGE_HEIGHT - margin, 12f, 12f, outerPaint)
                val innerPaint = Paint().apply {
                    color = accent
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                }
                canvas.drawRoundRect(margin + 4f, margin + 4f, PAGE_WIDTH - margin - 4f, PAGE_HEIGHT - margin - 4f, 10f, 10f, innerPaint)
            }
            "HAIRLINE" -> {
                val paint = Paint().apply {
                    color = Color.parseColor("#E2E8F0")
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                }
                canvas.drawRect(margin, margin, PAGE_WIDTH - margin, PAGE_HEIGHT - margin, paint)
            }
        }
    }

    private fun drawCornerMotif(canvas: Canvas, x: Float, y: Float, color: Int) {
        val paint = Paint().apply {
            this.color = color
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        canvas.drawCircle(x, y, 6f, paint)
    }

    private fun drawMarriageSectionHeader(
        canvas: Canvas,
        title: String,
        left: Float,
        right: Float,
        y: Float,
        primary: Int,
        secondary: Int,
        accent: Int,
        isSerif: Boolean
    ): Float {
        val height = 24f
        val rect = RectF(left, y, right, y + height)

        val bgPaint = Paint().apply { color = secondary; style = Paint.Style.FILL }
        canvas.drawRoundRect(rect, 4f, 4f, bgPaint)

        // Accent left edge bar
        val barPaint = Paint().apply { color = primary; style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(left, y, left + 4f, y + height), 2f, 2f, barPaint)

        val textPaint = Paint().apply {
            color = primary
            textSize = 12f
            typeface = Typeface.create(if (isSerif) Typeface.SERIF else Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(title, left + 14f, y + 16f, textPaint)

        return y + height
    }

    private fun drawJobSectionHeader(
        canvas: Canvas,
        title: String,
        left: Float,
        right: Float,
        y: Float,
        primary: Int,
        accent: Int
    ): Float {
        val textPaint = Paint().apply {
            color = primary
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
            isAntiAlias = true
        }
        canvas.drawText(title, left, y, textPaint)

        val linePaint = Paint().apply {
            color = accent
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        canvas.drawLine(left, y + 5f, right, y + 5f, linePaint)

        return y + 9f
    }

    private fun drawKeyValueTable(
        canvas: Canvas,
        items: List<Pair<String, String>>,
        left: Float,
        right: Float,
        startY: Float,
        labelWidth: Float,
        primaryColor: Int,
        labelColor: Int = Color.parseColor("#475569"),
        valueColor: Int = Color.parseColor("#0F172A")
    ): Float {
        var currentY = startY

        val labelPaint = Paint().apply {
            color = labelColor
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val valuePaint = Paint().apply {
            color = valueColor
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val colonPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            isAntiAlias = true
        }

        items.forEach { (label, value) ->
            canvas.drawText(label, left, currentY, labelPaint)
            canvas.drawText(":", left + labelWidth, currentY, colonPaint)

            val valueLeft = left + labelWidth + 10f
            val availableWidth = right - valueLeft
            currentY = drawWrappedText(canvas, value, valueLeft, currentY, availableWidth, 13f, valuePaint)
            currentY += 5f
        }

        return currentY
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        lineHeight: Float,
        paint: Paint
    ): Float {
        if (text.isBlank()) return y

        val words = text.split(Regex("\\s+"))
        var currentLine = ""
        var currentY = y

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width > maxWidth && currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, x, currentY, paint)
                currentY += lineHeight
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
        }
        return currentY
    }

    private fun drawPhotoWithFrame(
        canvas: Canvas,
        bitmap: Bitmap,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        borderColor: Int,
        shadowColor: Int
    ) {
        val rect = RectF(x, y, x + width, y + height)

        // Frame background
        val borderPaint = Paint().apply {
            color = borderColor
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        // Clip and draw bitmap inside rounded rect
        val innerRect = RectF(x + 2f, y + 2f, x + width - 2f, y + height - 2f)
        val path = Path().apply { addRoundRect(innerRect, 6f, 6f, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(bitmap, null, innerRect, null)
        canvas.restore()
    }

    private fun loadAndCropPhoto(
        context: Context,
        uriString: String?,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (original != null) {
                Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load photo bitmap: ${e.message}")
            null
        }
    }

    private fun parseColor(hex: String?, fallback: Int): Int {
        if (hex.isNullOrBlank()) return fallback
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            fallback
        }
    }
}
