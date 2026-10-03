package com.example.ui.documents

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.BiodataEntity
import com.example.data.local.BiodataJsonParser
import com.example.data.local.PaymentEntity
import com.example.data.model.JobBiodata
import com.example.data.model.MarriageBiodata
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDocumentsScreen(
    documents: List<BiodataEntity>,
    payments: List<PaymentEntity>,
    onEditMarriage: (MarriageBiodata) -> Unit,
    onEditJob: (JobBiodata) -> Unit,
    onViewPdf: (String) -> Unit = {},
    onDeleteDocument: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Biodatas, 1 = Payment History

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Library & Orders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Biodatas (${documents.size})") },
                    icon = { Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Payment History (${payments.size})") },
                    icon = { Icon(Icons.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTab == 0) {
                if (documents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No biodata documents created yet", fontWeight = FontWeight.Bold)
                            Text(
                                "Start by creating an elegant Marriage or Job Biodata from the Home screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(documents, key = { it.id }) { doc ->
                            DocumentItemCard(
                                doc = doc,
                                onEdit = {
                                    if (doc.type == "MARRIAGE") {
                                        val m = BiodataJsonParser.marriageFromJson(doc.jsonData)
                                        if (m != null) onEditMarriage(m)
                                    } else {
                                        val j = BiodataJsonParser.jobFromJson(doc.jsonData)
                                        if (j != null) onEditJob(j)
                                    }
                                },
                                onOpenPdf = { path ->
                                    onViewPdf(path)
                                },
                                onSharePdf = { path ->
                                    val file = File(path)
                                    if (file.exists()) {
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
                                            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onDelete = { onDeleteDocument(doc.id) }
                            )
                        }
                    }
                }
            } else {
                // Payment History List
                if (payments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No payment transactions recorded yet", fontWeight = FontWeight.Bold)
                            Text(
                                "Purchased premium templates and eBooks will appear here with invoice receipts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(payments, key = { it.id }) { pay ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pay.itemName, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "Order ID: ${pay.id}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Payment Ref: ${pay.paymentId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(pay.timestamp))
                                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹${pay.amount}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF16A34A)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFDCFCE7)
                                        ) {
                                            Text(
                                                text = "PAID",
                                                color = Color(0xFF16A34A),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentItemCard(
    doc: BiodataEntity,
    onEdit: () -> Unit,
    onOpenPdf: (String) -> Unit,
    onSharePdf: (String) -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(doc.updatedAt) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(doc.updatedAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (doc.type == "MARRIAGE") Color(0xFFFDE8E8) else Color(0xFFE1EFFE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (doc.type == "MARRIAGE") Icons.Default.Favorite else Icons.Default.Work,
                            contentDescription = null,
                            tint = if (doc.type == "MARRIAGE") Color(0xFFE02424) else Color(0xFF1E40AF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(doc.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${if (doc.type == "MARRIAGE") "Marriage" else "Job"} Biodata • $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                }

                if (!doc.pdfPath.isNullOrBlank()) {
                    FilledTonalButton(
                        onClick = { onOpenPdf(doc.pdfPath) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = { onSharePdf(doc.pdfPath) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
