package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppConfig
import com.example.data.model.BiodataType
import com.example.engine.TemplateRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appConfig: AppConfig,
    onNavigateToTemplates: (BiodataType) -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToEBooks: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onSecretAdminTrigger: () -> Unit
) {
    var secretClickCount by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_biodata_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    secretClickCount++
                                    if (secretClickCount >= 5) {
                                        secretClickCount = 0
                                        onSecretAdminTrigger()
                                    }
                                }
                        )
                        Column {
                            Text(
                                text = appConfig.appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Professional PDF Creator",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDocuments) {
                        Icon(Icons.Outlined.FolderShared, contentDescription = "My Documents")
                    }
                    IconButton(onClick = onNavigateToEBooks) {
                        Icon(Icons.Outlined.MenuBook, contentDescription = "eBook Store")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // Hero Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF1E3A8A),
                                        Color(0xFF3B82F6)
                                    )
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "110+ UNIQUE DESIGNS",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = appConfig.mainHeading,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = appConfig.subtitle,
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (appConfig.announcement.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Stars,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE047),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = appConfig.announcement,
                                        color = Color(0xFFFDE047),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Primary Biodata Categories
            item {
                Text(
                    text = "Select Biodata Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Marriage Biodata Card
            item {
                BiodataTypeCard(
                    title = "Marriage Biodata",
                    subtitle = "Create an elegant marriage biodata with horoscopes, family details & photos.",
                    templateCount = "55+ Templates",
                    icon = Icons.Default.Favorite,
                    iconBg = Color(0xFFFDE8E8),
                    iconTint = Color(0xFFE02424),
                    badgeText = "Royal & Traditional",
                    onClick = { onNavigateToTemplates(BiodataType.MARRIAGE) }
                )
            }

            // Job Biodata Card
            item {
                BiodataTypeCard(
                    title = "Job Biodata & Resume",
                    subtitle = "Create a professional career biodata with objective, experience, skills & ATS formats.",
                    templateCount = "55+ Templates",
                    icon = Icons.Default.Work,
                    iconBg = Color(0xFFE1EFFE),
                    iconTint = Color(0xFF1E40AF),
                    badgeText = "Corporate & Modern",
                    onClick = { onNavigateToTemplates(BiodataType.JOB) }
                )
            }

            // Integrated eBook Store Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToEBooks() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "eBook Store",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "NEW",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Career mastery, interview questions & wedding etiquette guides.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Tools & Storage Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "My Documents",
                        subtitle = "Saved drafts & PDFs",
                        icon = Icons.Outlined.Folder,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDocuments
                    )

                    QuickActionCard(
                        title = "Help & Support",
                        subtitle = "FAQs & contact",
                        icon = Icons.Outlined.HelpOutline,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSupport
                    )
                }
            }

            // Footer Links & Legal
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onNavigateToPrivacy) {
                                Text("Privacy Policy", fontSize = 12.sp)
                            }
                            Text("•", color = MaterialTheme.colorScheme.outline)
                            TextButton(onClick = onNavigateToTerms) {
                                Text("Terms & Conditions", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Biodata Maker v2.5.0 • 100% Secure & On-Device Drafts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clickable {
                                secretClickCount++
                                if (secretClickCount >= 5) {
                                    secretClickCount = 0
                                    onSecretAdminTrigger()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BiodataTypeCard(
    title: String,
    subtitle: String,
    templateCount: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    badgeText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🎨 $templateCount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
