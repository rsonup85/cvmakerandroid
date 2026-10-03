package com.example.ui.template

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BiodataTemplate
import com.example.data.model.BiodataType
import com.example.engine.TemplateRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateSelectionScreen(
    initialType: BiodataType,
    onTemplateChosen: (BiodataTemplate) -> Unit,
    onBack: () -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var filterPaid by remember { mutableStateOf("ALL") } // "ALL", "FREE", "PAID"

    val allTemplates = remember(selectedType) {
        TemplateRegistry.getTemplatesByType(selectedType)
    }

    val categories = remember(selectedType) {
        listOf("All") + TemplateRegistry.getCategories(selectedType)
    }

    val filteredTemplates = remember(allTemplates, searchQuery, selectedCategory, filterPaid) {
        allTemplates.filter { template ->
            val matchesSearch = template.name.contains(searchQuery, ignoreCase = true) ||
                    template.category.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || template.category == selectedCategory
            val matchesPaid = when (filterPaid) {
                "FREE" -> !template.isPaid || template.price == 0
                "PAID" -> template.isPaid && template.price > 0
                else -> true
            }
            matchesSearch && matchesCategory && matchesPaid
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedType == BiodataType.MARRIAGE) "Marriage Templates" else "Job Resume Templates",
                        fontWeight = FontWeight.Bold
                    )
                },
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
            // Type Switcher Tabs (Marriage vs Job)
            TabRow(
                selectedTabIndex = if (selectedType == BiodataType.MARRIAGE) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedType == BiodataType.MARRIAGE,
                    onClick = {
                        selectedType = BiodataType.MARRIAGE
                        selectedCategory = "All"
                    },
                    text = { Text("Marriage (55+)") },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedType == BiodataType.JOB,
                    onClick = {
                        selectedType = BiodataType.JOB
                        selectedCategory = "All"
                    },
                    text = { Text("Job & Resume (55+)") },
                    icon = { Icon(Icons.Default.Work, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Search Bar & Quick Filters
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, royal, modern, ATS...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Price Filter Chips (All, Free, Paid)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterPaid == "ALL",
                        onClick = { filterPaid = "ALL" },
                        label = { Text("All (${allTemplates.size})") }
                    )
                    FilterChip(
                        selected = filterPaid == "FREE",
                        onClick = { filterPaid = "FREE" },
                        label = { Text("Free Only") }
                    )
                    FilterChip(
                        selected = filterPaid == "PAID",
                        onClick = { filterPaid = "PAID" },
                        label = { Text("Premium Paid") }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Category Scrollable Chips
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    divider = {},
                    containerColor = Color.Transparent
                ) {
                    categories.forEach { cat ->
                        Tab(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            text = { Text(cat, fontSize = 12.sp, fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            // Template Grid
            if (filteredTemplates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No templates found matching your filters", fontWeight = FontWeight.Bold)
                        Text("Try searching for 'Royal', 'Modern', 'ATS' or clear filters.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredTemplates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            onSelect = { onTemplateChosen(template) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateCard(
    template: BiodataTemplate,
    onSelect: () -> Unit
) {
    val primaryColor = remember(template.primaryColorHex) {
        try { Color(android.graphics.Color.parseColor(template.primaryColorHex)) } catch (e: Exception) { Color(0xFF1E3A8A) }
    }
    val secondaryColor = remember(template.secondaryColorHex) {
        try { Color(android.graphics.Color.parseColor(template.secondaryColorHex)) } catch (e: Exception) { Color(0xFFF1F5F9) }
    }
    val accentColor = remember(template.accentColorHex) {
        try { Color(android.graphics.Color.parseColor(template.accentColorHex)) } catch (e: Exception) { Color(0xFFD97706) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            // Visual Miniature Canvas Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(android.graphics.Color.parseColor(template.backgroundColorHex)))
                    .border(
                        width = if (template.borderStyle == "DOUBLE_GOLD") 2.dp else 1.dp,
                        color = if (template.borderStyle == "DOUBLE_GOLD") accentColor else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            ) {
                // Layout visual simulator
                when (template.layoutType) {
                    "SIDEBAR_LEFT" -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left sidebar
                            Box(
                                modifier = Modifier
                                    .width(46.dp)
                                    .fillMaxHeight()
                                    .background(primaryColor)
                                    .padding(4.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.5f)))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.3f)))
                                }
                            }
                            // Main body
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp)
                            ) {
                                Box(modifier = Modifier.width(60.dp).height(6.dp).background(primaryColor))
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.LightGray))
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.8f).height(3.dp).background(Color.LightGray))
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.width(40.dp).height(4.dp).background(accentColor))
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.LightGray))
                            }
                        }
                    }
                    "EXECUTIVE_CORPORATE" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .background(primaryColor)
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Box(modifier = Modifier.width(50.dp).height(5.dp).background(Color.White))
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Box(modifier = Modifier.width(35.dp).height(3.dp).background(accentColor))
                                    }
                                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White))
                                }
                            }
                            Column(modifier = Modifier.padding(8.dp)) {
                                Box(modifier = Modifier.width(45.dp).height(4.dp).background(primaryColor))
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.LightGray))
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.width(45.dp).height(4.dp).background(primaryColor))
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.LightGray))
                            }
                        }
                    }
                    else -> {
                        // Classic / Royal header & body
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (template.headerStyle == "GANESHA") {
                                Text("ॐ", fontSize = 14.sp, color = primaryColor)
                            } else {
                                Text("✦", fontSize = 12.sp, color = accentColor)
                            }
                            Box(modifier = Modifier.width(55.dp).height(6.dp).background(primaryColor))
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .border(1.dp, accentColor, RoundedCornerShape(4.dp))
                                    .background(secondaryColor)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth(0.9f).height(3.dp).background(Color.LightGray))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth(0.7f).height(3.dp).background(Color.LightGray))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth(0.85f).height(3.dp).background(Color.LightGray))
                        }
                    }
                }

                // Free or Price Tag on top right
                Surface(
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    color = if (template.isPaid && template.price > 0) MaterialTheme.colorScheme.primary else Color(0xFF16A34A),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = if (template.isPaid && template.price > 0) "₹${template.price}" else "FREE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Category pill on bottom left
                Surface(
                    shape = RoundedCornerShape(topEnd = 8.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = template.category,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Card Footer: Title & Use Button
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = template.layoutType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSelect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("Use Template", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
