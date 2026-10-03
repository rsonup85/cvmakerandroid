package com.example.ui.preview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A dedicated PDF preview screen that uses Android's native [PdfRenderer] API
 * to render and display a high-fidelity, non-interactive rasterized preview of
 * the generated biodata PDF before final download, share, or email actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFPreviewScreen(
    pdfFile: File,
    title: String = "Biodata Preview",
    isPaid: Boolean = false,
    isUnlocked: Boolean = true,
    price: Int = 0,
    userEmail: String = "",
    userName: String = "",
    onPayToUnlock: () -> Unit,
    onEditClicked: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var renderedPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var renderError by remember { mutableStateOf<String?>(null) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf(userEmail) }

    // Render PDF pages on IO thread using Android's PdfRenderer API
    LaunchedEffect(pdfFile) {
        isLoading = true
        renderError = null
        try {
            val bitmaps = withContext(Dispatchers.IO) {
                renderPdfPages(pdfFile)
            }
            renderedPages = bitmaps
            isLoading = false
        } catch (e: Exception) {
            renderError = e.localizedMessage ?: "Failed to render PDF preview"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            text = "Rendered A4 Document Preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditClicked) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit Details")
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
                    if (isPaid && !isUnlocked) {
                        // Locked template action
                        Button(
                            onClick = onPayToUnlock,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pay ₹$price to Unlock & Download PDF", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Unlocked: Download / Share / Email actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { openPdfInViewer(context, pdfFile) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = { sharePdf(context, pdfFile) },
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
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(38.dp))
                        Text(
                            text = "Rendering high-precision PDF preview...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                renderError != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Preview Error", fontWeight = FontWeight.Bold)
                        Text(
                            text = renderError ?: "Unable to render PDF",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { openPdfInViewer(context, pdfFile) }) {
                            Text("Open in External Viewer")
                        }
                    }
                }

                renderedPages.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header info chip
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "A4 Page Format • 595 x 842 pt • Print Ready",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Rendered non-interactive page(s)
                        itemsIndexed(renderedPages) { index, bitmap ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Non-interactive A4 Page Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.95f)
                                        .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                                        .shadow(12.dp, RoundedCornerShape(6.dp))
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White)
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "PDF Page ${index + 1}",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Text(
                                    text = "Page ${index + 1} of ${renderedPages.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Email delivery dialog
    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Email Biodata PDF") },
            text = {
                Column {
                    Text(
                        text = "Enter recipient email address to send the generated PDF:",
                        style = MaterialTheme.typography.bodySmall
                    )
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
                        sendPdfViaEmail(context, pdfFile, emailInput, userName)
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

/**
 * Uses native [PdfRenderer] to rasterize each page of [pdfFile] into a crisp [Bitmap].
 */
private fun renderPdfPages(pdfFile: File): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()
    val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(pfd)

    for (pageIndex in 0 until renderer.pageCount) {
        val page = renderer.openPage(pageIndex)
        // Render at 2x resolution (144 dpi) for razor-sharp visual preview
        val scale = 2
        val width = page.width * scale
        val height = page.height * scale

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        bitmaps.add(bitmap)
    }

    renderer.close()
    pfd.close()
    return bitmaps
}

private fun openPdfInViewer(context: Context, file: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(viewIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "No PDF viewer application found on device.", Toast.LENGTH_SHORT).show()
    }
}

private fun sharePdf(context: Context, file: File) {
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

private fun sendPdfViaEmail(context: Context, file: File, email: String, recipientName: String) {
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            if (email.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            }
            putExtra(Intent.EXTRA_SUBJECT, "Biodata PDF - $recipientName")
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
        Toast.makeText(context, "Could not open email application: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
