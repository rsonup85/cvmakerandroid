package com.example.ui.ebook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.EBook
import com.example.engine.EBookRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EBookStoreScreen(
    onPurchaseEBook: (EBook) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var viewingEBook by remember { mutableStateOf<EBook?>(null) }

    val allEBooks = remember { EBookRegistry.getAllEBooks() }
    val categories = remember { EBookRegistry.getCategories() }

    val filteredEBooks = remember(selectedCategory) {
        if (selectedCategory == "All") allEBooks
        else allEBooks.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("eBook Career & Wedding Store", fontWeight = FontWeight.Bold) },
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
            // Category Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredEBooks, key = { it.id }) { ebook ->
                    val isUnlocked = EBookRegistry.isUnlocked(ebook)
                    val coverColor = remember(ebook.coverColor) {
                        try { Color(android.graphics.Color.parseColor(ebook.coverColor)) } catch (e: Exception) { Color(0xFF1E3A8A) }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewingEBook = ebook },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Book Cover Representation
                            Box(
                                modifier = Modifier
                                    .width(76.dp)
                                    .height(108.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(coverColor)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Book, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ebook.title,
                                        color = Color.White,
                                        fontSize = 8.5.sp,
                                        maxLines = 3,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = ebook.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "${ebook.rating}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = ebook.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )

                                Text(
                                    text = "By ${ebook.author}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = ebook.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isUnlocked) "UNLOCKED" else if (ebook.isPaid) "₹${ebook.price}" else "FREE",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = if (isUnlocked || !ebook.isPaid) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                                    )

                                    if (isUnlocked) {
                                        Button(
                                            onClick = { viewingEBook = ebook },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text("Read Guide", fontSize = 12.sp)
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = { onPurchaseEBook(ebook) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text("Buy Now", fontSize = 12.sp)
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

    // eBook Chapter Reader / Details Dialog
    viewingEBook?.let { ebook ->
        val isUnlocked = EBookRegistry.isUnlocked(ebook)
        Dialog(onDismissRequest = { viewingEBook = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(ebook.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("By ${ebook.author} • ${ebook.pageCount} Pages", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(ebook.description, style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Included Chapters & Curriculum:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))

                    ebook.chapters.forEach { chapter ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(chapter, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewingEBook = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close")
                        }

                        if (!isUnlocked) {
                            Button(
                                onClick = {
                                    val target = viewingEBook
                                    viewingEBook = null
                                    if (target != null) onPurchaseEBook(target)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Unlock ₹${ebook.price}")
                            }
                        }
                    }
                }
            }
        }
    }
}
