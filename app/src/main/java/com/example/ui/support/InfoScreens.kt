package com.example.ui.support

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Your Privacy Matters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "Biodata Maker is architected with a strict on-device, privacy-first commitment. Personal matrimonial and employment information is sensitive and belongs solely to you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                PolicySection(
                    title = "1. Data Storage & Local Persistence",
                    body = "All personal details, photos, family records, horoscope entries, and career histories you enter are stored locally inside your device's secured SQLite/Room database. We do not upload your personal biodata entries to external advertising servers."
                )
            }

            item {
                PolicySection(
                    title = "2. Photos & Media Handling",
                    body = "When you choose a profile picture, it is processed on your device using the modern Android Photo Picker. Images are compressed locally and embedded directly into your generated A4 PDF file."
                )
            }

            item {
                PolicySection(
                    title = "3. Payment & Transaction Security",
                    body = "Payments for premium templates or eBooks are processed securely via Razorpay's PCI-DSS compliant checkout. Biodata Maker never accesses, stores, or transmits credit/debit card numbers or UPI PINs."
                )
            }

            item {
                PolicySection(
                    title = "4. AI Writing Enhancements",
                    body = "When you choose to use the Gemini AI writing assistant for objectives or profiles, only the specific snippet you requested to enhance is processed to provide writing suggestions. AI never overwrites your original text without your explicit review and confirmation."
                )
            }

            item {
                PolicySection(
                    title = "5. Data Deletion",
                    body = "You have full control. You can delete any saved biodata draft or generated document at any time from the 'My Documents' screen, which permanently erases the data from your device."
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Terms of Service", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Effective Date: October 2026", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            item {
                PolicySection(
                    title = "1. License & Usage",
                    body = "By using Biodata Maker, you receive a personal license to create, download, print, and distribute your generated Marriage and Job Biodata PDFs for personal and professional applications."
                )
            }

            item {
                PolicySection(
                    title = "2. User Responsibility",
                    body = "You are solely responsible for ensuring that all information, qualifications, employment records, and family information stated in your biodata are truthful, accurate, and lawfully yours."
                )
            }

            item {
                PolicySection(
                    title = "3. Template Purchases & Refunds",
                    body = "Premium template purchases unlock instant PDF generation capabilities. Because digital documents are delivered immediately upon payment, fees are non-refundable once the PDF is generated. If you encounter any technical issues, our support team will assist you promptly."
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(supportEmail: String, onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Need Assistance?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "We are here to help with PDF downloads, template customization, or payment questions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$supportEmail")
                                    putExtra(Intent.EXTRA_SUBJECT, "Biodata Maker App Support Request")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            }
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Email: $supportEmail")
                        }
                    }
                }
            }

            item {
                Text("Frequently Asked Questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                FaqItem(
                    question = "How do I print my biodata on standard A4 paper?",
                    answer = "When your PDF is generated, tap 'Open' or 'Share' and select your printer. All templates are formatted with standard A4 margins (595 x 842 pt) for crisp, clean home and professional printing."
                )
            }

            item {
                FaqItem(
                    question = "Are my drafts saved if I close the app?",
                    answer = "Yes! Biodata Maker automatically saves your draft locally as you type. You can resume anytime from 'My Documents' on the Home screen."
                )
            }

            item {
                FaqItem(
                    question = "Can I change my template after filling the form?",
                    answer = "Absolutely. On the Preview screen, tap 'Change Template' on the top bar to switch to any other layout while keeping your entered information intact."
                )
            }

            item {
                FaqItem(
                    question = "How does the Gemini AI assistant work?",
                    answer = "Tap 'AI Improve' beside Career Objective or About Me. Gemini analyzes your draft and generates a polished, professional alternative. You can compare the original and improved versions side-by-side before deciding whether to use it."
                )
            }
        }
    }
}

@Composable
fun PolicySection(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FaqItem(question: String, answer: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(answer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
