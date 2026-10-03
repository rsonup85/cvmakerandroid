package com.example.ui.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarriageBiodata
import com.example.ui.components.ProfilePhotoUploadCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarriageFormScreen(
    initialData: MarriageBiodata,
    onDataChanged: (MarriageBiodata) -> Unit,
    onPreviewClicked: () -> Unit,
    onAiImproveAboutMe: (original: String, values: String) -> Unit,
    onBack: () -> Unit
) {
    var data by remember { mutableStateOf(initialData) }
    var nameError by remember { mutableStateOf(false) }

    fun updateAndEmit(newData: MarriageBiodata) {
        data = newData
        onDataChanged(newData)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Marriage Biodata Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onPreviewClicked) {
                        Text("Preview", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }

                    Button(
                        onClick = {
                            if (data.fullName.isBlank()) {
                                nameError = true
                            } else {
                                onPreviewClicked()
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Preview & Save", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Profile Photo Upload Card
            item {
                ProfilePhotoUploadCard(
                    currentPhotoUri = data.profilePhotoUri,
                    onPhotoSelected = { uri ->
                        updateAndEmit(data.copy(profilePhotoUri = uri))
                    }
                )
            }

            // Section 1: Personal Details
            item {
                SectionHeaderCard(
                    title = "Personal Details",
                    subtitle = "Basic matrimonial profile information"
                )
            }

            item {
                OutlinedTextField(
                    value = data.fullName,
                    onValueChange = {
                        nameError = false
                        updateAndEmit(data.copy(fullName = it))
                    },
                    label = { Text("Full Name *") },
                    isError = nameError,
                    supportingText = if (nameError) { { Text("Full name is required") } } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = data.dateOfBirth,
                        onValueChange = { updateAndEmit(data.copy(dateOfBirth = it)) },
                        label = { Text("Date of Birth") },
                        placeholder = { Text("DD/MM/YYYY") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = data.birthTime,
                        onValueChange = { updateAndEmit(data.copy(birthTime = it)) },
                        label = { Text("Birth Time") },
                        placeholder = { Text("e.g. 10:30 AM") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = data.birthPlace,
                    onValueChange = { updateAndEmit(data.copy(birthPlace = it)) },
                    label = { Text("Birth Place (City, State)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = data.rashi,
                        onValueChange = { updateAndEmit(data.copy(rashi = it)) },
                        label = { Text("Rashi (Zodiac)") },
                        placeholder = { Text("e.g. Mesh, Vrishabha") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = data.nakshatra,
                        onValueChange = { updateAndEmit(data.copy(nakshatra = it)) },
                        label = { Text("Nakshatra") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = data.gotra,
                        onValueChange = { updateAndEmit(data.copy(gotra = it)) },
                        label = { Text("Gotra") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = data.manglik,
                        onValueChange = { updateAndEmit(data.copy(manglik = it)) },
                        label = { Text("Manglik (Yes/No)") },
                        placeholder = { Text("No / Anshik") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = data.religion,
                        onValueChange = { updateAndEmit(data.copy(religion = it)) },
                        label = { Text("Religion") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = data.caste,
                        onValueChange = { updateAndEmit(data.copy(caste = it)) },
                        label = { Text("Caste / Community") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = data.height,
                        onValueChange = { updateAndEmit(data.copy(height = it)) },
                        label = { Text("Height") },
                        placeholder = { Text("e.g. 5' 9\"") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = data.weight,
                        onValueChange = { updateAndEmit(data.copy(weight = it)) },
                        label = { Text("Weight") },
                        placeholder = { Text("e.g. 68 kg") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = data.complexion,
                        onValueChange = { updateAndEmit(data.copy(complexion = it)) },
                        label = { Text("Complexion") },
                        placeholder = { Text("Fair / Wheatish") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = data.bloodGroup,
                        onValueChange = { updateAndEmit(data.copy(bloodGroup = it)) },
                        label = { Text("Blood Group") },
                        placeholder = { Text("B+ / O+") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = data.education,
                    onValueChange = { updateAndEmit(data.copy(education = it)) },
                    label = { Text("Highest Qualification / Education") },
                    placeholder = { Text("e.g. B.Tech (IIT Delhi), MBA") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = data.occupation,
                        onValueChange = { updateAndEmit(data.copy(occupation = it)) },
                        label = { Text("Occupation / Job Details") },
                        placeholder = { Text("e.g. Software Engineer at Infosys") },
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = data.annualIncome,
                        onValueChange = { updateAndEmit(data.copy(annualIncome = it)) },
                        label = { Text("Annual Income") },
                        placeholder = { Text("e.g. 18 LPA") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            // About Me / Partner Expectations with Gemini AI assistant
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "About Me & Partner Expectations",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            FilledTonalButton(
                                onClick = { onAiImproveAboutMe(data.aboutMe, data.familyValues) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Improve", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = data.aboutMe,
                            onValueChange = { updateAndEmit(data.copy(aboutMe = it)) },
                            placeholder = { Text("Share a short warm introduction about personality, hobbies, life outlook, and desired partner qualities...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }
            }

            // Section 2: Family Details
            item {
                SectionHeaderCard(
                    title = "Family Background",
                    subtitle = "Parents, siblings and family origin (Optional)"
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = data.fatherName,
                        onValueChange = { updateAndEmit(data.copy(fatherName = it)) },
                        label = { Text("Father's Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = data.fatherOccupation,
                        onValueChange = { updateAndEmit(data.copy(fatherOccupation = it)) },
                        label = { Text("Father's Occupation") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = data.motherName,
                        onValueChange = { updateAndEmit(data.copy(motherName = it)) },
                        label = { Text("Mother's Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = data.motherOccupation,
                        onValueChange = { updateAndEmit(data.copy(motherOccupation = it)) },
                        label = { Text("Mother's Occupation") },
                        placeholder = { Text("Homemaker / Teacher") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = data.brothers,
                        onValueChange = { updateAndEmit(data.copy(brothers = it)) },
                        label = { Text("Brother(s)") },
                        placeholder = { Text("e.g. 1 Elder (Married)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = data.sisters,
                        onValueChange = { updateAndEmit(data.copy(sisters = it)) },
                        label = { Text("Sister(s)") },
                        placeholder = { Text("e.g. 1 Younger (Studying)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = data.familyType,
                        onValueChange = { updateAndEmit(data.copy(familyType = it)) },
                        label = { Text("Family Type") },
                        placeholder = { Text("Nuclear / Joint") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = data.nativePlace,
                        onValueChange = { updateAndEmit(data.copy(nativePlace = it)) },
                        label = { Text("Native Place") },
                        placeholder = { Text("Ancestral Town / City") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            // Section 3: Contact Details
            item {
                SectionHeaderCard(
                    title = "Contact Information",
                    subtitle = "Phone numbers and address for communication"
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Display Contact Details on Biodata PDF",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = data.showContactOnPdf,
                        onCheckedChange = { updateAndEmit(data.copy(showContactOnPdf = it)) }
                    )
                }
            }

            if (data.showContactOnPdf) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = data.mobile,
                            onValueChange = { updateAndEmit(data.copy(mobile = it)) },
                            label = { Text("Primary Mobile Number") },
                            placeholder = { Text("+91 98765 43210") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = data.alternateMobile,
                            onValueChange = { updateAndEmit(data.copy(alternateMobile = it)) },
                            label = { Text("Alternate Number") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = data.email,
                        onValueChange = { updateAndEmit(data.copy(email = it)) },
                        label = { Text("Email Address") },
                        placeholder = { Text("name@example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = data.residentialAddress,
                        onValueChange = { updateAndEmit(data.copy(residentialAddress = it)) },
                        label = { Text("Residential Address") },
                        placeholder = { Text("House / Street, Landmark, City, State - PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SectionHeaderCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
