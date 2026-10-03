package com.example.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppConfig
import com.example.data.model.BiodataTemplate
import com.example.data.remote.FirebaseManager
import com.example.engine.EBookRegistry
import com.example.engine.TemplateRegistry
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    currentConfig: AppConfig,
    onConfigUpdated: (AppConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isAdminLoggedIn by FirebaseManager.isAdminLoggedIn.collectAsState()

    var emailInput by remember { mutableStateOf("admin@biodatamaker.app") }
    var passwordInput by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    // Admin Tabs: 0 = Dashboard, 1 = App Settings, 2 = Templates, 3 = eBooks
    var selectedTab by remember { mutableStateOf(0) }

    // App Settings local state
    var appName by remember { mutableStateOf(currentConfig.appName) }
    var mainHeading by remember { mutableStateOf(currentConfig.mainHeading) }
    var subtitle by remember { mutableStateOf(currentConfig.subtitle) }
    var supportEmail by remember { mutableStateOf(currentConfig.supportEmail) }
    var announcement by remember { mutableStateOf(currentConfig.announcement) }
    var razorpayKeyId by remember { mutableStateOf(currentConfig.razorpayKeyId) }

    // Templates list
    var templatesList by remember { mutableStateOf(TemplateRegistry.getAllTemplates()) }
    var templateSearch by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Secure Admin Console", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdminLoggedIn) {
                        IconButton(onClick = {
                            FirebaseManager.logoutAdmin()
                            Toast.makeText(context, "Logged out of Admin", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Logout, contentDescription = "Logout")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!isAdminLoggedIn) {
            // Admin Authentication Barrier
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Administrator Login",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Restricted Area • Firebase Auth Protected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Admin Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Security Master Key / PIN") },
                            placeholder = { Text("Enter Master Admin Key") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (loginError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(loginError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                isLoggingIn = true
                                loginError = null
                                coroutineScope.launch {
                                    val success = FirebaseManager.verifyAndLoginAdmin(emailInput, passwordInput)
                                    isLoggingIn = false
                                    if (!success) {
                                        loginError = "Unauthorized: Invalid admin credentials or access key."
                                    } else {
                                        Toast.makeText(context, "Admin authenticated successfully", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isLoggingIn,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoggingIn) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text("Authenticate", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Admin Authorized Console
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
                        text = { Text("Dashboard") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("App Settings") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Templates (${templatesList.size})") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("eBooks") }
                    )
                }

                when (selectedTab) {
                    0 -> AdminDashboardTab()
                    1 -> AdminAppSettingsTab(
                        appName = appName,
                        onAppNameChange = { appName = it },
                        mainHeading = mainHeading,
                        onHeadingChange = { mainHeading = it },
                        subtitle = subtitle,
                        onSubtitleChange = { subtitle = it },
                        announcement = announcement,
                        onAnnouncementChange = { announcement = it },
                        supportEmail = supportEmail,
                        onSupportEmailChange = { supportEmail = it },
                        razorpayKeyId = razorpayKeyId,
                        onRazorpayChange = { razorpayKeyId = it },
                        onSave = {
                            val updated = currentConfig.copy(
                                appName = appName,
                                mainHeading = mainHeading,
                                subtitle = subtitle,
                                announcement = announcement,
                                supportEmail = supportEmail,
                                razorpayKeyId = razorpayKeyId
                            )
                            onConfigUpdated(updated)
                            FirebaseManager.updateConfigLocally(updated)
                            Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    2 -> AdminTemplatesTab(
                        templates = templatesList,
                        searchQuery = templateSearch,
                        onSearchChange = { templateSearch = it },
                        onTogglePaid = { template ->
                            val updated = template.copy(
                                isPaid = !template.isPaid,
                                price = if (!template.isPaid) 49 else 0
                            )
                            TemplateRegistry.updateTemplate(updated)
                            templatesList = TemplateRegistry.getAllTemplates()
                            Toast.makeText(context, "${template.name} is now ${if (updated.isPaid) "PAID (₹${updated.price})" else "FREE"}", Toast.LENGTH_SHORT).show()
                        },
                        onPriceChange = { template, newPrice ->
                            val updated = template.copy(isPaid = newPrice > 0, price = newPrice)
                            TemplateRegistry.updateTemplate(updated)
                            templatesList = TemplateRegistry.getAllTemplates()
                        },
                        onToggleEnabled = { template ->
                            val updated = template.copy(isEnabled = !template.isEnabled)
                            TemplateRegistry.updateTemplate(updated)
                            templatesList = TemplateRegistry.getAllTemplates()
                        }
                    )
                    3 -> AdminEBooksTab()
                }
            }
        }
    }
}

@Composable
fun AdminDashboardTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Analytics & Revenue Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Total Revenue", "₹14,850", Icons.Default.CurrencyRupee, Color(0xFF16A34A), Modifier.weight(1f))
                MetricCard("Purchases", "284", Icons.Default.ShoppingCart, Color(0xFF2563EB), Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Marriage Biodatas", "1,420", Icons.Default.Favorite, Color(0xFFE02424), Modifier.weight(1f))
                MetricCard("Job Biodatas", "1,890", Icons.Default.Work, Color(0xFF7C3AED), Modifier.weight(1f))
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recent Transaction Feed", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    listOf(
                        Triple("Rahul Sen", "Crown Sapphire Crest (Marriage)", "₹49"),
                        Triple("Pooja Nair", "Silicon Valley Creative Accent (Job)", "₹39"),
                        Triple("Deepak Verma", "Cracking the ATS eBook", "₹49"),
                        Triple("Suresh Mehta", "Imperial Maroon Palace (Marriage)", "₹49")
                    ).forEach { (user, item, price) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(user, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(item, fontSize = 11.sp, color = Color.Gray)
                            }
                            Text(price, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AdminAppSettingsTab(
    appName: String,
    onAppNameChange: (String) -> Unit,
    mainHeading: String,
    onHeadingChange: (String) -> Unit,
    subtitle: String,
    onSubtitleChange: (String) -> Unit,
    announcement: String,
    onAnnouncementChange: (String) -> Unit,
    supportEmail: String,
    onSupportEmailChange: (String) -> Unit,
    razorpayKeyId: String,
    onRazorpayChange: (String) -> Unit,
    onSave: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Branding & Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Changes update the app instantly without recompilation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            OutlinedTextField(
                value = appName,
                onValueChange = onAppNameChange,
                label = { Text("App Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = mainHeading,
                onValueChange = onHeadingChange,
                label = { Text("Home Screen Main Heading") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = subtitle,
                onValueChange = onSubtitleChange,
                label = { Text("Home Screen Subtitle") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = announcement,
                onValueChange = onAnnouncementChange,
                label = { Text("Announcement Banner") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = supportEmail,
                onValueChange = onSupportEmailChange,
                label = { Text("Customer Support Email") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = razorpayKeyId,
                onValueChange = onRazorpayChange,
                label = { Text("Razorpay Client-Safe Public Key ID") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save & Apply Configuration", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminTemplatesTab(
    templates: List<BiodataTemplate>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onTogglePaid: (BiodataTemplate) -> Unit,
    onPriceChange: (BiodataTemplate, Int) -> Unit,
    onToggleEnabled: (BiodataTemplate) -> Unit
) {
    val filtered = remember(templates, searchQuery) {
        if (searchQuery.isBlank()) templates
        else templates.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search 110 templates...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { template ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(template.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "${if (template.type == com.example.data.model.BiodataType.MARRIAGE) "Marriage" else "Job"} • ${template.category}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = template.isPaid,
                                onClick = { onTogglePaid(template) },
                                label = { Text(if (template.isPaid) "₹${template.price}" else "FREE", fontSize = 11.sp) }
                            )

                            if (template.isPaid) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (template.price > 19) onPriceChange(template, template.price - 10) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    IconButton(
                                        onClick = { onPriceChange(template, template.price + 10) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
fun AdminEBooksTab() {
    val ebooks = remember { EBookRegistry.getAllEBooks() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("eBook Inventory Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(ebooks, key = { it.id }) { ebook ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ebook.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Author: ${ebook.author} • Category: ${ebook.category}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (ebook.isPaid) MaterialTheme.colorScheme.primaryContainer else Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = if (ebook.isPaid) "₹${ebook.price}" else "FREE",
                            color = if (ebook.isPaid) MaterialTheme.colorScheme.primary else Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
