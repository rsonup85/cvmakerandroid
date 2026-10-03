package com.example.ui.preview

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.BiodataTemplate
import com.example.data.model.BiodataType
import com.example.data.model.JobBiodata
import com.example.data.model.MarriageBiodata
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiodataPreviewScreen(
    type: BiodataType,
    template: BiodataTemplate,
    marriageData: MarriageBiodata,
    jobData: JobBiodata,
    isUnlocked: Boolean,
    generatedPdfFile: File?,
    isGeneratingPdf: Boolean,
    onEditClicked: () -> Unit,
    onChangeTemplateClicked: () -> Unit,
    onPayClicked: () -> Unit,
    onGeneratePdfClicked: () -> Unit,
    onSharePdfClicked: () -> Unit,
    onEmailPdfClicked: (email: String, name: String) -> Unit,
    onViewPdfRendererPreview: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember {
        mutableStateOf(if (type == BiodataType.MARRIAGE) marriageData.email else jobData.email)
    }

    val primaryColor = remember(template.primaryColorHex) {
        try { Color(android.graphics.Color.parseColor(template.primaryColorHex)) } catch (e: Exception) { Color(0xFF1E3A8A) }
    }
    val secondaryColor = remember(template.secondaryColorHex) {
        try { Color(android.graphics.Color.parseColor(template.secondaryColorHex)) } catch (e: Exception) { Color(0xFFF1F5F9) }
    }
    val accentColor = remember(template.accentColorHex) {
        try { Color(android.graphics.Color.parseColor(template.accentColorHex)) } catch (e: Exception) { Color(0xFFD97706) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("A4 Document Preview", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditClicked) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit Form")
                    }
                    IconButton(onClick = onChangeTemplateClicked) {
                        Icon(Icons.Outlined.Palette, contentDescription = "Change Template")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (generatedPdfFile != null) {
                        // PDF Generated - Actions Row
                        Text(
                            text = "✅ Your high-quality PDF is ready!",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = onViewPdfRendererPreview,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FindInPage, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View A4 Preview (PdfRenderer)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    // Open PDF viewer intent
                                    try {
                                        val uri: Uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            generatedPdfFile
                                        )
                                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(viewIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No PDF viewer found. You can Share or Email it.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = onSharePdfClicked,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = { showEmailDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Email", fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Not generated yet
                        if (!isUnlocked && template.isPaid && template.price > 0) {
                            Button(
                                onClick = onPayClicked,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pay ₹${template.price} & Download PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        } else {
                            Button(
                                onClick = onGeneratePdfClicked,
                                enabled = !isGeneratingPdf,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isGeneratingPdf) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generating High-Res A4 PDF...")
                                } else {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate Free A4 PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Template Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(template.name, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${template.category} • Layout: ${template.layoutType.replace("_", " ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (template.isPaid && template.price > 0 && !isUnlocked) MaterialTheme.colorScheme.primary else Color(0xFF16A34A)
                    ) {
                        Text(
                            text = if (isUnlocked || !template.isPaid) "READY" else "₹${template.price}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Realistic A4 Paper Sheet Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.707f) // Exact A4 ratio: 1 / 1.414 = ~0.707
                    .shadow(12.dp, RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(android.graphics.Color.parseColor(template.backgroundColorHex)))
                    .border(
                        width = if (template.borderStyle == "DOUBLE_GOLD") 2.dp else 1.dp,
                        color = if (template.borderStyle == "DOUBLE_GOLD") accentColor else Color.LightGray
                    )
            ) {
                if (type == BiodataType.MARRIAGE) {
                    MarriageLivePreview(
                        data = marriageData,
                        template = template,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        accentColor = accentColor
                    )
                } else {
                    JobLivePreview(
                        data = jobData,
                        template = template,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        accentColor = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Email Send Dialog
    if (showEmailDialog) {
        val userName = if (type == BiodataType.MARRIAGE) marriageData.fullName else jobData.fullName
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Email Biodata PDF") },
            text = {
                Column {
                    Text("Enter email address to send the generated A4 PDF:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmailDialog = false
                        onEmailPdfClicked(emailInput, userName)
                    }
                ) {
                    Text("Send Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MarriageLivePreview(
    data: MarriageBiodata,
    template: BiodataTemplate,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Auspicious Header
        if (template.headerStyle == "GANESHA") {
            Text(
                text = "|| श्री गणेशाय नमः ||",
                color = primaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = "❖  BIODATA  ❖",
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Name & Photo Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (data.fullName.isNotBlank()) data.fullName else "Full Name",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                if (data.occupation.isNotBlank()) {
                    Text(data.occupation, fontSize = 9.sp, color = Color.DarkGray)
                }
                if (data.education.isNotBlank()) {
                    Text(data.education, fontSize = 8.5.sp, color = Color.Gray)
                }
            }

            if (!data.profilePhotoUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.5.dp, accentColor, RoundedCornerShape(6.dp))
                ) {
                    AsyncImage(
                        model = data.profilePhotoUri,
                        contentDescription = "Preview Photo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = accentColor.copy(alpha = 0.5f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(6.dp))

        // Personal Details Miniature Table
        Text(
            text = "Personal Details",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor,
            modifier = Modifier
                .background(secondaryColor, RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))

        listOf(
            "DOB" to data.dateOfBirth,
            "Birth Place" to data.birthPlace,
            "Rashi / Nakshatra" to (data.rashi + (if (data.nakshatra.isNotBlank()) " / ${data.nakshatra}" else "")),
            "Religion & Caste" to (data.religion + (if (data.caste.isNotBlank()) " - ${data.caste}" else "")),
            "Height" to data.height,
            "Education" to data.education,
            "Income" to data.annualIncome
        ).filter { it.second.isNotBlank() }.take(5).forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), color = Color.DarkGray)
                Text(": $value", fontSize = 8.sp, color = Color.Black, maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Family Details Miniature Table
        Text(
            text = "Family Background",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor,
            modifier = Modifier
                .background(secondaryColor, RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))

        listOf(
            "Father" to data.fatherName,
            "Mother" to data.motherName,
            "Brothers" to data.brothers,
            "Native Place" to data.nativePlace
        ).filter { it.second.isNotBlank() }.take(3).forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), color = Color.DarkGray)
                Text(": $value", fontSize = 8.sp, color = Color.Black, maxLines = 1)
            }
        }

        if (data.showContactOnPdf) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Contact Information",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier
                    .background(secondaryColor, RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (data.mobile.isNotBlank()) {
                Text("Mobile: ${data.mobile}", fontSize = 8.sp)
            }
            if (data.email.isNotBlank()) {
                Text("Email: ${data.email}", fontSize = 8.sp)
            }
        }
    }
}

@Composable
fun JobLivePreview(
    data: JobBiodata,
    template: BiodataTemplate,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Corporate Top Header Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryColor, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (data.fullName.isNotBlank()) data.fullName else "Full Name",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (data.targetRole.isNotBlank()) data.targetRole else "Professional",
                        fontSize = 9.sp,
                        color = accentColor
                    )
                }

                if (!data.profilePhotoUri.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                    ) {
                        AsyncImage(
                            model = data.profilePhotoUri,
                            contentDescription = "Job Photo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = listOf(data.mobile, data.email, data.address).filter { it.isNotBlank() }.joinToString(" • "),
            fontSize = 7.5.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (data.careerObjective.isNotBlank()) {
            Text("CAREER OBJECTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Divider(color = accentColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(data.careerObjective, fontSize = 7.5.sp, maxLines = 2, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (data.workExperience.isNotEmpty()) {
            Text("WORK EXPERIENCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Divider(color = accentColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(3.dp))
            val exp = data.workExperience.first()
            Text("${exp.jobTitle} - ${exp.company}", fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(exp.responsibilities, fontSize = 7.5.sp, maxLines = 2, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(6.dp))
        }

        Text("EDUCATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = primaryColor)
        Divider(color = accentColor, thickness = 1.dp)
        Spacer(modifier = Modifier.height(3.dp))
        if (data.graduation.isNotBlank()) {
            Text("• ${data.graduation}", fontSize = 7.5.sp)
        }
        if (data.intermediate.isNotBlank()) {
            Text("• ${data.intermediate}", fontSize = 7.5.sp)
        }

        if (data.skills.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("SKILLS & COMPETENCIES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Divider(color = accentColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(data.skills, fontSize = 7.5.sp, maxLines = 2)
        }
    }
}
